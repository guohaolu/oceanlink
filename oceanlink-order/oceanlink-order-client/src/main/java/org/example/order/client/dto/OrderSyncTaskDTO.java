package org.example.order.client.dto;

/**
 * 同步任务传输对象。
 *
 * @param taskId 任务标识
 * @param amazonOrderId Amazon 订单号
 * @param taskType 任务类型
 * @param status 任务状态
 * @author guohao.lu
 */
public record OrderSyncTaskDTO(
        String taskId,
        String amazonOrderId,
        String taskType,
        String status) {
}
