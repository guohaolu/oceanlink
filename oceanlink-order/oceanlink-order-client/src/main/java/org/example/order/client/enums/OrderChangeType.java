package org.example.order.client.enums;

/**
 * Amazon 订单变更类型。
 *
 * @author guohao.lu
 */
public enum OrderChangeType {

    /**
     * 订单状态变化。
     */
    ORDER_STATUS_CHANGE("OrderStatusChange"),

    /**
     * 买家请求变化。
     */
    BUYER_REQUESTED_CHANGE("BuyerRequestedChange");

    /**
     * Amazon 原始变更类型值。
     */
    private final String amazonValue;

    /**
     * 创建订单变更类型枚举。
     *
     * @param amazonValue Amazon 原始值
     */
    OrderChangeType(String amazonValue) {
        this.amazonValue = amazonValue;
    }

    /**
     * 返回 Amazon 原始变更类型值。
     *
     * @return Amazon 原始值
     */
    public String amazonValue() {
        return amazonValue;
    }
}
