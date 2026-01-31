package org.example.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * CaffeineCacheRunner
 * @author guohao.lu
 */
@Component
public class CaffeineCacheRunner implements ApplicationRunner {
    private static final Cache<String, String> CACHE = Caffeine.newBuilder()
            .initialCapacity(10)
            .maximumSize(10)
            .expireAfterWrite(3, TimeUnit.HOURS)
            .removalListener((key, val, removalCause) -> { })
            .recordStats()
            .weakValues()
            .build();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // TODO 待实现
    }
}
