package org.example.inventory.controller;

import org.example.inventory.dto.DeductRequest;
import org.example.inventory.dto.DeductResult;
import org.example.inventory.service.InventoryMergeDeductService;
import org.example.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 库存接口：扣减（合并扣减 + Redis 强一致）、查询、同步
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryMergeDeductService mergeDeductService;
    private final InventoryService inventoryService;

    /**
     * 扣减库存（先 Redis 强一致扣减，再进入合并落库）
     */
    @PostMapping("/deduct")
    public DeductResult deduct(@Valid @RequestBody DeductRequest request) {
        return mergeDeductService.deduct(request);
    }

    /**
     * 查询当前库存（优先 Redis）
     */
    @GetMapping("/stock/{skuId}")
    public Integer getStock(@PathVariable Long skuId) {
        return inventoryService.getStock(skuId);
    }

    /**
     * 将 DB 库存同步到 Redis（运维/初始化用）
     */
    @PostMapping("/sync/{skuId}")
    public void syncToRedis(@PathVariable Long skuId) {
        inventoryService.syncStockToRedis(skuId);
    }
}
