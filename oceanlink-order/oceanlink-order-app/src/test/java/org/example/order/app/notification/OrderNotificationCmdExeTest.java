package org.example.order.app.notification;

import java.time.Instant;
import java.util.Optional;

import org.example.order.client.command.OrderNotificationCmd;
import org.example.order.client.dto.OrderSyncTaskDTO;
import org.example.order.client.enums.OrderChangeType;
import org.example.order.domain.notification.OrderChangeNotification;
import org.example.order.domain.notification.gateway.OrderChangeNotificationGateway;
import org.example.order.domain.sync.OrderSyncTask;
import org.example.order.domain.sync.gateway.OrderSyncTaskGateway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 验证通知命令执行器。
 *
 * @author guohao.lu
 */
class OrderNotificationCmdExeTest {

    /**
     * 验证新通知会落库存储并创建通知驱动任务。
     */
    @Test
    void shouldCreateNotificationDrivenTaskForNewMessage() {
        InMemoryOrderChangeNotificationGateway notificationGateway = new InMemoryOrderChangeNotificationGateway();
        InMemoryOrderSyncTaskGateway taskGateway = new InMemoryOrderSyncTaskGateway();
        OrderNotificationCmdExe cmdExe = new OrderNotificationCmdExe(notificationGateway, taskGateway);
        OrderNotificationCmd command = new OrderNotificationCmd(
                "notification-1",
                "seller-1",
                "ATVPDKIKX0DER",
                "112-1234567-1234567",
                OrderChangeType.ORDER_STATUS_CHANGE,
                Instant.parse("2026-04-13T10:30:00Z"));

        Optional<OrderSyncTaskDTO> result = cmdExe.execute(command);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("112-1234567-1234567", result.get().amazonOrderId());
        Assertions.assertEquals("NOTIFICATION_DRIVEN", result.get().taskType());
        Assertions.assertEquals(1, notificationGateway.savedCount);
        Assertions.assertEquals(1, taskGateway.savedCount);
    }

    /**
     * 验证重复通知不会重复生成同步任务。
     */
    @Test
    void shouldIgnoreDuplicatedNotification() {
        InMemoryOrderChangeNotificationGateway notificationGateway = new InMemoryOrderChangeNotificationGateway();
        InMemoryOrderSyncTaskGateway taskGateway = new InMemoryOrderSyncTaskGateway();
        OrderNotificationCmdExe cmdExe = new OrderNotificationCmdExe(notificationGateway, taskGateway);
        OrderNotificationCmd command = new OrderNotificationCmd(
                "notification-1",
                "seller-1",
                "ATVPDKIKX0DER",
                "112-1234567-1234567",
                OrderChangeType.ORDER_STATUS_CHANGE,
                Instant.parse("2026-04-13T10:30:00Z"));

        cmdExe.execute(command);
        Optional<OrderSyncTaskDTO> result = cmdExe.execute(command);

        Assertions.assertTrue(result.isEmpty());
        Assertions.assertEquals(1, notificationGateway.savedCount);
        Assertions.assertEquals(1, taskGateway.savedCount);
    }

    /**
     * 基于内存实现通知存储网关。
     *
     * @author guohao.lu
     */
    private static final class InMemoryOrderChangeNotificationGateway implements OrderChangeNotificationGateway {

        /**
         * 记录保存次数。
         */
        private int savedCount;

        /**
         * 最近保存的通知。
         */
        private OrderChangeNotification notification;

        /**
         * 判断通知是否已存在。
         *
         * @param notificationId 通知标识
         * @return 已存在返回 true
         */
        @Override
        public boolean existsByNotificationId(String notificationId) {
            return this.notification != null && this.notification.notificationId().equals(notificationId);
        }

        /**
         * 保存通知。
         *
         * @param orderChangeNotification 通知对象
         */
        @Override
        public void save(OrderChangeNotification orderChangeNotification) {
            this.notification = orderChangeNotification;
            this.savedCount++;
        }
    }

    /**
     * 基于内存实现同步任务网关。
     *
     * @author guohao.lu
     */
    private static final class InMemoryOrderSyncTaskGateway implements OrderSyncTaskGateway {

        /**
         * 记录保存次数。
         */
        private int savedCount;

        /**
         * 最近保存的任务。
         */
        private OrderSyncTask task;

        /**
         * 保存同步任务。
         *
         * @param orderSyncTask 任务对象
         */
        @Override
        public void save(OrderSyncTask orderSyncTask) {
            this.task = orderSyncTask;
            this.savedCount++;
        }
    }
}
