package org.example.manager.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.manager.NasOperations;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 基础设施装配者（Infrastructure Assembler / Bootstrapper）。
 *
 * <p>
 * 本类的唯一职责是<strong>装配基础设施对象</strong>，而不是参与任何业务或资源使用逻辑。
 * 它只负责把底层组件按既定依赖关系连接起来。
 *
 * <p>
 * 具体职责包括：
 * <ul>
 *   <li>创建并配置连接池（pool）</li>
 *   <li>基于 pool 构建访问模板（template）</li>
 *   <li>基于 template 构建对外暴露的操作对象（operations）</li>
 * </ul>
 *
 * <p>
 * 明确不承担的职责：
 * <ul>
 *   <li>不处理 SFTP 协议或会话的具体细节</li>
 *   <li>不定义异常转换或重试等错误处理策略</li>
 *   <li>不表达任何文件或目录操作的业务语义</li>
 * </ul>
 *
 * <p>
 * 换句话说，本类只关心<strong>“如何组装”</strong>，而不关心<strong>“如何使用”</strong>。
 * 它的存在是为了将基础设施的创建与使用解耦，
 * 是一个标准的 Infrastructure Bootstrapper / Assembler。
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
     *
     * @param path 文件路径
     * @return 文件输入流
     */
    public InputStream getFileStream(String path) {
        return nasOperations.openStream(path);
    }

    /**
     * 列出指定目录下的文件列表
     *
     * @param dir 目录路径
     * @return 文件名列表
     */
    public List<String> listFiles(String dir) {
        return nasOperations.list(dir);
    }

    /**
     * 收集指定路径下的所有文件
     *
     * @param path 要遍历的目录路径
     * @return 包含所有文件路径的字符串列表
     */
    public List<String> collectAllFiles(String path) {
        List<String> files = new ArrayList<>();
        // 遍历指定路径下的所有文件，并将文件路径添加到结果列表中
        nasOperations.walkFiles(path, files::add);
        return files;
    }


    /**
     * 删除指定路径的文件或目录
     *
     * @param path 要删除的文件或目录路径
     */
    public void delete(String path) {
        nasOperations.delete(path);
    }

    /**
     * 检查指定路径的文件或目录是否存在
     *
     * @param path 要检查的路径
     * @return 存在返回true，否则返回false
     */
    public boolean exists(String path) {
        return nasOperations.exists(path);
    }
}

