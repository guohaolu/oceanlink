package org.example.configuration;

import com.jcraft.jsch.ChannelSftp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * SFTP输入流的池化实现，包装了原始的InputStream和SftpConnection
 * 在关闭时会同时关闭底层输入流和SFTP连接
 * <p>
 *     InputStream 正确归还连接
 * </p>
 *
 * @author guohao.lu
 */
@Slf4j
public class PooledSftpInputStream extends FilterInputStream {

    private final GenericObjectPool<ChannelSftp> pool;
    private final ChannelSftp sftp;
    private boolean closed = false;

    public PooledSftpInputStream(
            InputStream in,
            GenericObjectPool<ChannelSftp> pool,
            ChannelSftp sftp) {
        super(in);
        this.pool = pool;
        this.sftp = sftp;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;

        IOException ex = null;
        try {
            super.close();
        } catch (IOException e) {
            ex = e;
        }

        try {
            pool.returnObject(sftp);
        } catch (Exception e) {
            if (ex == null) {
                ex = new IOException("归还 SFTP 连接失败", e);
            }
        }

        if (ex != null) {
            throw ex;
        }
    }
}
