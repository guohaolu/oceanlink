package org.example.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存主表
 * 持久化真实库存，合并扣减后异步落库
 */
@Data
@TableName("inventory")
public class Inventory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 商品 SKU */
    private Long skuId;
    /** 可用库存（扣减以该值为准，合并后同步到 DB） */
    private Integer stock;
    /** 乐观锁版本号，防超卖 */
    @Version
    private Integer version;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}
