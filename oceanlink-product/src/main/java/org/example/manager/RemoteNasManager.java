package org.example.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public InputStream getFileStream(String path) {
        return nasOperations.openStream(path);
    }

    public List<String> list(String dir) {
        return nasOperations.list(dir);
    }

    public void delete(String path) {
        nasOperations.delete(path);
    }

    public boolean exists(String path) {
        return nasOperations.exists(path);
    }


}
