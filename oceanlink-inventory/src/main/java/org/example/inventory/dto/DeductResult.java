package org.example.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 库存扣减结果
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DeductResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否扣减成功 */
    private boolean success;
    /** 提示信息，失败时说明原因 */
    private String message;
    /** 扣减成功后剩余库存（可选） */
    private Integer remainingStock;
    /** 业务单号回填 */
    private String orderId;
}
