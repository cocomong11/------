package com.example.taxassistant.ledger;

import java.time.LocalDate;

record LedgerPeriod(
        int year,
        Integer month,
        LocalDate startDate,
        LocalDate endDate
) {

    static LedgerPeriod of(int year, Integer month) {
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("year must be between 2000 and 2100");
        }
        if (month == null) {
            return new LedgerPeriod(
                    year,
                    null,
                    LocalDate.of(year, 1, 1),
                    LocalDate.of(year, 12, 31)
            );
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
        LocalDate startDate = LocalDate.of(year, month, 1);
        return new LedgerPeriod(year, month, startDate, startDate.withDayOfMonth(startDate.lengthOfMonth()));
    }
}

