package org.example.easyexcel;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.merge.AbstractMergeStrategy;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义合并策略
 * <p>
 * 该类用于在使用 EasyExcel 导出数据时，根据指定列的数据内容进行相同值的单元格合并。
 * 合并逻辑基于传入的目标列索引和待导出数据列表，通过分析数据中连续相同的值来决定合并范围。
 * </p>
 * 示例代码：
 * <pre>{@code
 * WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "海外仓库存及库龄-按产品线")
 *     .head(head1)  // 设置表头数据
 *     .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())  // 自动调整列宽
 *     .registerWriteHandler(new CustomMergeStrategy(  // 自定义合并策略
 *         pair.getKey().stream()
 *             .map(data -> data.get(0).getStringValue())
 *             .collect(Collectors.toList()),
 *         0))  // 从第0列开始合并
 *     .build();
 * }</pre>
 *
 * @author guohao.lu
 */
public class CustomMergeStrategy extends AbstractMergeStrategy {
    /**
     * 分组，每几行合并一次
     * <p>
     * 该列表记录了每一组连续相同值的行数，用于后续合并操作。
     * </p>
     */
    private final List<Integer> exportFieldGroupCountList;

    /**
     * 目标合并列index
     * <p>
     * 表示需要进行合并操作的列索引。
     * </p>
     */
    private final Integer targetColumnIndex;

    // 需要开始合并单元格的首行index
    private Integer rowIndex;

    /**
     * 构造方法
     * <p>
     * 初始化自定义合并策略实例。
     * </p>
     *
     * @param exportDataList     待合并目标列的值列表，用于分析哪些行需要合并
     * @param targetColumnIndex  目标合并列的索引
     */
    // exportDataList为待合并目标列的值
    public CustomMergeStrategy(List<String> exportDataList, Integer targetColumnIndex) {
        this.exportFieldGroupCountList = getGroupCountList(exportDataList);
        this.targetColumnIndex = targetColumnIndex;
    }

    /**
     * 执行单元格合并操作
     * <p>
     * 该方法由 EasyExcel 在写入每个单元格时调用。仅当当前单元格是目标列且为首行时，
     * 才会触发实际的合并操作。
     * </p>
     *
     * @param sheet           当前工作表对象
     * @param cell            当前单元格对象
     * @param head            单元格头部信息（未使用）
     * @param relativeRowIndex 当前行相对于数据区域的行索引（未使用）
     */
    @Override
    protected void merge(Sheet sheet, Cell cell, Head head, Integer relativeRowIndex) {
        if (null == rowIndex) {
            rowIndex = cell.getRowIndex();
        }
        // 仅从首行以及目标列的单元格开始合并，忽略其他
        if (cell.getRowIndex() == rowIndex && cell.getColumnIndex() == targetColumnIndex) {
            mergeGroupColumn(sheet);
        }
    }

    /**
     * 根据导出数据列表计算每组连续相同值的行数
     * <p>
     * 遍历输入列表，统计连续相同字符串的数量，并将结果保存到列表中。
     * </p>
     *
     * @param exportDataList 待处理的数据列表
     * @return 每组连续相同值的行数组成的列表
     */
    private List<Integer> getGroupCountList(List<String> exportDataList) {
        if (CollectionUtils.isEmpty(exportDataList)) {
            return new ArrayList<>();
        }

        List<Integer> groupCountList = new ArrayList<>();
        int count = 1;

        for (int i = 1; i < exportDataList.size(); i++) {
            if (exportDataList.get(i).equals(exportDataList.get(i - 1))) {
                count++;
            } else {
                groupCountList.add(count);
                count = 1;
            }
        }
        // 处理完最后一条后
        groupCountList.add(count);
        return groupCountList;
    }

    /**
     * 实际执行合并操作的方法
     * <p>
     * 遍历分组计数列表，对每一组大于1的连续行执行单元格合并。
     * </p>
     *
     * @param sheet 当前工作表对象
     */
    private void mergeGroupColumn(Sheet sheet) {
        int rowCount = rowIndex;
        for (Integer count : exportFieldGroupCountList) {
            if (count == 1) {
                rowCount += count;
                continue;
            }
            // 合并单元格
            CellRangeAddress cellRangeAddress = new CellRangeAddress(rowCount, rowCount + count - 1, targetColumnIndex, targetColumnIndex);
            sheet.addMergedRegionUnsafe(cellRangeAddress);
            rowCount += count;
        }
    }
}
