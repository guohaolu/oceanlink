package org.example.order.adapter.web;

import java.util.Optional;

import org.example.order.adapter.notification.SqsOrderChangeAdapter;
import org.example.order.client.command.OrderNotificationCmd;
import org.example.order.client.dto.OrderSyncTaskDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单通知管理控制器。
 *
 * @author guohao.lu
 */
@RestController
@RequestMapping("/admin/order/amazon/notifications")
public class OrderNotificationController {

    /**
     * SQS 订单变更通知适配器。
     */
    private final SqsOrderChangeAdapter sqsOrderChangeAdapter;

    /**
     * 创建订单通知管理控制器。
     *
     * @param sqsOrderChangeAdapter SQS 订单变更通知适配器
     */
    public OrderNotificationController(SqsOrderChangeAdapter sqsOrderChangeAdapter) {
        this.sqsOrderChangeAdapter = sqsOrderChangeAdapter;
    }

    /**
     * 提交一条订单通知命令用于本地联调。
     *
     * @param command 订单通知命令
     * @return 创建的同步任务
     */
    @PostMapping("/mock")
    public Optional<OrderSyncTaskDTO> submitMockNotification(@RequestBody OrderNotificationCmd command) {
        return sqsOrderChangeAdapter.handle(command);
    }
}
