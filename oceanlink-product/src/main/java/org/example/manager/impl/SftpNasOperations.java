package org.example.manager.impl;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import org.example.manager.NasOperations;
import org.example.template.SftpTemplate;

import java.io.InputStream;
import java.util.List;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * NAS 操作的语义层实现，角色等同于 Repository。
 *
 * <p>
 * 本类在 {@link NasOperations} 之上提供<strong>稳定、面向使用方的文件操作语义</strong>，
 * 将底层 SFTP/NAS 的协议细节、连接管理与异常处理统一封装。
 *
 * <p>
 * 在分层职责上：
 * <ul>
 *   <li>上游调用方通过本类表达“要做什么文件操作”</li>
 *   <li>底层基础设施负责“如何与 NAS/SFTP 通信”</li>
 * </ul>
 *
 * <p>
 * 因此，本类在职责定位上等同于 Repository：
 * <strong>负责语义稳定性与边界约束</strong>，
 * 而不是底层资源的创建或管理。
 *
 * @author guohao.lu
 */
@Slf4j
public class SftpNasOperations implements NasOperations {

    private final SftpTemplate sftpTemplate;

    public SftpNasOperations(SftpTemplate sftpTemplate) {
        this.sftpTemplate = sftpTemplate;
    }

    @Override
    public List<String> list(String path) {
        return sftpTemplate.execute(sftp -> {
            try {
                @SuppressWarnings("unchecked")
                Vector<ChannelSftp.LsEntry> entries = sftp.ls(path);
                return entries.stream()
                        .map(ChannelSftp.LsEntry::getFilename)
                        .filter(name -> !".".equals(name) && !"..".equals(name))
                        .map(name -> path + "/" + name)
                        .collect(Collectors.toList());
            } catch (SftpException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void walkFiles(String path, Consumer<String> fileConsumer) {
        sftpTemplate.execute(sftp -> {
            try {
                walk(sftp, path, fileConsumer);
                return null;
            } catch (SftpException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public boolean exists(String path) {
        return sftpTemplate.execute(sftp -> {
            try {
                sftp.stat(path);
                return true;
            } catch (SftpException e) {
                return false;
            }
        });
    }

    @Override
    public void delete(String path) {
        sftpTemplate.execute(sftp -> {
            try {
                sftp.rm(path);
                return null;
            } catch (SftpException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void mkdirs(String path) {
        sftpTemplate.execute(sftp -> {
            try {
                sftp.mkdir(path);
                return null;
            } catch (SftpException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public InputStream openStream(String path) {
        return sftpTemplate.openStream(sftp -> {
            try {
                return sftp.get(path);
            } catch (SftpException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 递归遍历SFTP服务器上的目录结构
     *
     * @param sftp SFTP通道对象，用于执行文件操作
     * @param path 要遍历的起始路径
     * @param fileConsumer 对每个找到的文件执行的操作回调函数
     * @throws SftpException 当SFTP操作发生异常时抛出
     */
    private void walk(ChannelSftp sftp, String path, Consumer<String> fileConsumer) throws SftpException {
        @SuppressWarnings("unchecked")
        Vector<ChannelSftp.LsEntry> entries = sftp.ls(path);

        for (ChannelSftp.LsEntry entry : entries) {
            String name = entry.getFilename();
            if (".".equals(name) || "..".equals(name)) {
                continue;
            }

            String fullPath = path + "/" + name;

            if (entry.getAttrs().isDir()) {
                // 递归处理子目录
                walk(sftp, fullPath, fileConsumer);
            } else {
                // 处理文件，调用回调函数
                fileConsumer.accept(fullPath);
            }
        }
    }

}
