package org.example.order.domain.order;

import java.time.Instant;
import java.util.List;

/**
 * 订单聚合合并策略。
 *
 * @author guohao.lu
 */
public class OrderMergePolicy {

    /**
     * 合并当前订单与新到订单快照。
     *
     * @param current 当前已存在订单
     * @param incoming 新到订单快照
     * @return 合并后的订单聚合
     */
    public OrderAggregate merge(OrderAggregate current, OrderAggregate incoming) {
        boolean incomingNewer = incoming.lastUpdatedTimeAmazon() != null
                && (current.lastUpdatedTimeAmazon() == null
                || incoming.lastUpdatedTimeAmazon().isAfter(current.lastUpdatedTimeAmazon()));
        String orderStatus = incomingNewer && hasText(incoming.orderStatus())
                ? incoming.orderStatus()
                : current.orderStatus();
        String fulfilledByRaw = incomingNewer && hasText(incoming.fulfilledByRaw())
                ? incoming.fulfilledByRaw()
                : current.fulfilledByRaw();
        OrderFulfillmentMode fulfillmentMode = incomingNewer && incoming.fulfillmentMode() != null
                ? incoming.fulfillmentMode()
                : current.fulfillmentMode();
        Instant lastUpdatedTimeAmazon = incomingNewer
                ? incoming.lastUpdatedTimeAmazon()
                : current.lastUpdatedTimeAmazon();
        String buyerEmail = resolveEnhancedValue(current.buyerEmail(), incoming.buyerEmail(), incomingNewer);
        String buyerCompanyName = resolveEnhancedValue(
                current.buyerCompanyName(),
                incoming.buyerCompanyName(),
                incomingNewer);
        List<String> programs = resolvePrograms(current.programs(), incoming.programs(), incomingNewer);
        return new OrderAggregate(
                current.sellerId(),
                current.marketplaceId(),
                current.amazonOrderId(),
                orderStatus,
                fulfilledByRaw,
                fulfillmentMode,
                lastUpdatedTimeAmazon,
                buyerEmail,
                buyerCompanyName,
                programs);
    }

    /**
     * 判断字符串是否包含有效文本。
     *
     * @param value 待判断值
     * @return 包含有效文本返回 true
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 解析增强字段值。
     *
     * @param currentValue 当前值
     * @param incomingValue 新值
     * @param incomingNewer 新快照是否更新
     * @return 合并后的字段值
     */
    private String resolveEnhancedValue(String currentValue, String incomingValue, boolean incomingNewer) {
        if (!hasText(currentValue) && hasText(incomingValue)) {
            return incomingValue;
        }
        if (incomingNewer && hasText(incomingValue)) {
            return incomingValue;
        }
        return currentValue;
    }

    /**
     * 解析扩展 program 列表。
     *
     * @param currentPrograms 当前 program 列表
     * @param incomingPrograms 新快照 program 列表
     * @param incomingNewer 新快照是否更新
     * @return 合并后的 program 列表
     */
    private List<String> resolvePrograms(
            List<String> currentPrograms,
            List<String> incomingPrograms,
            boolean incomingNewer) {
        boolean currentEmpty = currentPrograms == null || currentPrograms.isEmpty();
        boolean incomingEmpty = incomingPrograms == null || incomingPrograms.isEmpty();
        if (!incomingEmpty && (currentEmpty || incomingNewer)) {
            return incomingPrograms;
        }
        return currentPrograms;
    }
}
