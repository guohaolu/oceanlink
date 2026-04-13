package org.example.order.domain.notification;

import java.time.Instant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 验证订单通知的幂等键与路由键生成规则。
 *
 * @author guohao.lu
 */
class OrderNotificationIdentityFactoryTest {

    /**
     * 验证通知幂等键直接复用 Amazon NotificationId。
     */
    @Test
    void shouldUseNotificationIdAsDedupKey() {
        OrderChangeNotification notification = new OrderChangeNotification(
                "notification-1",
                "seller-1",
                "ATVPDKIKX0DER",
                "112-1234567-1234567",
                "OrderStatusChange",
                Instant.parse("2026-04-13T10:15:30Z"));
        Assertions.assertEquals("notification-1", OrderNotificationIdentityFactory.dedupKey(notification));
    }

    /**
     * 验证通知路由键由卖家与站点共同组成。
     */
    @Test
    void shouldBuildRouteKeyWithSellerAndMarketplace() {
        OrderChangeNotification notification = new OrderChangeNotification(
                "notification-2",
                "seller-2",
                "A1VC38T7YXB528",
                "112-7654321-1234567",
                "BuyerRequestedChange",
                Instant.parse("2026-04-13T12:00:00Z"));
        Assertions.assertEquals("seller-2#A1VC38T7YXB528", OrderNotificationIdentityFactory.routeKey(notification));
    }
}
