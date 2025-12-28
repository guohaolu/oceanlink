package org.example.manager;

import org.example.manager.impl.RemoteNasManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class RemoteNasManagerTest {
    @Autowired
    private RemoteNasManager remoteNasManager;

    @Test
    void list() {
        List<String> list = remoteNasManager.list("/IT");
        System.out.println(list);
    }

    @Test
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
}