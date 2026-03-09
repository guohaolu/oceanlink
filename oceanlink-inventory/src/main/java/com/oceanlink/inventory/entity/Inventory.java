package com.oceanlink.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存主表（符合文档库存域模型）
 * <ul>
 *   <li>sq 可售库存：实际可售数量，扣减时减少</li>
 *   <li>wq 预扣库存：下单后由 sq 转入，占位未付款</li>
 *   <li>oq 占用库存：货品仓模式，付款后由 wq 转入</li>
 *   <li>lq 预锁库存：锁库存时增加，用于 Redis 分桶扣减计数；DB 扣减条件 sq - lq - δq ≥ 0</li>
 * </ul>
 */
@Data
@TableName("inventory")
public class Inventory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 商品 SKU */
    private Long skuId;

    /** 可售库存 sq：实际可售数量 */
    private Integer sq;
    /** 预扣库存 wq：用户下单后由 sq 转入 */
    private Integer wq;
    /** 占用库存 oq：付款后由 wq 转入（货品仓模式） */
    private Integer oq;
    /** 预锁库存 lq：锁库存时增加，同步到 Redis 用于扣减计数 */
    private Integer lq;

    @Version
    private Integer version;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}
