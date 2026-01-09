package org.example.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author guohao.lu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class R<T> {
    /**
     * 接口响应状态码 0 表示成功 其他表示失败
     */
    private Integer code;
    /**
     * 数据
     */
    private T data;
    /**
     * 描述
     */
    private String msg;

    /**
     * 成功返回
     *
     * @param data 数据
     * @return R
     */
    public static <T> R<T> ok(T data) {
        return new R<>(0, data, "success");
    }
}
