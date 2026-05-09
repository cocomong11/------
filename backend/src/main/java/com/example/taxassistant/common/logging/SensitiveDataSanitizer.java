package com.example.taxassistant.common.logging;

import java.util.List;
import java.util.regex.Pattern;

public final class SensitiveDataSanitizer {

    private static final List<Replacement> REPLACEMENTS = List.of(
            new Replacement(Pattern.compile("(?i)(Bearer\\s+)[A-Za-z0-9._~+/=-]+"), "$1[TOKEN]"),
            new Replacement(Pattern.compile("\\beyJ[A-Za-z0-9_-]*\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b"), "[TOKEN]"),
            new Replacement(Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"), "[EMAIL]"),
            new Replacement(Pattern.compile("\\b\\d{3}-\\d{2}-\\d{5}\\b"), "[BUSINESS_REGISTRATION_NUMBER]"),
            new Replacement(
                    Pattern.compile("(?iu)(\"(?:businessRegistrationNumber|registrationNumber)\"\\s*:\\s*\")([^\"]*)(\")"),
                    "$1[BUSINESS_REGISTRATION_NUMBER]$3"
            ),
            new Replacement(
                    Pattern.compile("(?iu)((?:businessRegistrationNumber|registrationNumber|사업자등록번호)\\s*[:=]\\s*)([^,\\s}\\]]+)"),
                    "$1[BUSINESS_REGISTRATION_NUMBER]"
            ),
            new Replacement(
                    Pattern.compile("(?iu)(\"(?:merchantName|counterpartyName|customerName|vendorName)\"\\s*:\\s*\")([^\"]*)(\")"),
                    "$1[COUNTERPARTY]$3"
            ),
            new Replacement(
                    Pattern.compile("(?iu)((?:merchantName|counterpartyName|customerName|vendorName|거래처명|거래처|상호)\\s*[:=]\\s*)([^,\\s}\\]]+)"),
                    "$1[COUNTERPARTY]"
            ),
            new Replacement(
                    Pattern.compile("(?iu)(\"(?:amount|totalRevenue|totalExpense|netIncome|revenueAmount|expenseAmount|previousYearRevenue|vatAmount|differenceAmount)\"\\s*:\\s*)(\"?)-?\\d[\\d,]*(?:\\.\\d+)?(\"?)"),
                    "$1$2[AMOUNT]$3"
            ),
            new Replacement(
                    Pattern.compile("(?iu)((?:amount|totalRevenue|totalExpense|netIncome|revenueAmount|expenseAmount|previousYearRevenue|vatAmount|differenceAmount|금액|입금액|출금액|부가세|차이 금액)\\s*[:=]\\s*)-?\\d[\\d,]*(?:\\.\\d+)?"),
                    "$1[AMOUNT]"
            )
    );

    private SensitiveDataSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String sanitized = value;
        for (Replacement replacement : REPLACEMENTS) {
            sanitized = replacement.pattern().matcher(sanitized).replaceAll(replacement.replacement());
        }
        return sanitized;
    }

    private record Replacement(Pattern pattern, String replacement) {
    }
}
