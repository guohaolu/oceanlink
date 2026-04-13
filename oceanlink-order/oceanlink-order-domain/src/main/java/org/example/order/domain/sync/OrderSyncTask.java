package org.example.order.domain.sync;

import java.time.Instant;

/**
 * 订单同步任务。
 *
 * @param taskId 任务标识
 * @param sellerId Seller 标识
 * @param marketplaceId 站点标识
 * @param amazonOrderId Amazon 订单号
 * @param taskType 任务类型
 * @param sourceType 任务来源
 * @param status 任务状态
 * @param createdAt 创建时间
 * @author guohao.lu
 */
public record OrderSyncTask(
        String taskId,
        String sellerId,
        String marketplaceId,
        String amazonOrderId,
        OrderSyncTaskType taskType,
        OrderSyncTaskSourceType sourceType,
        OrderSyncTaskStatus status,
        Instant createdAt) {
}
