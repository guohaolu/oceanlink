package org.example.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.inventory.entity.Inventory;
import org.example.inventory.mapper.InventoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 库存基础服务：查询、初始化、同步缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryMapper inventoryMapper;
    private final RedisInventoryCacheService redisInventoryCacheService;

    public Inventory getBySkuId(Long skuId) {
        return inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>().eq(Inventory::getSkuId, skuId));
    }

    /**
     * 优先查 Redis，无则查 DB 并回填 Redis
     */
    public Integer getStock(Long skuId) {
        Integer cached = redisInventoryCacheService.getStock(skuId);
        if (cached != null) {
            return cached;
        }
        Inventory inv = getBySkuId(skuId);
        if (inv != null) {
            redisInventoryCacheService.setStock(skuId, inv.getStock());
            return inv.getStock();
        }
        return null;
    }

    /**
     * 初始化或同步 DB 库存到 Redis（用于上线/数据修复）
     */
    public void syncStockToRedis(Long skuId) {
        Inventory inv = getBySkuId(skuId);
        if (inv != null) {
            redisInventoryCacheService.setStock(skuId, inv.getStock());
            log.info("同步库存到 Redis skuId={} stock={}", skuId, inv.getStock());
        }
    }
}
