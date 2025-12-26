package org.example.configuration;

import com.jcraft.jsch.ChannelSftp;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.example.manager.NasOperations;
import org.example.manager.impl.SftpNasOperations;
import org.example.template.SftpTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * @author guohao.lu
 */
@Configuration
@EnableConfigurationProperties(NasProperties.class)
public class NasAutoConfiguration {

    @Bean(destroyMethod = "close")
    public GenericObjectPool<ChannelSftp> sftpPool(NasProperties properties) {
        SftpConnectionFactory factory = new SftpConnectionFactory(properties);
        GenericObjectPoolConfig<ChannelSftp> cfg = buildPoolConfig(properties);
        cfg.setJmxEnabled(false);
        return new GenericObjectPool<>(factory, cfg);
    }

    @Bean
    public SftpTemplate sftpTemplate(@Qualifier("sftpPool") GenericObjectPool<ChannelSftp> pool) {
        return new SftpTemplate(pool);
    }

    @Bean
    public NasOperations nasOperations(@Qualifier("sftpTemplate") SftpTemplate template) {
        return new SftpNasOperations(template);
    }

    private GenericObjectPoolConfig<ChannelSftp> buildPoolConfig(NasProperties props) {
        GenericObjectPoolConfig<ChannelSftp> config = new GenericObjectPoolConfig<>();

        config.setMaxTotal(props.getPool().getMaxTotal());
        config.setMaxIdle(props.getPool().getMaxIdle());
        config.setMinIdle(props.getPool().getMinIdle());
        config.setMaxWait(props.getPool().getMaxWait());

        // 借出、归还、空闲都要校验
        // JSch 偶发断线是常态，不校验等于裸奔
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        // NAS 半夜踢连接，这个能救你
        config.setTestWhileIdle(true);

        // 驱逐线程
        config.setTimeBetweenEvictionRuns(Duration.ofMinutes(1));
        config.setMinEvictableIdleTime(Duration.ofMinutes(5));

        // 防止线程被“池耗尽”永久卡死,防止业务线程雪崩
        config.setMaxWait(Duration.ofSeconds(10));

        return config;
    }
}
