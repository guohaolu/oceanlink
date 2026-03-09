package org.example.inventory.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 合并扣减配置
 * 控制合并窗口与落库策略
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "inventory.merge-deduct")
public class MergeDeductConfig {

    /** 合并窗口间隔（毫秒），到达后触发一次落库 */
    private long flushIntervalMs = 500L;
    /** 单 SKU 最大合并条数，超过则提前触发落库 */
    private int maxMergeCountPerSku = 100;
    /** 是否启用合并扣减（关闭则每次扣减直接写 DB） */
    private boolean enabled = true;
}
