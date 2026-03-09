package org.example.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

/**
 * 库存 Redis 配置：加载 Lua 脚本等
 */
@Configuration
public class InventoryRedisConfig {

    private static final String DEDUCT_SCRIPT_PATH = "scripts/deduct_stock.lua";

    @Bean
    public DefaultRedisScript<Long> deductStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(DEDUCT_SCRIPT_PATH));
        script.setResultType(Long.class);
        return script;
    }
}
