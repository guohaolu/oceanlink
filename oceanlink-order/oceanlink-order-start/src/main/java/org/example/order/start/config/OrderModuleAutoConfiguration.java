package org.example.order.start.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 订单模块自动配置。
 *
 * @author guohao.lu
 */
@Configuration
@EnableConfigurationProperties({
        AmazonSpApiProperties.class,
        AmazonSqsProperties.class,
        OrderSyncSchedulerProperties.class
})
public class OrderModuleAutoConfiguration {
}
