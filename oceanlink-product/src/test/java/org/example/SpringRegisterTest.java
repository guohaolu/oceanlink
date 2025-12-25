package org.example;

import org.example.configuration.NasAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.Map;

/**
 * @author guohao.lu
 */
@SpringBootTest
public class SpringRegisterTest {
    @Autowired
    private ApplicationContext ctx;

    @Test
    public void test() {
        Map<String, NasAutoConfiguration> beans = ctx.getBeansOfType(NasAutoConfiguration.class);
        beans.forEach((name, bean) -> System.out.println(name + " -> " + bean));
    }
}
