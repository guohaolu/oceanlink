package org.example.order.start.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Amazon SQS 配置。
 *
 * @param queueArn 队列 ARN
 * @param queueUrl 队列 URL
 * @param maxMessages 单次拉取数量
 * @param waitTimeSeconds 长轮询时间
 * @param visibilityTimeoutSeconds 可见性超时时间
 * @author guohao.lu
 */
@ConfigurationProperties(prefix = "order.amazon.sqs")
public record AmazonSqsProperties(
        String queueArn,
        String queueUrl,
        int maxMessages,
        int waitTimeSeconds,
        int visibilityTimeoutSeconds) {
}
