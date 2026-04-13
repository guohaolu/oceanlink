package org.example.order.domain.order;

import java.time.Instant;
import java.util.List;

/**
 * 订单聚合根。
 *
 * @param sellerId Seller 标识
 * @param marketplaceId 站点标识
 * @param amazonOrderId Amazon 订单号
 * @param orderStatus 订单状态
 * @param fulfilledByRaw Amazon 原始履约主体
 * @param fulfillmentMode 业务履约模式
 * @param lastUpdatedTimeAmazon Amazon 更新时间
 * @param buyerEmail 买家邮箱
 * @param buyerCompanyName 企业买家公司名
 * @param programs 订单扩展 program 列表
 * @author guohao.lu
 */
public record OrderAggregate(
        String sellerId,
        String marketplaceId,
        String amazonOrderId,
        String orderStatus,
        String fulfilledByRaw,
        OrderFulfillmentMode fulfillmentMode,
        Instant lastUpdatedTimeAmazon,
        String buyerEmail,
        String buyerCompanyName,
        List<String> programs) {
}
