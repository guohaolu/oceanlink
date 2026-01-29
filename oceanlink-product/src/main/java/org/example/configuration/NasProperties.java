package org.example.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * NAS 配置属性类。
 *
 * <p>
 * 该类用于绑定配置文件中的 NAS 相关属性，并提供 getter / setter 方法进行访问。
 *
 * <p>
 * 配置属性前缀为 {@code ewayt.nas}。
 * @author guohao.lu
 */
@Data
@ConfigurationProperties(prefix = "ewayt.nas")
public class NasProperties {
    private String host;
    private int port = 22;

    private String username;
    private String password;

    private String remoteDir;
    private String localDir;

    private Pool pool = new Pool();

    @Data
    public static class Pool {
        private int maxTotal = 8;
        private int maxIdle = 8;
        private int minIdle = 2;
        private Duration maxWait = Duration.ofSeconds(10);
    }
}
