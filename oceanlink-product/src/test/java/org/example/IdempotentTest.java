package org.example;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

/**
 * 幂等性测试
 *
 * @author guohao.lu
 */
public class IdempotentTest {
    @Test
    @DisplayName("幂等性测试:基于日期")
    public void test() {
        List<Long> localDateTimes = Stream.generate(System::nanoTime).limit(500).toList();
        int size = new HashSet<>(localDateTimes).size();
        System.out.println("size:" + size);
    }
}
