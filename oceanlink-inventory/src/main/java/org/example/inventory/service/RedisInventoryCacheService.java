package org.example.inventory.service;

import org.example.inventory.dto.DeductResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Objects;

/**
 * 基于 Redis + Lua 的强一致性库存扣减
 * 热点库存先扣 Redis，再由合并任务异步同步到 DB
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisInventoryCacheService {

    private static final String STOCK_KEY_PREFIX = "inventory:stock:";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> deductStockScript;

    public String stockKey(Long skuId) {
        return STOCK_KEY_PREFIX + skuId;
    }

    /**
     * 原子扣减 Redis 中的库存（Lua 保证强一致）
     *
     * @param skuId    商品 SKU
     * @param quantity 扣减数量
     * @return 扣减结果，剩余库存或失败原因
     */
    public DeductResult deduct(Long skuId, int quantity) {
        String key = stockKey(skuId);
        Long remaining = stringRedisTemplate.execute(
                deductStockScript,
                Collections.singletonList(key),
                String.valueOf(quantity));
        if (remaining == null) {
            return DeductResult.builder()
                    .success(false)
                    .message("Redis 执行扣减失败或 key 不存在，请先初始化库存")
                    .build();
        }
        if (remaining == -1) {
            return DeductResult.builder()
                    .success(false)
                    .message("库存不足")
                    .build();
        }
        if (remaining == -2) {
            return DeductResult.builder()
                    .success(false)
                    .message("扣减数量非法")
                    .build();
        }
        return DeductResult.builder()
                .success(true)
                .remainingStock(remaining.intValue())
                .message("OK")
                .build();
    }

    /**
     * 设置 Redis 库存（初始化或从 DB 同步）
     */
    public void setStock(Long skuId, int stock) {
        String key = stockKey(skuId);
        stringRedisTemplate.opsForValue().set(key, String.valueOf(stock));
    }

    /**
     * 获取 Redis 中当前库存，不存在返回 null
     */
    public Integer getStock(Long skuId) {
        String key = stockKey(skuId);
        String val = stringRedisTemplate.opsForValue().get(key);
        if (val == null) return null;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 增加 Redis 库存（回滚或补货时用）
     */
    public long addStock(Long skuId, int delta) {
        String key = stockKey(skuId);
        Long v = stringRedisTemplate.opsForValue().increment(key, delta);
        return Objects.requireNonNull(v).longValue();
    }
}
