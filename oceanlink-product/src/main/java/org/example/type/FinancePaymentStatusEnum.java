package org.example.type;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.pojo.dto.StudentDTO;

import java.util.List;

/**
 * 财务付款状态枚举
 *
 * @author guohao.lu
 */
@Getter
@AllArgsConstructor
public enum FinancePaymentStatusEnum {
    INVOICE_TYPE("发票分类", String.class),
    APPROVE_STATUS("审批状态分类", String.class),
    DETAIL_STATUS("明细状态分类", String.class),
    DETAIL_DRAFT_STATUS("明细草稿状态分类", String.class),
    STUDENT_STATUS("学生状态分类", StudentDTO.class),
    STUDENT_STATUS_LIST("学生状态分类列表", List.class),
    ;

    @EnumValue
    private final String type;
    private final Class<?> clazz;

    /**
     * TODO :待修改，存在问题
     */
    @SuppressWarnings("unchecked")
    public static <T> T getByValue(FinancePaymentStatusEnum statusEnum, String stringValue) {
        for (FinancePaymentStatusEnum value : values()) {
            if (value.getType().equals(statusEnum.getType())) {
                Class<?> clazz = value.getClazz();
                if (clazz == List.class) {
                    return (T) JSONUtil.toList(stringValue, clazz);
                }
                return (T) JSONUtil.toBean(stringValue, clazz);
            }
        }
        throw new IllegalArgumentException("No matching constant for [" + stringValue + "]");
    }
}

