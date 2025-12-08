package org.example.handler;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * CSV文件预处理
 * <p>
 * 示例代码:
 * <pre>{@code
 * String fileName = "C:\\Users\\Administrator\\Downloads\\货物短缺_MetrovisionVC_IT_2025-09-26_342305820_20250701-03SC.csv";
 * InputStream newStream = CsvPreprocessor.preprocessCsv(Files.newInputStream(Path.of(fileName)));
 * // 还原值
 * String newValue = CsvPreprocessor.restoreSpecialChars(oldValue);
 *  }</pre>
 *
 * @author guohao.lu
 */
public class CsvPreprocessor {
    /**
     * 对输入的CSV文件流进行预处理，主要目的是处理字段中包含逗号或引号的情况。
     * 将中间部分中的双引号替换为 ##SEP##，将逗号替换为 ##COMMA##，
     * 同时保留原有的外层引号结构以便后续解析。
     *
     * @param originalStream 原始CSV文件输入流
     * @return 预处理后的CSV文件输入流
     * @throws RuntimeException 当IO操作失败时抛出运行时异常
     */
    public static InputStream preprocessCsv(InputStream originalStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(originalStream, StandardCharsets.UTF_8));
             StringWriter writer = new StringWriter()) {

            String line;
            while ((line = reader.readLine()) != null) {
                String processedLine = preprocessCsvLine(line);
                writer.write(processedLine);
                writer.write(System.lineSeparator());
            }

            return new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8));

        } catch (IOException e) {
            throw new RuntimeException("CsvPreprocessor#preprocessCsv解析流失败", e);
        }
    }

    /**
     * 处理单行CSV数据。识别并处理被引号包裹的内容（即中间部分），
     * 替换其中可能引起歧义的逗号和引号字符。
     *
     * @param line 原始CSV行字符串
     * @return 经过预处理的行字符串
     */
    private static String preprocessCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return line;
        }

        // 查找第一个和最后一个特定模式的逗号位置，用于定位需要处理的部分
        int firstCommaIndex = line.indexOf("\",\"");
        int lastCommaIndex = line.lastIndexOf("\",\"");

        // 如果没有足够的分隔符，则跳过该行处理
        if (firstCommaIndex == -1 || lastCommaIndex == -1 || firstCommaIndex >= lastCommaIndex) {
            return line;
        }

        // 拆分行内容：前缀、待处理中间段、后缀
        // 包含第一个逗号
        String firstPart = line.substring(0, firstCommaIndex + 3);
        String middlePart = line.substring(firstCommaIndex + 3, lastCommaIndex);
        // 包含最后一个逗号
        String lastPart = line.substring(lastCommaIndex);

        // 处理中间部分内容
        String processedMiddle = processMiddlePart(middlePart);

        return firstPart + processedMiddle + lastPart;
    }

    /**
     * 处理CSV行中的中间部分字段内容，将其中的引号与逗号做转义处理。
     * 特别地，会临时替换原始的边界标记以避免冲突。
     *
     * @param middlePart 待处理的中间字段内容
     * @return 转义处理后的字段内容
     */
    private static String processMiddlePart(String middlePart) {
        if (StringUtils.isBlank(middlePart)) {
            return middlePart;
        }

        return middlePart
                // 先替换原始边界标记防止干扰
                .replace("\",\"", "##SPECIAL##")
                // 替换内部引号
                .replace("\"", "##SEP##")
                // 替换内部逗号
                .replace(",", "##COMMA##")
                // 还原原始边界标记
                .replace("##SPECIAL##", "\",\"");
    }

    /**
     * 在完成其他处理之后，还原之前替换掉的特殊标记。
     * 将 ##SEP## 和 ##COMMA## 分别还原成对应的双引号和逗号。
     *
     * @param field 已经经过预处理的数据字段
     * @return 恢复原始格式的字段内容
     */
    public static String restoreSpecialChars(String field) {
        if (field == null) {
            return null;
        }
        return field.replace("##SEP##", "\"")
                .replace("##COMMA##", ",");
    }
}
