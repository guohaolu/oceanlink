package org.example.manager;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author guohao.lu
 */
@Slf4j
@Component
public class RemoteNasManager {
    @Value("${ewayt.nas.server:172.16.2.222}")
    private String nasServer;

    @Value("${ewayt.nas.port:22}")
    private int nasPort;

    @Value("${ewayt.nas.username:test_finance}")
    private String nasUserName;

    @Value("${ewayt.nas.password:~Cq*8_Jd}")
    private String nasUserPassword;

    @Value("${ewayt.nas.remoteDir:/IT/财务/基础数据}")
    private String nasRemoteDir;

    @Value("${ewayt.nas.localDir:/data/upload_tmp/report}")
    private String nasLocalDir;

    @Value("${ewayt.nas.pool.maxTotal:8}")
    private int maxTotal;

    @Value("${ewayt.nas.pool.maxIdle:8}")
    private int maxIdle;

    @Value("${ewayt.nas.pool.minIdle:2}")
    private int minIdle;

    private GenericObjectPool<ChannelSftp> sftpPool;

    /**
     * SFTP连接包装类，实现AutoCloseable接口
     */
    private class SftpConnection implements AutoCloseable {
        private final GenericObjectPool<ChannelSftp> pool;
        private final ChannelSftp sftp;

        public SftpConnection(GenericObjectPool<ChannelSftp> pool) throws Exception {
            this.pool = pool;
            this.sftp = pool.borrowObject();
        }

        public ChannelSftp getSftp() {
            return sftp;
        }

        @Override
        public void close() {
            if (sftp != null) {
                pool.returnObject(sftp);
            }
        }
    }

    @PostConstruct
    public void init() {
        // 创建文件目录
        createLocalDirectory();

        // 初始化连接池配置
        GenericObjectPoolConfig<ChannelSftp> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);
        config.setTimeBetweenEvictionRuns(Duration.ofMinutes(1));
        config.setMinEvictableIdleTime(Duration.ofMinutes(5));
        // 最大等待时间10秒
        config.setMaxWait(Duration.ofSeconds(10));

        sftpPool = new GenericObjectPool<>(new SftpConnectionFactory(), config);

        // 预热连接池
        try {
            for (int i = 0; i < minIdle; i++) {
                sftpPool.addObject();
            }
        } catch (Exception e) {
            log.error("初始化SFTP连接池失败", e);
            throw new RuntimeException("初始化SFTP连接池失败", e);
        }
    }

    private void createLocalDirectory() {
        File localDir = new File(nasLocalDir);
        if (!localDir.exists() && !localDir.mkdirs()) {
            log.error("创建本地目录失败: {}", nasLocalDir);
            throw new RuntimeException("创建本地目录失败");
        }
    }

    public InputStream getFileStream(String filePath) throws Exception {
        try (SftpConnection conn = new SftpConnection(sftpPool)) {
            return conn.getSftp().get(filePath);
        }
    }

    public ChannelSftp getChannelSftp() throws Exception {
        try (SftpConnection conn = new SftpConnection(sftpPool)) {
            return conn.getSftp();
        }
    }

    @PreDestroy
    public void disconnect() {
        if (sftpPool != null) {
            sftpPool.close();
            log.info("SFTP连接池已关闭");
        }
    }

    public Boolean isNasAvailable() {
        try (SftpConnection conn = new SftpConnection(sftpPool)) {
            return conn.getSftp().isConnected();
        } catch (Exception e) {
            log.error("检查NAS可用性失败", e);
            return false;
        }
    }

    public List<String> listNasFiles(String remoteDir) {
        try (SftpConnection conn = new SftpConnection(sftpPool)) {
            return getDirectoryFileList(conn.getSftp(), remoteDir);
        } catch (Exception e) {
            log.error("获取文件列表失败: {} {}", remoteDir, e.getMessage());

            // throw new RuntimeException("获取文件列表失败", e);
        }
        return new ArrayList<>();
    }

    public File downloadNasFile(String filePath) {
        try (SftpConnection conn = new SftpConnection(sftpPool)) {
            Path path = Paths.get(filePath);
            String fileName = path.getFileName().toString();
            File localFile = new File(nasLocalDir, fileName);

            try (InputStream inputStream = conn.getSftp().get(filePath);
                 FileOutputStream outputStream = new FileOutputStream(localFile)) {
                byte[] buffer = new byte[8192];
                int readCount;
                while ((readCount = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, readCount);
                }
                log.info("文件已下载: {}", localFile.getAbsolutePath());
                return localFile;
            }
        } catch (Exception e) {
            log.error("下载文件失败: {}", e.getMessage());
            throw new RuntimeException("文件下载失败", e);
        }
    }

    public byte[] downloadNasFileByteArray(String filePath) {
        try (SftpConnection conn = new SftpConnection(sftpPool)) {
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                 InputStream inputStream = conn.getSftp().get(filePath)) {
                byte[] buffer = new byte[8192];
                int readCount;
                while ((readCount = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, readCount);
                }
                log.info("文件已下载到内存: {}", Paths.get(filePath).getFileName());
                return byteArrayOutputStream.toByteArray();
            }
        } catch (Exception e) {
            log.error("下载文件到内存失败: {}", e.getMessage());
            throw new RuntimeException("文件下载失败", e);
        }
    }

    private List<String> getDirectoryFileList(ChannelSftp sftp, String remoteDir) throws SftpException {
        List<String> fileList = new ArrayList<>();
        sftp.cd(remoteDir);

        @SuppressWarnings("unchecked")
        Vector<ChannelSftp.LsEntry> list = sftp.ls(remoteDir);

        for (ChannelSftp.LsEntry entry : list) {
            String filename = entry.getFilename();
            if (".".equals(filename) || "..".equals(filename)) {
                continue;
            }

            String fullPath = remoteDir + "/" + filename;
            if (entry.getAttrs().isDir()) {
                fileList.addAll(getDirectoryFileList(sftp, fullPath));
            } else {
                fileList.add(fullPath);
            }
        }
        return fileList;
    }

    private class SftpConnectionFactory extends BasePooledObjectFactory<ChannelSftp> {

        @Override
        public ChannelSftp create() throws Exception {
            try {
                JSch jsch = new JSch();
                Session session = jsch.getSession(nasUserName, nasServer, nasPort);
                session.setPassword(nasUserPassword);

                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);

                session.connect(30000); // 30秒超时

                Channel channel = session.openChannel("sftp");
                channel.connect(30000);

                log.debug("创建新的SFTP连接");
                return (ChannelSftp) channel;
            } catch (JSchException e) {
                log.error("创建SFTP连接失败", e);
                throw new RuntimeException("创建SFTP连接失败", e);
            }
        }

        @Override
        public PooledObject<ChannelSftp> wrap(ChannelSftp channelSftp) {
            return new DefaultPooledObject<>(channelSftp);
        }

        @Override
        public boolean validateObject(PooledObject<ChannelSftp> pooledObject) {
            ChannelSftp channelSftp = pooledObject.getObject();
            return channelSftp != null && channelSftp.isConnected() && !channelSftp.isClosed();
        }

        @Override
        public void destroyObject(PooledObject<ChannelSftp> pooledObject) {
            ChannelSftp channelSftp = pooledObject.getObject();
            if (channelSftp != null) {
                Session session = null;
                try {
                    session = channelSftp.getSession();
                } catch (JSchException e) {
                    throw new RuntimeException(e);
                }
                if (channelSftp.isConnected()) {
                    channelSftp.disconnect();
                }
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
                log.debug("销毁SFTP连接");
            }
        }
    }
}
