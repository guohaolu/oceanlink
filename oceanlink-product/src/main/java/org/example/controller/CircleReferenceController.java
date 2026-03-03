package org.example.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author guohao.lu
 */
@RestController
@RequestMapping("/testCircleReference")
public class CircleReferenceController {
    @GetMapping("/test")
    public void test() {
        Map<String, Object> f = new HashMap<>();
        f.put("name", "father001");
        Map<String, Object> s = new HashMap<>();
        s.put("name", "son001");

        f.put("son", s);
        s.put("father", f);
        // WORKS
        {
            String str = JSON.toJSONString(
                    s,
                    SerializerFeature.PrettyFormat
            );
            System.out.println(str);
        }
        String str = "/IT/财务/影刀/平台/Amazon-VC/NetPPM/2026/TeraVC-欧洲/2026-02-09/Net_PPM_ASIN_TeraVC-欧洲_ALL_year-to-date_42.42%_2026-02-09.xlsx";
        // ERROR
        {
            // 如果有循环依赖，且使用了 SerializerFeature.DisableCircularReferenceDetect 属性，则会
            // 抛出异常
            try {
                JSON.toJSONString(
                        s,
                        SerializerFeature.DisableCircularReferenceDetect,
                        SerializerFeature.PrettyFormat
                );
                System.out.println("should throw exception");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
