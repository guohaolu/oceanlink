package org.example.inventory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 库存模块启动类
 * 实现基于分布式缓存的强一致性热点库存合并扣减
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("org.example")
public class OceanlinkInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(OceanlinkInventoryApplication.class, args);
    }
}
