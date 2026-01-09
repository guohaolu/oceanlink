package org.example.mybatis;

import org.apache.ibatis.plugin.Interceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author guohao.lu
 */
@Configuration
public class MyBatisConfig {
    @Bean
    public Interceptor ClickHouseSqlTraceInterceptor() {
        return new ClickHouseSqlTraceInterceptor();
    }
}
