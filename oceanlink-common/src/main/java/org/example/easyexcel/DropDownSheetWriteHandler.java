package org.example.easyexcel;

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
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * EasyExcel下拉框写入处理器
 *
 * @author guohao.lu
 */
@RequiredArgsConstructor
public class DropDownSheetWriteHandler implements SheetWriteHandler, ApplicationContextAware {
    private final Class<?> clazz;

    private IRemoteDictManager remoteDictManager;

    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        Sheet sheet = writeSheetHolder.getSheet();
        DataValidationHelper helper = sheet.getDataValidationHelper();

        // 获取所有字段
        Field[] fields = clazz.getDeclaredFields();

        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
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

            // 创建下拉框
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

            // 如果不允许自定义输入
            if (!dropDown.allowCustomInput()) {
                validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
                validation.createErrorBox("错误", "请从下拉框中选择有效值");
                validation.setShowErrorBox(true);
            }

            sheet.addValidationData(validation);
        }
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.remoteDictManager = applicationContext.getBean(IRemoteDictManager.class);
    }
}