package org.example.mybatis;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Java 中生成 唯一 的 DateTime64(6)
 * <pre>{@code
 * LocalDateTime dt = UniqueDateTime64.nextDateTime64();
 * System.out.println(dt);
 * }</pre>
 *
 * @author guohao.lu
 */
public class UniqueDateTime64 {
    private static final AtomicLong LAST_MICRO = new AtomicLong(0);

    /**
     * 生成唯一微秒时间戳
     *
     * @return 微秒时间戳
     */
    public static long nextMicroTimestamp() {
        // 取纳秒低6位转微秒
        long micros = Instant.now().toEpochMilli() * 1000
                + (System.nanoTime() % 1_000_000) / 1000;

        long last = LAST_MICRO.get();

        if (micros <= last) {
            // 防止重复
            micros = last + 1;
        }

        LAST_MICRO.set(micros);
        return micros;
    }

    /**
     * 转为 ClickHouse 对应的 LocalDateTime
     *
     * @return LocalDateTime
     */
    public static LocalDateTime nextDateTime64() {
        long micro = nextMicroTimestamp();
        long second = micro / 1_000_000;
        long microPart = micro % 1_000_000;

        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(second, microPart * 1000),
                ZoneId.systemDefault()
        );
    }
}
