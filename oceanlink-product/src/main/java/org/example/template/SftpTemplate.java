package org.example.template;

import com.jcraft.jsch.ChannelSftp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.example.configuration.PooledSftpInputStream;

import java.io.InputStream;
import java.util.function.Function;

/**
 * SFTP 核心执行模板（borrow / return）。
 *
 * <p>
 * 本类是一个<strong>池感知的安全访问模板</strong>，
 * 负责从连接池中借出资源、执行操作，并在结束后确保资源被正确归还。
 *
 * <p>
 * 其核心职责是：
 * <ul>
 *   <li>封装连接的借用与归还（borrow / return）流程</li>
 *   <li>统一资源生命周期管理，防止泄漏</li>
 *   <li>为上层提供一个安全、可复用的执行入口</li>
 * </ul>
 *
 * <p>
 * 本类<strong>不表达任何业务或文件操作语义</strong>，
 * 也不关心调用方要执行什么具体的 SFTP 行为，
 * 仅保证“在池的约束下，执行是安全的”。
 *
 * <p>
 * 在分层结构中，本类位于基础设施与语义层之间，
 * 是一个典型的 Template / Executor 角色。
 *
 * @author guohao.lu
 */
@Slf4j
@RequiredArgsConstructor
public class SftpTemplate {
    private final GenericObjectPool<ChannelSftp> sftpPool;

    /**
     * 执行SFTP操作的通用方法
     * @param action 要执行的SFTP操作函数
     * @param <T> 返回值类型
     * @return 操作结果
     */
    public <T> T execute(Function<ChannelSftp, T> action) {
        ChannelSftp sftp = null;
        try {
            sftp = sftpPool.borrowObject();
            return action.apply(sftp);
        } catch (Exception e) {
            throw new RuntimeException("SFTP 操作失败", e);
        } finally {
            if (sftp != null) {
                sftpPool.returnObject(sftp);
            }
        }
    }

    /**
     * 专门给 InputStream 用（延迟归还）
     * @param action 要执行的SFTP流操作函数
     * @return 包装后的PooledSftpInputStream，支持延迟归还连接
     */
    public PooledSftpInputStream openStream(Function<ChannelSftp, InputStream> action) {
        ChannelSftp sftp;
        try {
            sftp = sftpPool.borrowObject();
            InputStream in = action.apply(sftp);
            return new PooledSftpInputStream(in, sftpPool, sftp);
        } catch (Exception e) {
            throw new RuntimeException("打开 SFTP 流失败", e);
        }
    }
}

