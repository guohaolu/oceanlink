package org.example.order.domain.notification.gateway;

import org.example.order.domain.notification.OrderChangeNotification;

/**
 * 订单通知持久化网关。
 *
 * @author guohao.lu
 */
public interface OrderChangeNotificationGateway {

    /**
     * 判断指定通知是否已存在。
     *
     * @param notificationId 通知标识
     * @return 已存在返回 true
     */
    boolean existsByNotificationId(String notificationId);

    /**
     * 保存订单变更通知。
     *
     * @param orderChangeNotification 订单变更通知
     */
    void save(OrderChangeNotification orderChangeNotification);
}
