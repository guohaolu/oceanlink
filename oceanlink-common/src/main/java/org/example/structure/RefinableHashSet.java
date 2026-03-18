package org.example.structure;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于 Refinable 机制的 HashSet
 * <p>
 * StripedLock + 动态扩容锁 + CAS协调 + 全局暂停点
 *
 * @param <T> T
 * @author guohao.lu
 */
public class RefinableHashSet<T> {

    // ===== 基础结构 =====
    protected volatile List<T>[] table;
    protected AtomicInteger setSize;

    // ===== 并发控制 =====
    private final AtomicMarkableReference<Thread> owner;
    private volatile ReentrantLock[] locks;

    public RefinableHashSet(int capacity) {
        setSize = new AtomicInteger(0);
        table = (List<T>[]) new List[capacity];
        locks = new ReentrantLock[capacity];
        owner = new AtomicMarkableReference<>(null, false);

        for (int i = 0; i < capacity; i++) {
            table[i] = new ArrayList<>();
            locks[i] = new ReentrantLock();
        }
    }

    // ===== hash =====
    private int hash(T x) {
        return (x.hashCode() & 0x7fffffff) % table.length;
    }

    // ===== acquire / release =====
    public void acquire(T x) {
        boolean[] mark = {true};
        Thread me = Thread.currentThread();
        Thread who;

        while (true) {
            // 等待 resize 完成
            do {
                who = owner.get(mark);
            } while (mark[0] && who != me);

            ReentrantLock[] oldLocks = locks;
            ReentrantLock lock = oldLocks[x.hashCode() % oldLocks.length];

            lock.lock();

            who = owner.get(mark);

            // 二次校验
            if ((!mark[0] || who == me) && locks == oldLocks) {
                return;
            } else {
                lock.unlock();
            }
        }
    }

    public void release(T x) {
        locks[x.hashCode() % locks.length].unlock();
    }

    // ===== contains =====
    public boolean contains(T x) {
        acquire(x);
        try {
            return table[hash(x)].contains(x);
        } finally {
            release(x);
        }
    }

    // ===== add =====
    public boolean add(T x) {
        boolean result;

        acquire(x);
        try {
            List<T> bucket = table[hash(x)];
            result = !bucket.contains(x);
            if (result) {
                bucket.add(x);
                setSize.getAndIncrement();
            }
        } finally {
            release(x);
        }

        if (policy()) {
            resize();
        }

        return result;
    }

    // ===== remove =====
    public boolean remove(T x) {
        boolean result;

        acquire(x);
        try {
            List<T> bucket = table[hash(x)];
            result = bucket.remove(x);
            if (result) {
                setSize.getAndDecrement();
            }
        } finally {
            release(x);
        }

        return result;
    }

    // ===== 扩容策略 =====
    private boolean policy() {
        return setSize.get() / table.length > 4;
    }

    // ===== resize =====
    public void resize() {
        int oldCapacity = table.length;
        int newCapacity = 2 * oldCapacity;

        Thread me = Thread.currentThread();

        // CAS 抢占 resize 权限
        if (owner.compareAndSet(null, me, false, true)) {
            try {
                if (table.length != oldCapacity) {
                    return;
                }

                // 等待所有线程退出临界区
                quiesce();

                List<T>[] oldTable = table;

                // 新表
                table = (List<T>[]) new List[newCapacity];
                for (int i = 0; i < newCapacity; i++) {
                    table[i] = new ArrayList<>();
                }

                // 新锁
                locks = new ReentrantLock[newCapacity];
                for (int i = 0; i < newCapacity; i++) {
                    locks[i] = new ReentrantLock();
                }

                // 数据迁移
                for (List<T> bucket : oldTable) {
                    for (T x : bucket) {
                        table[hash(x)].add(x);
                    }
                }

            } finally {
                owner.set(null, false);
            }
        }
    }

    // ===== 等待所有锁释放 =====
    private void quiesce() {
        for (ReentrantLock lock : locks) {
            while (lock.isLocked()) {
                // busy spin
            }
        }
    }
}
