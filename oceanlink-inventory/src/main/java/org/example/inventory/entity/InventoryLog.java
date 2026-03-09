package org.example.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存扣减流水
 * 记录每次合并扣减的明细，用于对账与追溯
 */
@Data
@TableName("inventory_log")
public class InventoryLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long skuId;
    /** 扣减数量（可为负表示加库存） */
    private Integer deductQty;
    /** 业务单号（订单号等） */
    private String orderId;
    /** 业务类型：ORDER / REFUND / ADJUST 等 */
    private String bizType;
    /** 状态：SUCCESS / FAIL */
    private String status;
    private LocalDateTime gmtCreate;
}
