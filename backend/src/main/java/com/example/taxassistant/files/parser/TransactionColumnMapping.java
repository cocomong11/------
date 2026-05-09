package com.example.taxassistant.files.parser;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class TransactionColumnMapping {

    private static final Map<Field, List<String>> DEFAULT_ALIASES = Map.of(
            Field.TRANSACTION_DATE, List.of("거래일자", "거래일", "일자", "날짜", "date", "transactiondate", "transactedat"),
            Field.MERCHANT, List.of("거래처", "가맹점", "상호", "사용처", "merchant", "vendor", "payee"),
            Field.DESCRIPTION, List.of("내용", "적요", "메모", "품목", "description", "memo", "summary"),
            Field.DEPOSIT_AMOUNT, List.of("입금액", "입금", "수입", "매출", "deposit", "income", "credit"),
            Field.WITHDRAWAL_AMOUNT, List.of("출금액", "출금", "지출", "비용", "withdrawal", "expense", "debit"),
            Field.VAT_AMOUNT, List.of("부가세", "부가가치세", "vat", "tax")
    );

    private final int transactionDateIndex;
    private final int merchantIndex;
    private final int descriptionIndex;
    private final int depositAmountIndex;
    private final int withdrawalAmountIndex;
    private final int vatAmountIndex;

    private TransactionColumnMapping(
            int transactionDateIndex,
            int merchantIndex,
            int descriptionIndex,
            int depositAmountIndex,
            int withdrawalAmountIndex,
            int vatAmountIndex
    ) {
        this.transactionDateIndex = transactionDateIndex;
        this.merchantIndex = merchantIndex;
        this.descriptionIndex = descriptionIndex;
        this.depositAmountIndex = depositAmountIndex;
        this.withdrawalAmountIndex = withdrawalAmountIndex;
        this.vatAmountIndex = vatAmountIndex;
    }

    static TransactionColumnMapping fromHeaders(List<String> headers) {
        return new TransactionColumnMapping(
                findIndex(headers, Field.TRANSACTION_DATE),
                findIndex(headers, Field.MERCHANT),
                findIndex(headers, Field.DESCRIPTION),
                findIndex(headers, Field.DEPOSIT_AMOUNT),
                findIndex(headers, Field.WITHDRAWAL_AMOUNT),
                findIndex(headers, Field.VAT_AMOUNT)
        );
    }

    boolean hasRequiredColumns() {
        return transactionDateIndex >= 0 && (depositAmountIndex >= 0 || withdrawalAmountIndex >= 0);
    }

    String getTransactionDate(List<String> row) {
        return valueAt(row, transactionDateIndex);
    }

    String getMerchant(List<String> row) {
        return valueAt(row, merchantIndex);
    }

    String getDescription(List<String> row) {
        return valueAt(row, descriptionIndex);
    }

    String getDepositAmount(List<String> row) {
        return valueAt(row, depositAmountIndex);
    }

    String getWithdrawalAmount(List<String> row) {
        return valueAt(row, withdrawalAmountIndex);
    }

    String getVatAmount(List<String> row) {
        return valueAt(row, vatAmountIndex);
    }

    private static int findIndex(List<String> headers, Field field) {
        List<String> aliases = DEFAULT_ALIASES.get(field).stream()
                .map(TransactionColumnMapping::normalize)
                .toList();
        for (int index = 0; index < headers.size(); index++) {
            String normalized = normalize(headers.get(index));
            if (aliases.contains(normalized)) {
                return index;
            }
        }
        return -1;
    }

    private static String valueAt(List<String> row, int index) {
        if (index < 0 || index >= row.size()) {
            return "";
        }
        return row.get(index);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String withoutBom = value.replace("\uFEFF", "");
        return Normalizer.normalize(withoutBom, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_\\-()/\\.]", "")
                .trim();
    }

    private enum Field {
        TRANSACTION_DATE,
        MERCHANT,
        DESCRIPTION,
        DEPOSIT_AMOUNT,
        WITHDRAWAL_AMOUNT,
        VAT_AMOUNT
    }
}

