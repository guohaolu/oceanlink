package org.example.order.domain.sync;

/**
 * 同步任务状态。
 *
 * @author guohao.lu
 */
public enum OrderSyncTaskStatus {

    /**
     * 待处理。
     */
    PENDING,

    /**
     * 执行中。
     */
    RUNNING,

    /**
     * 执行成功。
     */
    SUCCESS,

    /**
     * 执行失败。
     */
    FAILED
}
