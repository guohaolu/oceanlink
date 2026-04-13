package org.example.order.domain.notification;

/**
 * 订单通知标识工厂。
 *
 * @author guohao.lu
 */
public final class OrderNotificationIdentityFactory {

    /**
     * 私有构造函数。
     */
    private OrderNotificationIdentityFactory() {
    }

    /**
     * 生成通知幂等键。
     *
     * @param notification 通知对象
     * @return 幂等键
     */
    public static String dedupKey(OrderChangeNotification notification) {
        return notification.notificationId();
    }

    /**
     * 生成通知路由键。
     *
     * @param notification 通知对象
     * @return 路由键
     */
    public static String routeKey(OrderChangeNotification notification) {
        return notification.sellerId() + "#" + notification.marketplaceId();
    }
}
