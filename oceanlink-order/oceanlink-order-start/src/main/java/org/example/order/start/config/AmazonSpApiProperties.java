package org.example.order.start.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Amazon SP-API 配置。
 *
 * @param endpoint API 根地址
 * @param connectTimeoutSeconds 连接超时时间
 * @param readTimeoutSeconds 读取超时时间
 * @param defaultIncludedData 默认拉取数据块
 * @author guohao.lu
 */
@ConfigurationProperties(prefix = "order.amazon.sp-api")
public record AmazonSpApiProperties(
        String endpoint,
        int connectTimeoutSeconds,
        int readTimeoutSeconds,
        List<String> defaultIncludedData) {
}
