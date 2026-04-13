package org.example.order.client.command;

import java.time.Instant;

import org.example.order.client.enums.OrderChangeType;

/**
 * 订单变更通知命令。
 *
 * @param notificationId Amazon 通知标识
 * @param sellerId Amazon SellerId
 * @param marketplaceId Amazon MarketplaceId
 * @param amazonOrderId Amazon 订单号
 * @param orderChangeType 订单变更类型
 * @param eventTime 通知事件时间
 * @author guohao.lu
 */
public record OrderNotificationCmd(
        String notificationId,
        String sellerId,
        String marketplaceId,
        String amazonOrderId,
        OrderChangeType orderChangeType,
        Instant eventTime) {
}
