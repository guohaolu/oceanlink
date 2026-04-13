package org.example.order.domain.sync;

/**
 * 同步任务类型。
 *
 * @author guohao.lu
 */
public enum OrderSyncTaskType {

    /**
     * 通知驱动任务。
     */
    NOTIFICATION_DRIVEN,

    /**
     * 历史回补任务。
     */
    BACKFILL,

    /**
     * 修复任务。
     */
    REPAIR
}
