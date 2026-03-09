package org.example.inventory.service;

import org.example.inventory.config.MergeDeductConfig;
import org.example.inventory.dto.DeductRequest;
import org.example.inventory.dto.DeductResult;
import org.example.inventory.entity.Inventory;
import org.example.inventory.entity.InventoryLog;
import org.example.inventory.mapper.InventoryLogMapper;
import org.example.inventory.mapper.InventoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 合并扣减服务
 * 1. 先扣 Redis（强一致）
 * 2. 扣减成功后写入内存合并队列（按 skuId 聚合）
 * 3. 定时/阈值触发，批量落库并写流水
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryMergeDeductService {

    private final RedisInventoryCacheService redisInventoryCacheService;
    private final InventoryMapper inventoryMapper;
    private final InventoryLogMapper inventoryLogMapper;
    private final MergeDeductConfig mergeDeductConfig;

    /** 合并缓冲区：skuId -> 该 SKU 本窗口内待落库的扣减汇总 */
    private final Map<Long, MergeSlot> mergeBuffer = new ConcurrentHashMap<>();

    /**
     * 执行扣减：先 Redis 再入合并队列
     */
    public DeductResult deduct(DeductRequest request) {
        DeductResult result = redisInventoryCacheService.deduct(
                request.getSkuId(), request.getQuantity());
        if (!result.isSuccess()) {
            return result;
        }
        if (mergeDeductConfig.isEnabled()) {
            addToMergeBuffer(request.getSkuId(), request.getQuantity(),
                    request.getOrderId(), request.getBizType());
        } else {
            flushSkuToDb(request.getSkuId(), request.getQuantity(),
                    request.getOrderId(), request.getBizType());
        }
        return result.toBuilder().orderId(request.getOrderId()).build();
    }

    private void addToMergeBuffer(Long skuId, int qty, String orderId, String bizType) {
        mergeBuffer.compute(skuId, (k, slot) -> {
            if (slot == null) {
                slot = new MergeSlot();
            }
            slot.add(qty, orderId, bizType);
            return slot;
        });
    }

    /** 定时将合并缓冲区落库 */
    @Scheduled(fixedDelayString = "${inventory.merge-deduct.flush-interval-ms:500}")
    public void flushMergeBuffer() {
        if (mergeBuffer.isEmpty()) {
            return;
        }
        Map<Long, MergeSlot> snapshot;
        synchronized (mergeBuffer) {
            snapshot = new java.util.HashMap<>(mergeBuffer);
            mergeBuffer.clear();
        }
        for (Map.Entry<Long, MergeSlot> e : snapshot.entrySet()) {
            Long skuId = e.getKey();
            MergeSlot slot = e.getValue();
            try {
                flushSkuToDb(skuId, slot.totalQty, slot.orderId, slot.bizType);
            } catch (Exception ex) {
                log.warn("合并落库失败 skuId={}", skuId, ex);
                mergeBuffer.merge(skuId, slot, (old, add) -> {
                    if (old != null) {
                        old.totalQty += add.totalQty;
                        return old;
                    }
                    return add;
                });
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void flushSkuToDb(Long skuId, int totalDeductQty, String orderId, String bizType) {
        Inventory inv = inventoryMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, skuId));
        if (inv == null) {
            log.warn("库存记录不存在 skuId={}", skuId);
            return;
        }
        int newStock = inv.getStock() - totalDeductQty;
        if (newStock < 0) {
            log.warn("合并落库时库存不足 skuId={} deduct={} current={}", skuId, totalDeductQty, inv.getStock());
            return;
        }
        inv.setStock(newStock);
        inv.setGmtModified(LocalDateTime.now());
        inventoryMapper.updateById(inv);

        InventoryLog logEntity = new InventoryLog();
        logEntity.setSkuId(skuId);
        logEntity.setDeductQty(totalDeductQty);
        logEntity.setOrderId(orderId);
        logEntity.setBizType(bizType != null ? bizType : "ORDER");
        logEntity.setStatus("SUCCESS");
        logEntity.setGmtCreate(LocalDateTime.now());
        inventoryLogMapper.insert(logEntity);
    }

    /** 单 SKU 合并槽 */
    private static class MergeSlot {
        int totalQty;
        String orderId;
        String bizType;

        void add(int qty, String orderId, String bizType) {
            this.totalQty += qty;
            if (this.orderId == null) {
                this.orderId = orderId;
                this.bizType = bizType;
            }
        }
    }
}
