package org.example.handler;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据批量处理类
 * <p>
 * 主要通过异步线程池和调度线程池实现，其中{@code dataList}是状态值，两个线程池对其的操作需要加锁
 * </p>
 *
 * @author guohao.lu
 */
@Slf4j
public class NasFileBatchProcessorScheduled<M extends BaseMapper<T>, T> {
    /**
     * 数据列表，用于存储待处理的数据项
     */
    private final List<T> dataList = new ArrayList<>();

    /**
     * 批处理大小，定义每次批量处理的数据量阈值
     */
    private static final int BATCH_SIZE = 4000;

    /**
     * 同步锁对象，用于保护共享资源的线程安全访问
     */
    private final Object lock = new Object();

    /**
     * 关闭标志，标识服务是否正在关闭过程中
     */
    private volatile boolean shutdown = false;

    /**
     * 强制刷新标志，标识是否正在进行强制刷新操作
     */
    private volatile boolean isForceFlushing = false;

    /**
     * 待处理任务计数器，用于跟踪当前等待执行的任务数量
     */
    private final AtomicInteger pendingTasks = new AtomicInteger(0);

    /**
     * 数据仓库服务实现，提供数据持久化操作的具体实现
     */
    private final ServiceImpl<M, T> repository;

    /**
     * 定时调度执行器，用于执行定时任务和周期性任务
     */
    private final ScheduledExecutorService scheduler;

    /**
     * 异步执行器，用于执行异步任务
     */
    private final ExecutorService asyncExecutor;

    /**
     * 构造函数，初始化批量处理器。
     *
     * @param repository 数据库服务接口实现类，用于执行批量插入操作
     */
    public NasFileBatchProcessorScheduled(ServiceImpl<M, T> repository) {
        this.repository = repository;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.asyncExecutor = Executors.newFixedThreadPool(2);
        startScheduledCheck();
    }

    /**
     * 添加单条数据到缓存列表中。当达到批处理大小时自动触发批量处理。
     *
     * @param data 待添加的数据项
     * @throws IllegalStateException 当前处理器已经关闭时抛出该异常
     */
    public void addData(T data) {
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

    /**
     * 内部批量处理方法，在持有锁的情况下将当前缓存中的数据提交为一个批次任务。
     * 必须在同步块内调用此方法以确保线程安全。
     */
    private void processBatchInternal() {
        if (dataList.isEmpty() || shutdown) {
            return;
        }

        List<T> batchData = new ArrayList<>(dataList);
        dataList.clear();
        submitBatchTask(batchData);
    }

    /**
     * 提交一批数据作为异步任务进行数据库插入。
     *
     * @param batchData 需要插入的一批数据
     */
    private void submitBatchTask(List<T> batchData) {
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

    /**
     * 启动定时检查机制，周期性地尝试处理缓存中的未满批次数据。
     */
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

    /**
     * 将指定的数据列表插入数据库。
     *
     * @param batchData 要插入的数据列表
     */
    private void insertToDatabase(List<T> batchData) {
        try {
            repository.getBaseMapper().insert(batchData);
        } catch (Exception e) {
            log.error("数据库插入异常: {}", e.getMessage(), e);
            // 注意：这里如果重新加入队列，需要在同步块内操作
            // synchronized (lock) {
            //     dataList.addAll(batchData);
            // }
        }
    }

    /**
     * 获取当前处理器的状态信息。
     *
     * @return 包含缓冲区大小、待处理任务数、是否正在强制刷新以及是否已关闭等信息的对象
     */
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

    /**
     * 强制刷新缓存中的所有数据并等待其全部处理完毕（无超时限制）。
     */
    public void forceFlush() {
        boolean forceFlushFlag = forceFlushWithTimeout(0, TimeUnit.SECONDS);
        if (!forceFlushFlag) {
            throw new RuntimeException("Force flush failed");
        }
    }

    /**
     * 带超时控制的强制刷新方法，会立即处理缓存中的所有数据，并等待所有任务完成或超时退出。
     *
     * @param timeout 最大等待时间
     * @param unit    时间单位
     * @return 如果成功处理完所有数据则返回true；否则返回false
     * @throws IllegalStateException 当前处理器已经关闭时抛出该异常
     */
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
                            // 无限等待时定期检查
                            lock.wait(100);
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
     * 设置关闭标志位，处理剩余数据，然后关闭线程池资源。
     */
    public void shutdown() {
        // 先设置关闭标志，防止新任务加入
        shutdown = true;

        synchronized (lock) {
            // 处理剩余数据
            if (!dataList.isEmpty()) {
                List<T> remainingData = new ArrayList<>(dataList);
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

    /**
     * 表示批量处理器运行状态的信息记录类。
     *
     * @param bufferSize      当前缓冲区中尚未处理的数据数量
     * @param pendingTasks    正在异步处理但还未完成的任务数量
     * @param isForceFlushing 是否正在进行强制刷新操作
     * @param isShutdown      处理器是否已被关闭
     */
    public record BatchProcessorStatus(int bufferSize, int pendingTasks, boolean isForceFlushing, boolean isShutdown) {
    }

    /**
     * 注册 JVM 关闭钩子，在程序终止前自动调用 shutdown 方法释放资源。
     */
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }
}
