package org.example.config;

import org.example.mybatis.ClickHouseSqlInjector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * mybatis-plus 配置类
 *
 * @author guohao.lu
 */
@Configuration
public class MybatisPlusConfig {
    /**
     * Clickhouse sql注入器
     *
     * @return Clickhouse sql注入器
     */
    @Bean
    public ClickHouseSqlInjector clickHouseSqlInjector() {
        return new ClickHouseSqlInjector();
    }
}
