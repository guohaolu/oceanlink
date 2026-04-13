package org.example.order.domain.order;

/**
 * 履约主体映射器。
 *
 * @author guohao.lu
 */
public final class OrderFulfillmentModeMapper {

    /**
     * 私有构造函数。
     */
    private OrderFulfillmentModeMapper() {
    }

    /**
     * 将 Amazon 原始履约主体映射为业务履约模式。
     *
     * @param fulfilledByRaw Amazon 原始履约主体
     * @return 业务履约模式
     */
    public static OrderFulfillmentMode map(String fulfilledByRaw) {
        if ("AMAZON".equalsIgnoreCase(fulfilledByRaw)) {
            return OrderFulfillmentMode.FBA;
        }
        if ("MERCHANT".equalsIgnoreCase(fulfilledByRaw)) {
            return OrderFulfillmentMode.FBM;
        }
        return OrderFulfillmentMode.UNKNOWN;
    }
}
