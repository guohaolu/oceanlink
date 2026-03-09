package org.example.inventory.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 合并扣减项：同一 SKU 在时间窗口内的扣减汇总
 * 用于异步落库时批量更新 DB
 */
@Data
public class MergeDeductItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long skuId;
    /** 本窗口内该 SKU 总扣减量 */
    private Integer totalDeductQty;
    /** 关联的业务单号列表（可选，用于流水表） */
    private String orderId;
    private String bizType;
}
