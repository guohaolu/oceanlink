package org.example.configuration;

import com.jcraft.jsch.ChannelSftp;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.example.repository.NasOperations;
import org.example.repository.impl.SftpNasOperations;
import org.example.template.SftpTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * NAS（Network Attached Storage）SFTP 自动配置类。
 *
 * <p>
 * 该自动配置负责完成以下工作：
 * <ul>
 *     <li>基于 {@link NasProperties} 初始化 SFTP 连接工厂</li>
 *     <li>构建并管理 {@link ChannelSftp} 的对象池（连接池）</li>
 *     <li>提供 {@link SftpTemplate} 作为 SFTP 操作的基础封装</li>
 *     <li>对外暴露 {@link NasOperations}，作为统一的 NAS 访问抽象</li>
 * </ul>
 *
 * <p>
 * 设计目标：
 * <ul>
 *     <li>避免频繁创建 / 销毁 SFTP 连接带来的性能与稳定性问题</li>
 *     <li>通过连接池机制应对 JSch 连接易断、NAS 主动断连等不稳定场景</li>
 *     <li>将 SFTP 细节与业务代码解耦，业务侧只感知 {@link NasOperations}</li>
 * </ul>
 *
 * <p>
 * 该配置类通常作为 starter 的一部分引入，在 Spring 容器启动时自动生效。
 *
 * @author guohao.lu
 */
@Configuration
@EnableConfigurationProperties(NasProperties.class)
public class NasAutoConfiguration {

    /**
     * 创建 SFTP 连接池。
     *
     * <p>
     * 该 Bean 使用 {@link GenericObjectPool} 对 {@link ChannelSftp} 进行池化管理，
     * 以降低连接创建成本，并提升高并发场景下的稳定性。
     *
     * <p>
     * 连接池的关键策略包括：
     * <ul>
     *     <li>借出、归还、空闲时均进行连接有效性校验</li>
     *     <li>定期驱逐空闲且可能已失效的连接</li>
     *     <li>限制最大等待时间，防止线程在池耗尽时永久阻塞</li>
     * </ul>
     *
     * <p>
     * Bean 在容器销毁时会自动调用 {@code close()} 方法，
     * 释放池中所有 SFTP 连接资源。
     *
     * @param properties NAS 配置属性（包含 SFTP 连接信息及连接池参数）
     * @return {@link ChannelSftp} 的对象池
     */
    @Bean(destroyMethod = "close")
    public GenericObjectPool<ChannelSftp> sftpPool(NasProperties properties) {
        SftpConnectionFactory factory = new SftpConnectionFactory(properties);
        GenericObjectPoolConfig<ChannelSftp> cfg = buildPoolConfig(properties);
        cfg.setJmxEnabled(false);
        return new GenericObjectPool<>(factory, cfg);
    }

    /**
     * 创建 SFTP 模板对象。
     *
     * <p>
     * {@link SftpTemplate} 是对底层 SFTP 操作的轻量封装，
     * 负责从连接池中获取 / 归还 {@link ChannelSftp}，
     * 并屏蔽连接管理细节。
     *
     * <p>
     * 业务代码不应直接操作连接池，而应通过该模板完成文件操作。
     *
     * @param pool SFTP 连接池
     * @return SFTP 操作模板
     */
    @Bean
    public SftpTemplate sftpTemplate(@Qualifier("sftpPool") GenericObjectPool<ChannelSftp> pool) {
        return new SftpTemplate(pool);
    }

    /**
     * 创建 NAS 操作抽象实现。
     *
     * <p>
     * {@link NasOperations} 是面向业务层的统一接口，
     * 封装了与 NAS / SFTP 相关的具体实现细节。
     *
     * <p>
     * 通过该抽象，可以：
     * <ul>
     *     <li>避免业务代码直接依赖 SFTP 协议</li>
     *     <li>在未来替换存储实现（如对象存储）时减少侵入性</li>
     * </ul>
     *
     * @param template SFTP 操作模板
     * @return NAS 操作接口实现
     */
    @Bean
    public NasOperations nasOperations(@Qualifier("sftpTemplate") SftpTemplate template) {
        return new SftpNasOperations(template);
    }

    /**
     * 构建 SFTP 连接池配置。
     *
     * <p>
     * 该方法集中定义了连接池的核心行为策略，主要针对以下风险场景：
     * <ul>
     *     <li>JSch 连接偶发断线但客户端无感知</li>
     *     <li>NAS 服务端长时间空闲后主动断开连接</li>
     *     <li>连接池耗尽导致业务线程长时间阻塞甚至雪崩</li>
     * </ul>
     *
     * <p>
     * 因此配置上强调：
     * <ul>
     *     <li>借出、归还、空闲阶段均进行连接校验</li>
     *     <li>定期执行驱逐线程清理失效连接</li>
     *     <li>限制最大等待时间，避免线程无限期等待</li>
     * </ul>
     *
     * @param props NAS 配置属性
     * @return {@link ChannelSftp} 对象池配置
     */
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
