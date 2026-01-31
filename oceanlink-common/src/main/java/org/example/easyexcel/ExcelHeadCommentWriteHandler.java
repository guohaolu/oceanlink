package org.example.easyexcel;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Comment;

import java.util.List;

/**
 * 表头注释处理器
 * <p>
 * 示例代码：
 * <pre>{@code
 *     @ExcelProperty("NETppm经营目标")
 *     @ExcelHeadTip("填写 0~1 之间的小数\n例如：1 = 100%，0.15 = 15%")
 *     private BigDecimal netppm;
 * }</pre>
 * @author guohao.lu
 */
public class ExcelHeadCommentWriteHandler implements CellWriteHandler {

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder,
                                 WriteTableHolder writeTableHolder,
                                 List<WriteCellData<?>> cellDataList,
                                 Cell cell,
                                 Head head,
                                 Integer relativeRowIndex,
                                 Boolean isHead) {

        // 只处理表头
        if (!Boolean.TRUE.equals(isHead) || head == null) {
            return;
        }

        // 取字段上的自定义注解
        ExcelHeadTip tip = head.getField().getAnnotation(ExcelHeadTip.class);
        if (tip == null) {
            return;
        }

        Sheet sheet = writeSheetHolder.getSheet();
        Workbook workbook = sheet.getWorkbook();

        Drawing<?> drawing = sheet.createDrawingPatriarch();
        CreationHelper helper = workbook.getCreationHelper();

        ClientAnchor anchor = helper.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(cell.getColumnIndex() + 3);
        anchor.setRow1(cell.getRowIndex());
        anchor.setRow2(cell.getRowIndex() + 4);

        Comment comment = drawing.createCellComment(anchor);
        comment.setString(helper.createRichTextString(tip.value()));
        comment.setAuthor("ewayt");

        cell.setCellComment(comment);
    }
}
