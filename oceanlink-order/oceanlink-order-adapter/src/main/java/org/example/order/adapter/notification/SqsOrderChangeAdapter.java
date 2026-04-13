package org.example.order.adapter.notification;

import java.util.Optional;

import org.example.order.app.notification.OrderNotificationCmdExe;
import org.example.order.client.command.OrderNotificationCmd;
import org.example.order.client.dto.OrderSyncTaskDTO;
import org.springframework.stereotype.Component;

/**
 * SQS 订单变更通知适配器。
 *
 * @author guohao.lu
 */
@Component
public class SqsOrderChangeAdapter {

    /**
     * 订单通知命令执行器。
     */
    private final OrderNotificationCmdExe orderNotificationCmdExe;

    /**
     * 创建 SQS 订单变更通知适配器。
     *
     * @param orderNotificationCmdExe 订单通知命令执行器
     */
    public SqsOrderChangeAdapter(OrderNotificationCmdExe orderNotificationCmdExe) {
        this.orderNotificationCmdExe = orderNotificationCmdExe;
    }

    /**
     * 处理订单通知命令。
     *
     * @param command 订单通知命令
     * @return 创建的同步任务
     */
    public Optional<OrderSyncTaskDTO> handle(OrderNotificationCmd command) {
        return orderNotificationCmdExe.execute(command);
    }
}
