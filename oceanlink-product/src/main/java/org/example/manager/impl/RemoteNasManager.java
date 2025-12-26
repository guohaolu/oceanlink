package org.example.manager.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.manager.NasOperations;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 基础设施装配者（Assembler）
 * <p>
 * 它的职责只有三件事：
 * <p>
 * 创建连接池（pool）
 * <p>
 * 用 pool 构建 template
 * <p>
 * 用 template 构建 operations
 * <p>
 * 它不应该：
 * <p>
 * 管 SFTP 细节
 * <p>
 * 管异常策略
 * <p>
 * 管文件操作语义
 * <p>
 * 它只是把“零件”装成“机器”。这是标准的 Infrastructure Bootstrapper。
 *
 * @author guohao.lu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteNasManager {
    private final NasOperations nasOperations;

    /**
     * 获取指定路径文件的输入流
     * @param path 文件路径
     * @return 文件输入流
     */
    public InputStream getFileStream(String path) {
        return nasOperations.openStream(path);
    }

    /**
     * 列出指定目录下的文件列表
     * @param dir 目录路径
     * @return 文件名列表
     */
    public List<String> list(String dir) {
        return nasOperations.list(dir);
    }

    /**
     * 删除指定路径的文件或目录
     * @param path 要删除的文件或目录路径
     */
    public void delete(String path) {
        nasOperations.delete(path);
    }

    /**
     * 检查指定路径的文件或目录是否存在
     * @param path 要检查的路径
     * @return 存在返回true，否则返回false
     */
    public boolean exists(String path) {
        return nasOperations.exists(path);
    }
}

