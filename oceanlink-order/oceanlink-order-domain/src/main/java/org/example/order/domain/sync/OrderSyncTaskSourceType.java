package org.example.order.domain.sync;

/**
 * 同步任务来源类型。
 *
 * @author guohao.lu
 */
public enum OrderSyncTaskSourceType {

    /**
     * SQS 通知来源。
     */
    SQS,

    /**
     * 定时调度来源。
     */
    SCHEDULE,

    /**
     * 人工触发来源。
     */
    MANUAL
}
