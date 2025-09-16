package org.example.easyexcel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.example.manager.IRemoteDictManager;
import org.example.pojo.dto.SysDictItemDTO;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EasyExcel下拉框写入处理器
 * <p>
 * 该类用于在使用 EasyExcel 导出 Excel 文件时，为指定字段添加下拉框校验功能。
 * 支持通过注解 {@link ExcelDropDown} 配置下拉框的选项来源（静态值或远程字典），
 * 并可设置输入提示和是否允许自定义输入。
 * </p>
 * 示例代码：
 * <pre>{@code
 *    EasyExcel.write(response.getOutputStream(), FinanceInvoiceConfigExcelDTO.class)
 *                     .registerWriteHandler(new DropDownSheetWriteHandler(FinanceInvoiceConfigExcelDTO.class))
 *                     .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
 *                     .sheet("发票类型判断配置")
 *                     .doWrite(new ArrayList<>());
 * }</pre>
 *
 * @author guohao.lu
 */
@RequiredArgsConstructor
public class DropDownSheetWriteHandler implements SheetWriteHandler {
    /**
     * 当前处理的实体类，用于获取字段上的 {@link ExcelDropDown} 注解
     */
    private final Class<?> clazz;

    /**
     * 远程字典管理器，用于根据字典类型获取字典项列表
     */
    private final IRemoteDictManager remoteDictManager;

    /**
     * 在 Sheet 创建完成后执行的操作，用于添加数据验证（如下拉框）
     *
     * @param writeWorkbookHolder 工作簿持有者，包含工作簿相关信息
     * @param writeSheetHolder    Sheet 持有者，包含当前 Sheet 的信息
     */
    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        Sheet sheet = writeSheetHolder.getSheet();
        DataValidationHelper helper = sheet.getDataValidationHelper();

        // 获取所有字段
        // 只处理有ExcelProperty注解的字段
        List<Field> excelFields = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers())) // 排除静态字段
                .filter(field -> field.isAnnotationPresent(ExcelProperty.class)).sorted((f1, f2) -> {
                    ExcelProperty ep1 = f1.getAnnotation(ExcelProperty.class);
                    ExcelProperty ep2 = f2.getAnnotation(ExcelProperty.class);
                    return Integer.compare(ep1.index(), ep2.index());
                }).toList();

        // 按@ExcelProperty的index或order排序

        for (int i = 0; i < excelFields.size(); i++) {
            Field field = excelFields.get(i);
            ExcelDropDown dropDown = field.getAnnotation(ExcelDropDown.class);
            if (dropDown == null) {
                continue;
            }

            // 获取字典项
            List<String> dictItems = new ArrayList<>();

            if (ArrayUtils.isNotEmpty(dropDown.dictValues())) {
                dictItems = List.of(dropDown.dictValues());
            } else if (StringUtils.isNotBlank(dropDown.dictType())) {
                dictItems = remoteDictManager.listByType(dropDown.dictType()).stream()
                        .map(SysDictItemDTO::getItemValue)
                        .toList();
            }

            if (dictItems.isEmpty()) {
                continue;
            }

            // 创建下拉框约束条件
            DataValidationConstraint constraint = helper.createExplicitListConstraint(
                    dictItems.toArray(new String[0])
            );

            // 设置下拉范围（假设第一行是表头，从第二行开始）
            CellRangeAddressList addressList = new CellRangeAddressList(
                    1, 65535, i, i
            );

            DataValidation validation = helper.createValidation(constraint, addressList);

            // 设置输入提示
            if (!dropDown.prompt().isEmpty()) {
                validation.createPromptBox("提示", dropDown.prompt());
            }

            // 如果不允许自定义输入，则设置错误提示框
            if (!dropDown.allowCustomInput()) {
                validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
                validation.createErrorBox("错误", "请从下拉框中选择有效值");
                validation.setShowErrorBox(true);
            }

            sheet.addValidationData(validation);
        }
    }
}
