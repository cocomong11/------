package com.example.taxassistant.reports;

import com.example.taxassistant.common.LegalNotice;
import com.example.taxassistant.domain.enums.ClassificationStatus;
import com.example.taxassistant.domain.enums.EvidenceStatus;
import com.example.taxassistant.domain.enums.TransactionType;
import com.example.taxassistant.domain.transaction.Transaction;
import com.example.taxassistant.reports.dto.CategoryExpenseResponse;
import com.example.taxassistant.reports.dto.ReportSummaryResponse;
import com.example.taxassistant.reports.dto.SuspiciousItemResponse;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

@Service
public class ReportComputationService {

    public ReportSummaryResponse summarize(int year, Integer month, List<Transaction> transactions) {
        BigDecimal totalRevenue = sumByType(transactions, TransactionType.INCOME);
        BigDecimal totalExpense = sumByType(transactions, TransactionType.EXPENSE);
        List<CategoryExpenseResponse> categoryExpenses = categoryExpenses(transactions);
        long unclassifiedCount = transactions.stream().filter(this::isUnclassified).count();
        long missingEvidenceCount = transactions.stream()
                .filter(transaction -> transaction.getEvidenceStatus() == EvidenceStatus.MISSING)
                .count();

        return new ReportSummaryResponse(
                year,
                month,
                totalRevenue,
                totalExpense,
                totalRevenue.subtract(totalExpense),
                categoryExpenses,
                unclassifiedCount,
                missingEvidenceCount,
                suspiciousIncomeDepositItems(transactions),
                LegalNotice.REFERENCE_ONLY
        );
    }

    public List<SuspiciousItemResponse> suspiciousIncomeDepositItems(List<Transaction> transactions) {
        BigDecimal incomeDepositTotal = transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal salesTaggedTotal = transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.INCOME)
                .filter(this::looksLikeSales)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (incomeDepositTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        BigDecimal difference = incomeDepositTotal.subtract(salesTaggedTotal).abs();
        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        return List.of(new SuspiciousItemResponse(
                "매출 거래와 입금 거래 차이 확인 필요",
                "입금 거래 합계와 매출로 식별된 거래 합계가 다릅니다. 실제 매출, 계좌이체, 개인 입금 등이 섞였는지 확인해주세요.",
                difference
        ));
    }

    public boolean isUnclassified(Transaction transaction) {
        return transaction.getCategoryName() == null
                || transaction.getCategoryName().isBlank()
                || transaction.getClassificationStatus() == ClassificationStatus.UNCLASSIFIED
                || transaction.getClassificationStatus() == ClassificationStatus.NEEDS_REVIEW;
    }

    private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<CategoryExpenseResponse> categoryExpenses(List<Transaction> transactions) {
        Map<String, BigDecimal> totals = new TreeMap<>();
        transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.EXPENSE)
                .forEach(transaction -> totals.merge(
                        categoryName(transaction),
                        transaction.getAmount(),
                        BigDecimal::add
                ));
        return totals.entrySet()
                .stream()
                .map(entry -> new CategoryExpenseResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CategoryExpenseResponse::amount).reversed())
                .toList();
    }

    private String categoryName(Transaction transaction) {
        if (transaction.getCategoryName() == null || transaction.getCategoryName().isBlank()) {
            return "미분류";
        }
        return transaction.getCategoryName();
    }

    private boolean looksLikeSales(Transaction transaction) {
        String text = normalize(transaction.getCategoryName())
                + " "
                + normalize(transaction.getMerchantName())
                + " "
                + normalize(transaction.getDescription());
        return text.contains("매출")
                || text.contains("판매")
                || text.contains("sales");
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .trim();
    }
}
