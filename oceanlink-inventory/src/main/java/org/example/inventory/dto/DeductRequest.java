package org.example.inventory.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 库存扣减请求
 */
@Data
public class DeductRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "skuId 不能为空")
    private Long skuId;
    @NotNull(message = "扣减数量不能为空")
    @Min(value = 1, message = "扣减数量必须大于 0")
    private Integer quantity;
    /** 业务单号，如订单号 */
    private String orderId;
    /** 业务类型：ORDER / REFUND / ADJUST，默认 ORDER */
    private String bizType = "ORDER";
}
