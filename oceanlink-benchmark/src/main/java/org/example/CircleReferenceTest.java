package org.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.util.HashMap;
import java.util.Map;

/**
 * @author guohao.lu
 */
public class CircleReferenceTest {
    public static void main(String[] args) {
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
