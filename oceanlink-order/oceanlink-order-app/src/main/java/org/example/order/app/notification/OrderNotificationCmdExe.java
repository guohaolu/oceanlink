package org.example.order.app.notification;

import java.util.Optional;
import java.util.UUID;

import org.example.order.client.command.OrderNotificationCmd;
import org.example.order.client.dto.OrderSyncTaskDTO;
import org.example.order.domain.notification.OrderChangeNotification;
import org.example.order.domain.notification.gateway.OrderChangeNotificationGateway;
import org.example.order.domain.sync.OrderSyncTask;
import org.example.order.domain.sync.OrderSyncTaskSourceType;
import org.example.order.domain.sync.OrderSyncTaskStatus;
import org.example.order.domain.sync.OrderSyncTaskType;
import org.example.order.domain.sync.gateway.OrderSyncTaskGateway;
import org.springframework.stereotype.Service;

/**
 * 订单通知命令执行器。
 *
 * @author guohao.lu
 */
@Service
public class OrderNotificationCmdExe {

    /**
     * 订单通知持久化网关。
     */
    private final OrderChangeNotificationGateway orderChangeNotificationGateway;

    /**
     * 同步任务持久化网关。
     */
    private final OrderSyncTaskGateway orderSyncTaskGateway;

    /**
     * 创建订单通知命令执行器。
     *
     * @param orderChangeNotificationGateway 订单通知持久化网关
     * @param orderSyncTaskGateway 同步任务持久化网关
     */
    public OrderNotificationCmdExe(
            OrderChangeNotificationGateway orderChangeNotificationGateway,
            OrderSyncTaskGateway orderSyncTaskGateway) {
        this.orderChangeNotificationGateway = orderChangeNotificationGateway;
        this.orderSyncTaskGateway = orderSyncTaskGateway;
    }

    /**
     * 执行订单通知命令。
     *
     * @param command 通知命令
     * @return 新创建的同步任务，若通知已存在则返回空
     */
    public Optional<OrderSyncTaskDTO> execute(OrderNotificationCmd command) {
        if (orderChangeNotificationGateway.existsByNotificationId(command.notificationId())) {
            return Optional.empty();
        }
        OrderChangeNotification notification = new OrderChangeNotification(
                command.notificationId(),
                command.sellerId(),
                command.marketplaceId(),
                command.amazonOrderId(),
                command.orderChangeType().amazonValue(),
                command.eventTime());
        orderChangeNotificationGateway.save(notification);
        OrderSyncTask orderSyncTask = new OrderSyncTask(
                UUID.randomUUID().toString(),
                command.sellerId(),
                command.marketplaceId(),
                command.amazonOrderId(),
                OrderSyncTaskType.NOTIFICATION_DRIVEN,
                OrderSyncTaskSourceType.SQS,
                OrderSyncTaskStatus.PENDING,
                command.eventTime());
        orderSyncTaskGateway.save(orderSyncTask);
        return Optional.of(new OrderSyncTaskDTO(
                orderSyncTask.taskId(),
                orderSyncTask.amazonOrderId(),
                orderSyncTask.taskType().name(),
                orderSyncTask.status().name()));
    }
}
