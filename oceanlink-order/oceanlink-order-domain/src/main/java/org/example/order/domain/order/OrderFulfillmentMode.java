package org.example.order.domain.order;

/**
 * 订单履约模式。
 *
 * @author guohao.lu
 */
public enum OrderFulfillmentMode {

    /**
     * 亚马逊履约。
     */
    FBA,

    /**
     * 商家履约。
     */
    FBM,

    /**
     * 未知履约模式。
     */
    UNKNOWN
}
