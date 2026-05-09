package com.example.taxassistant.files.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

class TransactionRowParser {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy.M.d"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    );

    ParsedTransactionRow parse(int rowNumber, List<String> row, TransactionColumnMapping mapping) {
        LocalDate transactionDate = parseDate(mapping.getTransactionDate(row));
        BigDecimal depositAmount = parseAmount(mapping.getDepositAmount(row));
        BigDecimal withdrawalAmount = parseAmount(mapping.getWithdrawalAmount(row));
        BigDecimal vatAmount = parseAmount(mapping.getVatAmount(row));

        boolean hasDeposit = depositAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean hasWithdrawal = withdrawalAmount.compareTo(BigDecimal.ZERO) > 0;
        if (hasDeposit == hasWithdrawal) {
            throw new IllegalArgumentException("입금액 또는 출금액 중 하나만 0보다 커야 합니다.");
        }

        return new ParsedTransactionRow(
                rowNumber,
                transactionDate,
                blankToNull(mapping.getMerchant(row)),
                blankToNull(mapping.getDescription(row)),
                hasDeposit ? depositAmount : withdrawalAmount,
                vatAmount,
                hasDeposit,
                rawData(row)
        );
    }

    private LocalDate parseDate(String value) {
        String normalized = requireText(value, "거래일자가 비어 있습니다.")
                .replace("년", "-")
                .replace("월", "-")
                .replace("일", "")
                .replace(" ", "")
                .trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("거래일자 형식을 확인해주세요.");
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        String normalized = value
                .replace(",", "")
                .replace("원", "")
                .replace("₩", "")
                .trim();
        boolean negativeByParentheses = normalized.startsWith("(") && normalized.endsWith(")");
        normalized = normalized.replace("(", "").replace(")", "");
        if (normalized.isBlank() || "-".equals(normalized)) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = new BigDecimal(normalized).abs();
        return negativeByParentheses ? amount.abs() : amount;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String rawData(List<String> row) {
        return String.join(" | ", row);
    }
}
