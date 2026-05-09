package com.example.taxassistant.checklist;

import com.example.taxassistant.checklist.dto.ChecklistItemResponse;
import com.example.taxassistant.checklist.dto.ChecklistResponse;
import com.example.taxassistant.common.LegalNotice;
import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.checklist.ChecklistItem;
import com.example.taxassistant.domain.checklist.ChecklistItemRepository;
import com.example.taxassistant.domain.enums.ChecklistItemType;
import com.example.taxassistant.domain.enums.EvidenceStatus;
import com.example.taxassistant.domain.enums.Severity;
import com.example.taxassistant.domain.transaction.Transaction;
import com.example.taxassistant.domain.transaction.TransactionRepository;
import com.example.taxassistant.reports.ReportComputationService;
import com.example.taxassistant.security.ResourceOwnershipService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChecklistService {

    private final TransactionRepository transactionRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ReportComputationService reportComputationService;
    private final ResourceOwnershipService ownershipService;

    public ChecklistService(
            TransactionRepository transactionRepository,
            ChecklistItemRepository checklistItemRepository,
            ReportComputationService reportComputationService,
            ResourceOwnershipService ownershipService
    ) {
        this.transactionRepository = transactionRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.reportComputationService = reportComputationService;
        this.ownershipService = ownershipService;
    }

    @Transactional
    public ChecklistResponse generate(UUID userId, UUID businessId) {
        Business business = ownershipService.requireOwnedBusiness(userId, businessId);
        List<Transaction> transactions = transactionRepository.findAllByBusinessIdAndBusinessOwnerId(
                businessId,
                userId
        );

        checklistItemRepository.deleteAllByBusinessId(businessId);
        List<ChecklistItem> items = buildItems(business, transactions);
        List<ChecklistItem> savedItems = checklistItemRepository.saveAll(items);

        int dangerCount = countBySeverity(savedItems, Severity.DANGER);
        int warningCount = countBySeverity(savedItems, Severity.WARNING);
        int normalCount = countBySeverity(savedItems, Severity.INFO);

        List<ChecklistItemResponse> responses = savedItems.stream()
                .sorted(Comparator.comparing(ChecklistItem::getSeverity, this::compareSeverity)
                        .thenComparing(ChecklistItem::getCreatedAt))
                .map(ChecklistItemResponse::from)
                .toList();

        return new ChecklistResponse(
                businessId,
                responses.size(),
                dangerCount,
                warningCount,
                normalCount,
                responses,
                LegalNotice.REFERENCE_ONLY
        );
    }

    private List<ChecklistItem> buildItems(Business business, List<Transaction> transactions) {
        List<ChecklistItem> items = new ArrayList<>();
        if (transactions.isEmpty()) {
            items.add(new ChecklistItem(
                    business,
                    null,
                    ChecklistItemType.MISSING_DOCUMENT,
                    Severity.WARNING,
                    "거래 자료 업로드 필요",
                    "신고 준비 리포트를 만들 거래 자료가 없습니다. 카드, 계좌, 매출 자료를 업로드해주세요."
            ));
            return items;
        }

        transactions.stream()
                .filter(reportComputationService::isUnclassified)
                .forEach(transaction -> items.add(new ChecklistItem(
                        business,
                        transaction,
                        ChecklistItemType.UNCLASSIFIED_TRANSACTION,
                        Severity.WARNING,
                        "미분류 거래 확인 필요",
                        transactionLabel(transaction) + " 거래의 계정과목을 확인해주세요."
                )));

        transactions.stream()
                .filter(transaction -> transaction.getEvidenceStatus() == EvidenceStatus.MISSING)
                .forEach(transaction -> items.add(new ChecklistItem(
                        business,
                        transaction,
                        ChecklistItemType.MISSING_EVIDENCE,
                        Severity.DANGER,
                        "증빙 누락 거래 확인 필요",
                        transactionLabel(transaction) + " 거래의 영수증, 세금계산서, 카드전표 등 증빙을 확인해주세요."
                )));

        reportComputationService.suspiciousIncomeDepositItems(transactions)
                .forEach(item -> items.add(new ChecklistItem(
                        business,
                        null,
                        ChecklistItemType.BUSINESS_INFO_REVIEW,
                        Severity.WARNING,
                        item.title(),
                        item.description() + " 차이 금액: " + item.differenceAmount().toPlainString() + "원"
                )));

        if (items.isEmpty()) {
            items.add(new ChecklistItem(
                    business,
                    null,
                    ChecklistItemType.BUSINESS_INFO_REVIEW,
                    Severity.INFO,
                    "신고 준비 상태 정상",
                    "현재 업로드된 자료 기준으로 즉시 확인이 필요한 항목이 없습니다."
            ));
        }
        return items;
    }

    private String transactionLabel(Transaction transaction) {
        String merchant = transaction.getMerchantName() == null || transaction.getMerchantName().isBlank()
                ? "거래처 미입력"
                : transaction.getMerchantName();
        return transaction.getTransactionDate() + " " + merchant;
    }

    private int countBySeverity(List<ChecklistItem> items, Severity severity) {
        return (int) items.stream()
                .filter(item -> item.getSeverity() == severity)
                .count();
    }

    private int compareSeverity(Severity left, Severity right) {
        return Integer.compare(severityRank(left), severityRank(right));
    }

    private int severityRank(Severity severity) {
        return switch (severity) {
            case DANGER -> 0;
            case WARNING -> 1;
            case INFO -> 2;
        };
    }
}
