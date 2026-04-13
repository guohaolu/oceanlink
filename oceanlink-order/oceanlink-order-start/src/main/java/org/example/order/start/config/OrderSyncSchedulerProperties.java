package org.example.order.start.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 订单同步调度配置。
 *
 * @param backfillCron 历史回补 cron
 * @param reconciliationCron 轮询补偿 cron
 * @param repairCron 修复任务 cron
 * @param outboxRelayCron Outbox 投递 cron
 * @author guohao.lu
 */
@ConfigurationProperties(prefix = "order.sync.scheduler")
public record OrderSyncSchedulerProperties(
        String backfillCron,
        String reconciliationCron,
        String repairCron,
        String outboxRelayCron) {
}
