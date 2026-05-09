package com.example.taxassistant.files.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class XlsxTransactionFileParser implements TransactionFileParser {

    private final TransactionRowParser rowParser = new TransactionRowParser();
    private final DataFormatter dataFormatter = new DataFormatter();

    @Override
    public boolean supports(String extension) {
        return "xlsx".equals(extension);
    }

    @Override
    public TransactionParseResult parse(byte[] content) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            List<List<String>> rows = toRows(sheet);
            return parseRows(rows);
        } catch (IOException | RuntimeException exception) {
            return new TransactionParseResult(
                    List.of(),
                    List.of(new ParseFailure(1, "XLSX 파일을 읽을 수 없습니다.", ""))
            );
        }
    }

    private List<List<String>> toRows(Sheet sheet) {
        List<List<String>> rows = new ArrayList<>();
        int lastRowNumber = sheet.getLastRowNum();
        for (int rowIndex = 0; rowIndex <= lastRowNumber; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                rows.add(List.of());
                continue;
            }
            short lastCellNumber = row.getLastCellNum();
            List<String> values = new ArrayList<>();
            for (int cellIndex = 0; cellIndex < lastCellNumber; cellIndex++) {
                values.add(dataFormatter.formatCellValue(row.getCell(cellIndex)));
            }
            rows.add(values);
        }
        return rows;
    }

    private TransactionParseResult parseRows(List<List<String>> rows) {
        if (rows.isEmpty()) {
            return new TransactionParseResult(
                    List.of(),
                    List.of(new ParseFailure(1, "헤더 행을 찾을 수 없습니다.", ""))
            );
        }
        TransactionColumnMapping mapping = TransactionColumnMapping.fromHeaders(rows.get(0));
        if (!mapping.hasRequiredColumns()) {
            return new TransactionParseResult(
                    List.of(),
                    List.of(new ParseFailure(1, "거래일자와 입금액/출금액 컬럼을 찾을 수 없습니다.", String.join(" | ", rows.get(0))))
            );
        }

        List<ParsedTransactionRow> parsedRows = new ArrayList<>();
        List<ParseFailure> failures = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            if (isBlankRow(row)) {
                continue;
            }
            int rowNumber = index + 1;
            try {
                parsedRows.add(rowParser.parse(rowNumber, row, mapping));
            } catch (RuntimeException exception) {
                failures.add(new ParseFailure(rowNumber, exception.getMessage(), String.join(" | ", row)));
            }
        }
        return new TransactionParseResult(parsedRows, failures);
    }

    private boolean isBlankRow(List<String> row) {
        return row.stream().allMatch(value -> value == null || value.isBlank());
    }
}

