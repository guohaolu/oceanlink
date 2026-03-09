# oceanlink-inventory 库存模块

本模块实现 **基于分布式缓存的强一致性热点库存合并扣减** 方案，对应文档：`doc/assert/库存合并扣减：一种基于分布式缓存的强一致性热点库存扣减方案.md`。

## 架构概览

```
                    ┌─────────────────────────────────────────────────────────┐
                    │                    库存扣减请求                           │
                    └─────────────────────────┬───────────────────────────────┘
                                              │
                    ┌────────────────────────▼───────────────────────────────┐
                    │  Redis + Lua 原子扣减（强一致，防超卖）                    │
                    │  KEYS[1]=inventory:stock:{skuId}  ARGV[1]=quantity       │
                    └─────────────────────────┬───────────────────────────────┘
                                              │ 成功
                    ┌────────────────────────▼───────────────────────────────┐
                    │  合并缓冲区 Map<skuId, MergeSlot>                       │
                    │  同一 SKU 多笔扣减汇总为 totalQty                        │
                    └─────────────────────────┬───────────────────────────────┘
                                              │ 定时 / 阈值
                    ┌────────────────────────▼───────────────────────────────┐
                    │  批量落库：UPDATE inventory SET stock=stock-?, version+1 │
                    │  写入 inventory_log 流水                                 │
                    └─────────────────────────────────────────────────────────┘
```

- **强一致**：先扣 Redis（Lua 原子扣减），再异步合并写 MySQL，避免热点行锁。
- **合并扣减**：同一 SKU 在时间窗口内的多次扣减合并为一次 DB 更新，降低 DB 压力。
- **可关闭合并**：`inventory.merge-deduct.enabled=false` 时，每次扣减后立即落库（适合低 QPS 场景）。

## 核心组件

| 组件 | 说明 |
|------|------|
| `RedisInventoryCacheService` | Redis 库存缓存 + Lua 原子扣减 |
| `InventoryMergeDeductService` | 合并缓冲区 + 定时落库 + 流水 |
| `deduct_stock.lua` | 原子“检查库存 ≥ 扣减量 → 扣减 → 返回剩余” |
| `MergeDeductConfig` | 合并窗口间隔、单 SKU 最大合并条数、是否启用合并 |

## 配置

```yaml
inventory:
  merge-deduct:
    enabled: true
    flush-interval-ms: 500
    max-merge-count-per-sku: 100
```

## 接口

- `POST /inventory/deduct` 扣减（Body: skuId, quantity, orderId?, bizType?）
- `GET /inventory/stock/{skuId}` 查询当前库存（优先 Redis）
- `POST /inventory/sync/{skuId}` 将 DB 库存同步到 Redis（初始化/修复用）

## 表结构

- `inventory`：主表，`sku_id` 唯一，`stock` + `version` 乐观锁。
- `inventory_log`：扣减流水，`sku_id`、`deduct_qty`、`order_id`、`biz_type`。

建表语句见 `src/main/resources/schema.sql`。
