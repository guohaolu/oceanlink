package org.example.manager;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import org.example.util.SftpTemplate;

import java.io.InputStream;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * 语义层
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
}
