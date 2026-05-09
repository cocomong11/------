package com.example.taxassistant.ledger;

import com.example.taxassistant.common.LegalNotice;
import com.example.taxassistant.domain.ledger.LedgerEntry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class LedgerExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public byte[] export(List<LedgerEntry> entries, int year, Integer month) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("간편장부");
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle moneyStyle = moneyStyle(workbook);
            CellStyle totalStyle = totalStyle(workbook);

            int rowIndex = 0;
            Row titleRow = sheet.createRow(rowIndex++);
            titleRow.createCell(0).setCellValue(title(year, month));

            Row noticeRow = sheet.createRow(rowIndex++);
            noticeRow.createCell(0).setCellValue(LegalNotice.REFERENCE_ONLY);

            rowIndex++;

            Row headerRow = sheet.createRow(rowIndex++);
            String[] headers = {"일자", "거래처/내용", "계정과목", "수입", "비용", "증빙상태", "차액"};
            for (int index = 0; index < headers.length; index++) {
                Cell cell = headerRow.createCell(index);
                cell.setCellValue(headers[index]);
                cell.setCellStyle(headerStyle);
            }

            BigDecimal totalRevenue = BigDecimal.ZERO;
            BigDecimal totalExpense = BigDecimal.ZERO;
            for (LedgerEntry entry : entries) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(entry.getEntryDate().format(DATE_FORMATTER));
                row.createCell(1).setCellValue(entry.getSummary());
                row.createCell(2).setCellValue(entry.getAccountTitle());
                moneyCell(row, 3, entry.getRevenueAmount(), moneyStyle);
                moneyCell(row, 4, entry.getExpenseAmount(), moneyStyle);
                row.createCell(5).setCellValue(entry.getEvidenceStatus().name());
                moneyCell(row, 6, entry.getRevenueAmount().subtract(entry.getExpenseAmount()), moneyStyle);
                totalRevenue = totalRevenue.add(entry.getRevenueAmount());
                totalExpense = totalExpense.add(entry.getExpenseAmount());
            }

            Row totalRow = sheet.createRow(rowIndex);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("합계");
            totalLabel.setCellStyle(totalStyle);
            totalRow.createCell(1).setCellValue("");
            totalRow.createCell(2).setCellValue("");
            moneyCell(totalRow, 3, totalRevenue, totalStyle);
            moneyCell(totalRow, 4, totalExpense, totalStyle);
            totalRow.createCell(5).setCellValue("");
            moneyCell(totalRow, 6, totalRevenue.subtract(totalExpense), totalStyle);

            for (int index = 0; index < headers.length; index++) {
                sheet.autoSizeColumn(index);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create ledger excel", exception);
        }
    }

    private String title(int year, Integer month) {
        if (month == null) {
            return year + "년 간편장부";
        }
        return year + "년 " + month + "월 간편장부";
    }

    private void moneyCell(Row row, int index, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = bordered(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle moneyStyle(Workbook workbook) {
        CellStyle style = bordered(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        return style;
    }

    private CellStyle totalStyle(Workbook workbook) {
        CellStyle style = moneyStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle bordered(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
