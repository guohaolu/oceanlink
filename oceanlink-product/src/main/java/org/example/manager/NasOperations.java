package org.example.manager;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * NAS 文件操作接口。
 *
 * <p>
 * 定义对网络附加存储（NAS）的<strong>通用文件操作能力</strong>，
 * 用于屏蔽具体存储协议或实现差异（如 NFS、SMB、SFTP 等）。
 *
 * <p>
 * 本接口只描述<strong>“能做什么文件操作”</strong>，
 * 不关心<strong>“如何建立连接”</strong>、
 * <strong>“如何管理会话或资源”</strong>，
 * 也不包含任何业务语义或流程控制。
 *
 * <p>
 * 具体实现应专注于协议适配，
 * 上层调用方仅通过本接口进行文件访问与操作。
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
     * 遍历指定路径下的所有文件
     *
     * @param path         要遍历的目录路径
     * @param fileConsumer 文件处理器，用于处理每个文件
     */
    void walkFiles(String path, Consumer<String> fileConsumer);

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
