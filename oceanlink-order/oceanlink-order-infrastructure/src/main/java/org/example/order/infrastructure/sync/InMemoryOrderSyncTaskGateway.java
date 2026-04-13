package org.example.order.infrastructure.sync;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.example.order.domain.sync.OrderSyncTask;
import org.example.order.domain.sync.gateway.OrderSyncTaskGateway;
import org.springframework.stereotype.Repository;

/**
 * 基于内存的同步任务网关实现。
 *
 * @author guohao.lu
 */
@Repository
public class InMemoryOrderSyncTaskGateway implements OrderSyncTaskGateway {

    /**
     * 任务缓存。
     */
    private final Map<String, OrderSyncTask> tasks = new ConcurrentHashMap<>();

    /**
     * 保存同步任务。
     *
     * @param orderSyncTask 同步任务
     */
    @Override
    public void save(OrderSyncTask orderSyncTask) {
        tasks.put(orderSyncTask.taskId(), orderSyncTask);
    }
}
