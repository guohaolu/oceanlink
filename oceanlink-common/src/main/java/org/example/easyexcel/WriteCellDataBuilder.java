package org.example.easyexcel;

import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.data.DataFormatData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.metadata.style.WriteFont;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 单元格构建器
 * <p>
 * 提供链式调用方式，用于构建带有样式和格式的Excel单元格数据。
 * </p>
 * 示例代码：
 * <pre>{@code
 *     WriteCellData<?> cell = row.get(i);
 *     // 单元格更新样式
 *     WriteCellData<?> newCell = WriteCellDataBuilder.builder(cell).red().bold().build();
* }</pre>
 *
 * @author guohao.lu
 */
public final class WriteCellDataBuilder {
    /**
     * 单元格
     */
    private final WriteCellData<?> writeCellData;

    /**
     * 构造方法，初始化单元格数据对象
     *
     * @param writeCellData 单元格数据对象
     */
    private WriteCellDataBuilder(WriteCellData<?> writeCellData) {
        this.writeCellData = writeCellData;
    }

    /**
     * 构建builder
     * <p>
     * 根据传入的值类型创建对应的WriteCellData对象，并返回一个新的WriteCellDataBuilder实例。
     *
     * @param value 值，支持多种类型（String、BigDecimal、Number、Boolean、LocalDateTime、Date、byte[]等）
     * @return builder 实例
     */
    public static WriteCellDataBuilder builder(Object value) {
        WriteCellData<?> writeCellData;
        if (value instanceof WriteCellData) {
            writeCellData = (WriteCellData<?>) value;
        } else if (value instanceof String) {
            writeCellData = new WriteCellData<>((String) value);
        } else if (value instanceof BigDecimal) {
            writeCellData = new WriteCellData<>((BigDecimal) value);
        } else if (value instanceof Number) {
            writeCellData = new WriteCellData<>(new BigDecimal(value.toString()));
        } else if (value instanceof Boolean) {
            writeCellData = new WriteCellData<>((Boolean) value);
        } else if (value instanceof LocalDateTime) {
            writeCellData = new WriteCellData<>((LocalDateTime) value);
        } else if (value instanceof Date) {
            writeCellData = new WriteCellData<>((Date) value);
        } else if (value instanceof byte[]) {
            writeCellData = new WriteCellData<>((byte[]) value);
        } else {
            writeCellData = new WriteCellData<>(value.toString());
        }
        return new WriteCellDataBuilder(writeCellData);
    }

    /**
     * 生成单元格
     * <p>
     * 返回当前构建完成的WriteCellData对象。
     *
     * @return 单元格数据对象
     */
    public WriteCellData<?> build() {
        return this.writeCellData;
    }

    /**
     * 设置红色字体
     * <p>
     * 修改当前单元格的字体颜色为红色。
     *
     * @return builder 实例，支持链式调用
     */
    public WriteCellDataBuilder red() {
        // 创建字体样式（红色）
        WriteFont targetWriteFont = writeCellData.getOrCreateStyle().getWriteFont() == null ? new WriteFont() : writeCellData.getOrCreateStyle().getWriteFont();
        targetWriteFont.setColor(IndexedColors.RED.getIndex());

        writeCellData.getWriteCellStyle().setWriteFont(targetWriteFont);
        return this;
    }

    /**
     * 设置加粗字体
     * <p>
     * 修改当前单元格的字体为加粗样式。
     *
     * @return builder 实例，支持链式调用
     */
    public WriteCellDataBuilder bold() {
        // 创建字体样式（加粗）
        WriteFont targetWriteFont = writeCellData.getOrCreateStyle().getWriteFont() == null ? new WriteFont() : writeCellData.getOrCreateStyle().getWriteFont();
        targetWriteFont.setBold(true);

        writeCellData.getWriteCellStyle().setWriteFont(targetWriteFont);
        return this;
    }

    /**
     * 设置百分比格式
     * <p>
     * 将当前单元格的值转换为百分比形式，并设置单元格显示格式为百分比。
     *
     * @param rate 比例基数（例如：100 表示以100为基准进行百分比转换）
     * @return builder 实例，支持链式调用
     */
    public WriteCellDataBuilder percent(Integer rate) {
        String stringValue = writeCellData.getStringValue();
        BigDecimal numberValue = writeCellData.getNumberValue();
        if (stringValue != null && stringValue.contains("%")) {
            // 1. 移除百分号并转换为数值
            BigDecimal value = new BigDecimal(stringValue.replace("%", ""))
                    .divide(new BigDecimal("" + rate), 4, RoundingMode.HALF_UP);
            writeCellData.setNumberValue(value);
        } else if (stringValue != null) {
            BigDecimal value = new BigDecimal(stringValue)
                    .divide(new BigDecimal("" + rate), 4, RoundingMode.HALF_UP);
            writeCellData.setNumberValue(value);
        } else if (numberValue != null) {
            BigDecimal value = new BigDecimal(numberValue + "")
                    .divide(new BigDecimal("" + rate), 4, RoundingMode.HALF_UP);
            writeCellData.setNumberValue(value);
        }

        // 设置 0.00% 格式
        DataFormatData dataFormatData = writeCellData.getOrCreateStyle().getDataFormatData() == null ? new DataFormatData() : writeCellData.getOrCreateStyle().getDataFormatData();
        dataFormatData.setIndex((short) 10);

        writeCellData.getWriteCellStyle().setDataFormatData(dataFormatData);
        writeCellData.setType(CellDataTypeEnum.NUMBER);
        return this;
    }
}
