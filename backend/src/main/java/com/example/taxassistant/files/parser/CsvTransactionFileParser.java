package com.example.taxassistant.files.parser;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CsvTransactionFileParser implements TransactionFileParser {

    private final TransactionRowParser rowParser = new TransactionRowParser();

    @Override
    public boolean supports(String extension) {
        return "csv".equals(extension);
    }

    @Override
    public TransactionParseResult parse(byte[] content) {
        try (CSVReader reader = new CSVReader(new StringReader(decode(content)))) {
            List<String[]> allRows = reader.readAll();
            return parseRows(allRows.stream().map(Arrays::asList).toList());
        } catch (IOException | CsvException exception) {
            return new TransactionParseResult(
                    List.of(),
                    List.of(new ParseFailure(1, "CSV 파일을 읽을 수 없습니다.", ""))
            );
        }
    }

    private String decode(byte[] content) {
        String utf8 = new String(content, StandardCharsets.UTF_8);
        if (!utf8.contains("\uFFFD")) {
            return utf8;
        }
        return new String(content, Charset.forName("MS949"));
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
