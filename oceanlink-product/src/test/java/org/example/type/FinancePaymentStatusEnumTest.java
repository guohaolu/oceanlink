package org.example.type;

import org.example.pojo.dto.StudentDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

class FinancePaymentStatusEnumTest {

    @Test
    void getByValue() {
        String str1 = """
                {"name":"张三百", "age":"16"}
                """;
        String str2 = """
                [
                    {"name":"张三百", "age":"16"},
                    {"name":"张三百2", "age":"18"}
                ]
                """;
        StudentDTO byValue = FinancePaymentStatusEnum.getByValue(FinancePaymentStatusEnum.STUDENT_STATUS, str1);
        List<StudentDTO> byValue2 = FinancePaymentStatusEnum.getByValue(FinancePaymentStatusEnum.STUDENT_STATUS_LIST, str2);
    }
}