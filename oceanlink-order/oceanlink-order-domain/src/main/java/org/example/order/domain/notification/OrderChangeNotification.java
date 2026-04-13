package org.example.order.domain.notification;

import java.time.Instant;

/**
 * 订单变更通知领域对象。
 *
 * @param notificationId Amazon 通知标识
 * @param sellerId Seller 标识
 * @param marketplaceId 站点标识
 * @param amazonOrderId Amazon 订单号
 * @param orderChangeType 变更类型
 * @param eventTime 事件时间
 * @author guohao.lu
 */
public record OrderChangeNotification(
        String notificationId,
        String sellerId,
        String marketplaceId,
        String amazonOrderId,
        String orderChangeType,
        Instant eventTime) {
}
