package com.example.taxassistant.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

class SampleFileValidationTest {

    @Test
    void sampleCsvContainsExpectedColumns() throws Exception {
        Path path = samplePath("sample-transactions.csv");

        String content = Files.readString(path, StandardCharsets.UTF_8);

        assertThat(content).startsWith("거래일자,거래처,내용,입금액,출금액,부가세");
        assertThat(content).contains("스마트스토어");
        assertThat(content).contains("네이버광고");
    }

    @Test
    void sampleXlsxCanBeOpenedByApachePoi() throws Exception {
        Path path = samplePath("sample-transactions.xlsx");

        try (Workbook workbook = WorkbookFactory.create(path.toFile())) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("transactions");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("date");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("스마트스토어");
            assertThat(sheet.getRow(4).getCell(4).getNumericCellValue()).isEqualTo(55000.0);
        }
    }

    private Path samplePath(String filename) {
        return Path.of("..", "docs", "sample-files", filename);
    }
}
