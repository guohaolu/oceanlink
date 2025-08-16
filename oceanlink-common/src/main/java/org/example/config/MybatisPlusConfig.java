package org.example.config;

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.example.mybatis.ClickHouseSqlInjector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

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

    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider databaseIdProvider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("clickhouse", "mysql");
        databaseIdProvider.setProperties(properties);
        return databaseIdProvider;
    }
}
