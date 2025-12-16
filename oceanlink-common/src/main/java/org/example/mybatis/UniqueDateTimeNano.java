package org.example.mybatis;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 逻辑唯一纳秒时间（单调、可排序）
 * <p>
 *  单 JVM 内严格递增
 *  可安全落库、排序、去重
 * </p>
 *
 * @author guohao.lu
 */
public class UniqueDateTimeNano {

    /**
     * JVM 启动时的基准点
     */
    private static final long START_EPOCH_NANO;
    private static final long START_NANO_TIME;

    /**
     * 保证单调递增
     */
    private static final AtomicLong LAST_NANO = new AtomicLong(0);

    static {
        Instant now = Instant.now();
        START_EPOCH_NANO =
                now.getEpochSecond() * 1_000_000_000L + now.getNano();
        START_NANO_TIME = System.nanoTime();
    }

    /**
     * 生成唯一纳秒时间戳（epoch nano）
     */
    public static long nextNanoTimestamp() {
        long elapsedNano = System.nanoTime() - START_NANO_TIME;
        long nano = START_EPOCH_NANO + elapsedNano;

        long last = LAST_NANO.get();
        if (nano <= last) {
            nano = last + 1;
        }

        LAST_NANO.set(nano);
        return nano;
    }

    /**
     * 转为 LocalDateTime（纳秒精度）
     */
    public static LocalDateTime nextDateTimeNano() {
        long nano = nextNanoTimestamp();

        long seconds = nano / 1_000_000_000L;
        int nanoPart = (int) (nano % 1_000_000_000L);

        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(seconds, nanoPart),
                ZoneId.systemDefault()
        );
    }
}

