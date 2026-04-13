package org.example.order.domain.sync.gateway;

import org.example.order.domain.sync.OrderSyncTask;

/**
 * 同步任务持久化网关。
 *
 * @author guohao.lu
 */
public interface OrderSyncTaskGateway {

    /**
     * 保存同步任务。
     *
     * @param orderSyncTask 同步任务
     */
    void save(OrderSyncTask orderSyncTask);
}
