package org.example.template;

import com.jcraft.jsch.ChannelSftp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.example.configuration.PooledSftpInputStream;

import java.io.InputStream;
import java.util.function.Function;

/**
 * 核心执行模板（borrow / return）
 * <p>
 * 池感知的“安全访问层”
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

