package org.example.support;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则执行上下文（Rule Context）。
 *
 * <p>
 * 该类用于在规则匹配与执行过程中，保存和提供运行时数据。
 * 规则中的条件表达式可以通过变量引用的方式，从上下文中读取实际值，
 * 从而实现规则与具体数据的解耦。
 *
 * <p>
 * 上下文内部以 {@link Map} 的形式存储数据，Key 为字段名，
 * Value 为运行时对象（如站点、标题、SKU、品牌等）。
 *
 * <p>
 * 支持在规则配置中使用引用表达式，例如：
 * <pre>
 *     ${age}
 *     ${site}
 * </pre>
 * 在规则执行时，这些引用会被解析为上下文中对应 Key 的实际值。
 *
 * <p>
 * 设计约束：
 * <ul>
 *     <li>该上下文是轻量级、无状态约束的容器，不做类型转换</li>
 *     <li>变量不存在时返回 {@code null}，由上层逻辑决定如何处理</li>
 *     <li>不支持嵌套引用或复杂表达式，仅用于简单变量替换</li>
 * </ul>
 *
 * <p>
 * 通常该对象的生命周期与一次规则匹配过程一致。
 *
 * @author guohao.lu
 */
public class RuleContext {
    /**
     * 规则执行过程中使用的数据容器。
     *
     * <p>
     * Key 为变量名，Value 为对应的运行时值。
     * 例如：
     * <pre>
     *     site  -> "US"
     *     title -> "running shoe"
     *     sku   -> "NK-001"
     * </pre>
     */
    private final Map<String, Object> data = new HashMap<>();

    /**
     * 变量引用的匹配模式。
     *
     * <p>
     * 用于识别形如 <code>${variableName}</code> 的字符串，
     * 并提取其中的变量名部分。
     */
    private static final Pattern REF_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * 向规则上下文中写入一个变量。
     *
     * <p>
     * 该方法通常在规则执行前调用，用于准备规则判断所需的数据。
     *
     * @param key   变量名
     * @param value 变量值
     */
    public void put(String key, Object value) {
        data.put(key, value);
    }

    /**
     * 从规则上下文中获取指定变量的值。
     *
     * @param key 变量名
     * @return 对应的变量值；如果不存在则返回 {@code null}
     */
    public Object getValue(String key) {
        return data.get(key);
    }

    /**
     * 解析规则中的值定义。
     *
     * <p>
     * 该方法用于将规则配置中的“值描述”解析为实际参与比较的值。
     * 支持两种形式：
     * <ul>
     *     <li>引用值（REFERENCE）：例如 <code>${age}</code></li>
     *     <li>字面量：例如 <code>18</code>、<code>US</code></li>
     * </ul>
     *
     * <p>
     * 当 {@code valueType} 为 {@code REFERENCE} 时：
     * <ul>
     *     <li>从 {@code valueContent} 中解析变量名</li>
     *     <li>从上下文中查找对应的实际值并返回</li>
     * </ul>
     *
     * <p>
     * 其他情况下，直接将 {@code valueContent} 作为字面量返回。
     *
     * @param valueType    值类型标识（如 {@code REFERENCE}）
     * @param valueContent 值内容（可能包含变量引用）
     * @return 解析后的实际值，可能为 {@code null}
     */
    public Object resolveValue(String valueType, String valueContent) {
        if ("REFERENCE".equals(valueType)) {
            Matcher matcher = REF_PATTERN.matcher(valueContent);
            if (matcher.find()) {
                String varName = matcher.group(1);
                return data.get(varName);
            }
        }

        // 非引用类型，按字面量处理
        return valueContent;
    }
}