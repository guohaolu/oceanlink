package org.example;

import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
//import com.baomidou.mybatisplus.generator.FastAutoGenerator;
//import com.baomidou.mybatisplus.generator.config.OutputFile;
//import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Paths;
import java.util.Collections;

/**
 * @author guohao.lu
 */
@SpringBootTest
@ActiveProfiles("local")
public class MybatisGeneratorTest {
    @Autowired
    private DynamicDataSourceProperties dynamicDataSourceProperties;

    @Test
    @DisplayName("clickhouse代码生成")
    public void testClickhouseGenerator() {
        DataSourceProperty clickhouse = dynamicDataSourceProperties.getDatasource().get("clickhouse");

//        FastAutoGenerator.create(clickhouse.getUrl(), clickhouse.getUsername(), clickhouse.getPassword())
//                .globalConfig(builder -> builder
//                        .author("guohao.lu")
//                        .outputDir(Paths.get(System.getProperty("user.dir")) + "/src/main/java")
//                        .commentDate("yyyy-MM-dd")
//                )
//                .packageConfig(builder -> builder
//                        .parent("com.ewayt.erp")
//                        .moduleName("finance")
//                        .entity("pojo.entity")
//                        .mapper("mapper")
//                        .serviceImpl("repository.impl")
//                        .pathInfo(Collections.singletonMap(OutputFile.xml, Paths.get(System.getProperty("user.dir")) + "/src/main/resources/mapper"))
//                )
//                .strategyConfig(builder ->
//                        builder.addInclude("finance_vc_invoice_related")
//                                .addTableSuffix("_t", "_v", "_mv")
//
//                                .entityBuilder()
//                                .enableLombok()                  // 使用 Lombok
//                                .formatFileName("%sEntity")      // 生成类名以 Entity 结尾
//                                .enableTableFieldAnnotation()
//
//                                .controllerBuilder().disable()
//
//                                .serviceBuilder().disableService()                               // 不要 service 接口
//                                .formatServiceImplFileName("%sRepository") // 自定义：类名后缀 Repository
//
//                                .mapperBuilder()
//                                .mapperAnnotation(Mapper.class)
//                                .enableBaseColumnList()
//                                .enableBaseResultMap()
//                )
//                .templateEngine(new FreemarkerTemplateEngine())
//                .execute();
    }
}
