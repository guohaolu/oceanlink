package org.example.easyexcel;

import java.lang.annotation.*;

/**
 * Excel下拉框注解
 *
 * @author guohao.lu
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelDropDown {
    /**
     * 数据字典类型
     */
    String dictType() default "";

    /**
     * 是否允许自定义输入（默认不允许）
     */
    boolean allowCustomInput() default false;

    /**
     * 不通过数据字典，自己配置的数据字典值
     */
    String[] dictValues() default {};

    /**
     * 下拉框提示信息
     */
    String prompt() default "请选择下拉框中的值";
}
