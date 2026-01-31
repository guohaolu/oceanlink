package org.example.easyexcel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 头部提示注解
 *
 * @author guohao.lu
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelHeadTip {

    /** 提示内容 */
    String value();

    /** 批注宽度（像素，Excel 会近似） */
    int width() default 200;

    /** 批注高度 */
    int height() default 80;
}
