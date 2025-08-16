package org.example.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存配置管理（支持 Caffeine/Redis/ConcurrentMap）
 * <p>
 * 示例代码：
 * <pre>{@code
 * @Cacheable(cacheManager = "caffeineCacheManager", value = "userCache")
 * }</pre>
 *
 * @author guohao.lu
 */
@EnableCaching
@Configuration
@RequiredArgsConstructor
public class CacheConfig {
    /**
     * 主缓存管理器（组合多种缓存实现）
     */
    @Bean
    @Primary
    public CacheManager primaryCacheManager() {
        return new CompositeCacheManager(
                caffeineCacheManager(),
                simpleCacheManager()
        );
    }

    /**
     * Caffeine 本地缓存（高性能）
     */
    @Bean
    public CacheManager caffeineCacheManager() {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .recordStats();

        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(caffeine);
        return caffeineCacheManager;
    }

    /**
     * ConcurrentMap 简单内存缓存（开发测试用）
     */
    @Bean
    public CacheManager simpleCacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                new ConcurrentMapCache("simpleCache"),
                new ConcurrentMapCache("tempCache")
        ));
        return manager;
    }
}
