package org.example.order.infrastructure.notification;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.example.order.domain.notification.OrderChangeNotification;
import org.example.order.domain.notification.gateway.OrderChangeNotificationGateway;
import org.springframework.stereotype.Repository;

/**
 * 基于内存的订单通知网关实现。
 *
 * @author guohao.lu
 */
@Repository
public class InMemoryOrderChangeNotificationGateway implements OrderChangeNotificationGateway {

    /**
     * 通知缓存。
     */
    private final Map<String, OrderChangeNotification> notifications = new ConcurrentHashMap<>();

    /**
     * 判断通知是否已存在。
     *
     * @param notificationId 通知标识
     * @return 已存在返回 true
     */
    @Override
    public boolean existsByNotificationId(String notificationId) {
        return notifications.containsKey(notificationId);
    }

    /**
     * 保存通知。
     *
     * @param orderChangeNotification 订单通知
     */
    @Override
    public void save(OrderChangeNotification orderChangeNotification) {
        notifications.put(orderChangeNotification.notificationId(), orderChangeNotification);
    }
}
