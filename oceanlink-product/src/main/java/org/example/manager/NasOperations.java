package org.example.manager;

import java.io.InputStream;
import java.util.List;

/**
 * NAS操作接口
 * 提供对网络附加存储(NAS)的基本文件操作功能
 *
 * @author guohao.lu
 */
public interface NasOperations {
    /**
     * 列出指定路径下的文件和目录
     *
     * @param path 要列出内容的目录路径
     * @return 文件和目录名称列表，如果路径不存在或无内容则返回空列表
     */
    List<String> list(String path);

    /**
     * 检查指定路径是否存在
     *
     * @param path 要检查的路径
     * @return 如果路径存在返回true，否则返回false
     */
    boolean exists(String path);

    /**
     * 删除指定路径的文件或目录
     *
     * @param path 要删除的文件或目录路径
     */
    void delete(String path);

    /**
     * 创建指定路径的目录（包括父目录）
     *
     * @param path 要创建的目录路径
     */
    void mkdirs(String path);

    /**
     * 打开指定路径文件的输入流
     *
     * @param path 要打开的文件路径
     * @return 文件的输入流，用于读取文件内容
     */
    InputStream openStream(String path);
}
