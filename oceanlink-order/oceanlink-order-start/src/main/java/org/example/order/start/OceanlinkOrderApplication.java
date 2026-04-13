package org.example.order.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 订单模块启动类。
 *
 * @author guohao.lu
 */
@SpringBootApplication(scanBasePackages = "org.example.order")
public class OceanlinkOrderApplication {

    /**
     * 启动订单模块应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(OceanlinkOrderApplication.class, args);
    }
}
