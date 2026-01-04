package org.example.manager;

import org.example.manager.impl.RemoteNasManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("dev")
class RemoteNasManagerTest {
    @Autowired
    private RemoteNasManager remoteNasManager;

    @Test
    @DisplayName("列出目录")
    void listFiles() {
        List<String> list = remoteNasManager.listFiles("/IT");
        System.out.println(list);
    }

    @Test
    @DisplayName("收集目录下所有文件")
    void collectAllFiles() {
        String path = "/IT/财务/影刀/平台/Amazon-VC/NetPPM";
        List<String> fileNames = remoteNasManager.collectAllFiles(path);
        System.out.println(fileNames);
    }

    @Test
    void deleteYearToDate() {
        String path = "/IT/财务/影刀/平台/Amazon-VC/NetPPM";
        List<String> fileNames = remoteNasManager.collectAllFiles(path);

        List<String> yearToDateList = fileNames.stream().filter(fileName -> fileName.contains("_year-to-date_")).toList();
    }

    @Test
    @DisplayName("删除文件")
    void delete() {
        List<String> paths = List.of(
                "/IT/财务/影刀/平台/Amazon-VC/NetPPM/2025/VelwayVC-欧洲/2025-12-23/Net_PPM_ASIN_VelwayVC-欧洲_ALL_2024-01_\u200C_2025-12-23.xlsx",
                "/IT/财务/影刀/平台/Amazon-VC/NetPPM/2025/VelwayVC-欧洲/2025-12-23/Net_PPM_ASIN_VelwayVC-欧洲_FR_2024-01_\u200C_2025-12-23.xlsx",
                "/IT/财务/影刀/平台/Amazon-VC/NetPPM/2025/VelwayVC-欧洲/2025-12-23/Net_PPM_ASIN_VelwayVC-欧洲_PL_2024-01_\u200C_2025-12-23.xlsx",
                "/IT/财务/影刀/平台/Amazon-VC/NetPPM/2025/VelwayVC-欧洲/2025-12-23/Net_PPM_ASIN_VelwayVC-欧洲_GB_2024-01_\u200C_2025-12-23.xlsx",
                "/IT/财务/影刀/平台/Amazon-VC/NetPPM/2025/VelwayVC-欧洲/2025-12-23/Net_PPM_ASIN_VelwayVC-欧洲_DE_2024-01_\u200C_2025-12-23.xlsx",
                "/IT/财务/影刀/平台/Amazon-VC/NetPPM/2025/VelwayVC-欧洲/2025-12-23/Net_PPM_ASIN_VelwayVC-欧洲_ES_2024-01_\u200C_2025-12-23.xlsx",
                "/IT/财务/影刀/平台/Amazon-VC/NetPPM/2025/VelwayVC-欧洲/2025-12-23/Net_PPM_ASIN_VelwayVC-欧洲_SE_2024-01_\u200C_2025-12-23.xlsx",
                "/IT/财务/影刀/平台/Amazon-VC/NetPPM/2025/VelwayVC-欧洲/2025-12-23/Net_PPM_ASIN_VelwayVC-欧洲_IT_2024-01_\u200C_2025-12-23.xlsx",
                "/IT/财务/影刀/平台/Amazon-VC/NetPPM/2025/VelwayVC-欧洲/2025-12-23/Net_PPM_ASIN_VelwayVC-欧洲_NL_2024-01_\u200C_2025-12-23.xlsx"
        );

        for (String path : paths) {
            remoteNasManager.delete(path);
        }
    }

    @Test
    @DisplayName("检查文件是否存在")
    void exists() {
        String path = "/IT/财务/影刀/平台/Amazon-VC/NetPPM/2025/VelwayVC-欧洲/2025-12-23/Net_PPM_ASIN_VelwayVC-欧洲_NL_2024-01_\u200C_2025-12-23.xlsx";
        boolean exists = remoteNasManager.exists(path);
        Assertions.assertFalse(exists);

        String path2 = "/IT/财务/影刀/平台/Amazon-VC/NetPPM/2025/CosylandVC-欧洲/2025-12-25/Net_PPM_ASIN_CosylandVC-欧洲_FR_2024-02_32.60%_2025-12-25.xlsx";
        boolean exists2 = remoteNasManager.exists(path2);
        Assertions.assertTrue(exists2);
    }
}