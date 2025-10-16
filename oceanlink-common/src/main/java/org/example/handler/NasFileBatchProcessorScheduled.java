package org.example.handler;


import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据批量处理类
 * <p>
 *     主要通过异步线程池和调度线程池实现，其中{@code dataList}是状态值，两个线程池对其的操作需要加锁
 * </p>
 *
 * @author guohao.lu
 */
@Slf4j
public class NasFileBatchProcessorScheduled {
    // 改为 ArrayList，因为所有访问都在同步块内
    private final List<FinanceInvoiceDetailSourceEntity> dataList = new ArrayList<>();
    private static final int BATCH_SIZE = 4000;
    private final Object lock = new Object(); // 所有状态变更都用这个锁

    // 状态变量
    private volatile boolean shutdown = false;
    private volatile boolean isForceFlushing = false;
    private final AtomicInteger pendingTasks = new AtomicInteger(0);

    private final FinanceInvoiceDetailSourceRepository repository;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService asyncExecutor;

    public NasFileBatchProcessorScheduled(FinanceInvoiceDetailSourceRepository repository) {
        this.repository = repository;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.asyncExecutor = Executors.newFixedThreadPool(2);
        startScheduledCheck();
    }

    public void addData(FinanceInvoiceDetailSourceEntity data) {
        synchronized (lock) {
            if (shutdown) {
                throw new IllegalStateException("Processor is shutting down");
            }

            dataList.add(data);

            // 在同步块内检查和处理，保证原子性
            if (dataList.size() >= BATCH_SIZE && !isForceFlushing) {
                processBatchInternal();
            }
        }
    }

    // 新增：内部批量处理方法，必须在同步块内调用
    private void processBatchInternal() {
        if (dataList.isEmpty() || shutdown) {
            return;
        }

        List<FinanceInvoiceDetailSourceEntity> batchData = new ArrayList<>(dataList);
        dataList.clear();
        submitBatchTask(batchData);
    }

    private void submitBatchTask(List<FinanceInvoiceDetailSourceEntity> batchData) {
        pendingTasks.incrementAndGet();

        asyncExecutor.submit(() -> {
            try {
                insertToDatabase(batchData);
            } finally {
                // 任务完成时需要在锁内更新状态
                synchronized (lock) {
                    int remaining = pendingTasks.decrementAndGet();
                    // 通知等待强制刷新的线程
                    if (remaining == 0 && dataList.isEmpty()) {
                        lock.notifyAll();
                    }
                }
            }
        });
    }

    private void startScheduledCheck() {
        scheduler.scheduleAtFixedRate(() -> {
            if (shutdown) {
                return;
            }

            synchronized (lock) {
                if (!dataList.isEmpty() && !isForceFlushing && !shutdown) {
                    processBatchInternal();
                }
            }
        }, 1, 3, TimeUnit.SECONDS);
    }

    private void insertToDatabase(List<FinanceInvoiceDetailSourceEntity> batchData) {
        try {
            repository.getBaseMapper().insert(batchData);
            log.info("成功批量插入 {} 条数据", batchData.size());
        } catch (Exception e) {
            log.error("数据库插入异常: {}", e.getMessage(), e);
            // 注意：这里如果重新加入队列，需要在同步块内操作
            // synchronized (lock) {
            //     dataList.addAll(batchData);
            // }
        }
    }

    // 状态检查也需要同步
    public BatchProcessorStatus getStatus() {
        synchronized (lock) {
            return new BatchProcessorStatus(
                    dataList.size(),
                    pendingTasks.get(),
                    isForceFlushing,
                    shutdown
            );
        }
    }

    // 强制刷新 - 修复版本
    public void forceFlush() {
        forceFlushWithTimeout(0, TimeUnit.SECONDS);
    }

    public boolean forceFlushWithTimeout(long timeout, TimeUnit unit) {
        // 先检查关闭状态，避免进入同步块后无法关闭
        if (shutdown) {
            throw new IllegalStateException("Processor is shutting down");
        }

        synchronized (lock) {
            // 再次检查关闭状态
            if (shutdown) {
                return dataList.isEmpty() && pendingTasks.get() == 0;
            }

            isForceFlushing = true;

            try {
                // 处理当前缓冲区数据
                if (!dataList.isEmpty()) {
                    processBatchInternal();
                }

                long startTime = System.currentTimeMillis();
                long timeoutMs = timeout > 0 ? unit.toMillis(timeout) : Long.MAX_VALUE;

                // 等待所有任务完成
                while ((pendingTasks.get() > 0 || !dataList.isEmpty()) && !shutdown) {
                    // 检查超时
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (timeout > 0 && elapsed >= timeoutMs) {
                        log.warn("强制刷新超时，剩余任务: {}, 缓冲区: {}",
                                pendingTasks.get(), dataList.size());
                        return false;
                    }

                    try {
                        long remainingWait = Math.max(1, timeoutMs - elapsed);
                        if (timeout == 0) {
                            lock.wait(100); // 无限等待时定期检查
                        } else {
                            lock.wait(Math.min(100, remainingWait));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("强制刷新被中断");
                        return false;
                    }
                }

                // 最终状态检查
                boolean success = pendingTasks.get() == 0 && dataList.isEmpty() && !shutdown;
                if (success) {
                    log.info("强制刷新完成，所有数据已提交");
                }
                return success;

            } finally {
                isForceFlushing = false;
                lock.notifyAll();
            }
        }
    }

    /**
     * 优雅关闭方法 - 修复版本
     */
    public void shutdown() {
        // 先设置关闭标志，防止新任务加入
        shutdown = true;

        synchronized (lock) {
            // 处理剩余数据
            if (!dataList.isEmpty()) {
                List<FinanceInvoiceDetailSourceEntity> remainingData = new ArrayList<>(dataList);
                dataList.clear();
                // 同步插入，因为即将关闭
                insertToDatabase(remainingData);
            }

            // 通知所有等待的线程
            lock.notifyAll();
        }

        // 关闭线程池
        scheduler.shutdown();
        asyncExecutor.shutdown();

        try {
            // 等待正在执行的任务完成
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!asyncExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
            asyncExecutor.shutdownNow();
        }

        log.info("处理器已关闭");
    }

    // 状态类保持不变
    public static class BatchProcessorStatus {
        private final int bufferSize;
        private final int pendingTasks;
        private final boolean isForceFlushing;
        private final boolean isShutdown;

        public BatchProcessorStatus(int bufferSize, int pendingTasks,
                                    boolean isForceFlushing, boolean isShutdown) {
            this.bufferSize = bufferSize;
            this.pendingTasks = pendingTasks;
            this.isForceFlushing = isForceFlushing;
            this.isShutdown = isShutdown;
        }

        // Getters...
        public int getBufferSize() { return bufferSize; }
        public int getPendingTasks() { return pendingTasks; }
        public boolean isForceFlushing() { return isForceFlushing; }
        public boolean isShutdown() { return isShutdown; }
    }

    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }
}