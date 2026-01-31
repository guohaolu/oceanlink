package org.example.configuration;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.util.Properties;

/**
 * 最底层的“原生连接制造商”
 * <p>
 *     职责是原生的创建出连接出来,并且负责“销毁”和“鉴定”连接是否还活着
 * </p>
 *
 * @author guohao.lu
 */
@Slf4j
public class SftpConnectionFactory extends BasePooledObjectFactory<ChannelSftp> {

    private final NasProperties properties;

    public SftpConnectionFactory(NasProperties properties) {
        this.properties = properties;
    }

    @Override
    public ChannelSftp create() throws Exception {
        JSch jsch = new JSch();

        Session session = jsch.getSession(
                properties.getUsername(),
                properties.getHost(),
                properties.getPort()
        );
        session.setPassword(properties.getPassword());

        Properties props = new Properties();
        props.put("StrictHostKeyChecking", "no");
        session.setConfig(props);

        // 会话级超时
        session.connect(30_000);

        Channel channel = session.openChannel("sftp");
        channel.connect(30_000);

        log.debug("SFTP connected to {}", properties.getHost());
        return (ChannelSftp) channel;
    }

    @Override
    public PooledObject<ChannelSftp> wrap(ChannelSftp sftp) {
        return new DefaultPooledObject<>(sftp);
    }

    /**
     * 校验一定要“偏保守”
     */
    @Override
    public boolean validateObject(PooledObject<ChannelSftp> pooled) {
        ChannelSftp sftp = pooled.getObject();
        try {
            if (sftp == null || !sftp.isConnected()) {
                return false;
            }
            Session session = sftp.getSession();
            return session != null && session.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 销毁要「彻底」
     */
    @Override
    public void destroyObject(PooledObject<ChannelSftp> pooled) {
        ChannelSftp sftp = pooled.getObject();
        if (sftp == null) {
            return;
        }

        try {
            Session session = sftp.getSession();

            if (sftp.isConnected()) {
                sftp.disconnect();
            }

            if (session != null && session.isConnected()) {
                session.disconnect();
            }

            log.debug("销毁 SFTP 连接");
        } catch (Exception e) {
            log.warn("销毁 SFTP 连接异常", e);
        }
    }
}
