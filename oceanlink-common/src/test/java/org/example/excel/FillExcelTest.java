package org.example.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * @author guohao.lu
 */
public class FillExcelTest {
    @Test
    public void testFillRemittance() throws IOException {
        ClassPathResource templateResource = new ClassPathResource("template/Remittance_template.xlsx");

        String testResourcesPath = Paths.get(
                "src", "test", "resources", "output.xlsx"
        ).toAbsolutePath().toString();

        // 1. 准备数据
        List<DataA> tableAData = Arrays.asList(
                new DataA("A1", "A2", "A3"),
                new DataA("B1", "B2", "B3")
        );

        List<DataB> tableBData = Arrays.asList(
                new DataB(100, 200),
                new DataB(300, 400),
                new DataB(500, 600)
        );

        try (InputStream templateInputStream = templateResource.getInputStream();
             ExcelWriter excelWriter = EasyExcel.write(testResourcesPath)
                     .withTemplate(templateInputStream).build()) {

            WriteSheet writeSheet = EasyExcel.writerSheet().build();

            excelWriter.fill(tableAData, FillConfig.builder().forceNewRow(true).build(), writeSheet);

            // 6. 填充表B数据（需要跳过表A数据占用的行数）
            // 假设表A数据后有2行间隔（表B标题和一行空白）
            excelWriter.fill(tableBData, FillConfig.builder().forceNewRow(true).build(), writeSheet);
        }
    }

    // 表A数据模型
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataA {
        private String v1;
        private String v2;
        private String v3;
    }

    // 表B数据模型
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataB {
        private int b1;
        private int b2;
    }
}
