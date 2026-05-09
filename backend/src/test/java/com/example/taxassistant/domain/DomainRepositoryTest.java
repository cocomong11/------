package com.example.taxassistant.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.taxassistant.common.LegalNotice;
import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.business.BusinessRepository;
import com.example.taxassistant.domain.category.CategoryRule;
import com.example.taxassistant.domain.category.CategoryRuleRepository;
import com.example.taxassistant.domain.checklist.ChecklistItem;
import com.example.taxassistant.domain.checklist.ChecklistItemRepository;
import com.example.taxassistant.domain.enums.BookkeepingType;
import com.example.taxassistant.domain.enums.BusinessIndustryGroup;
import com.example.taxassistant.domain.enums.ChecklistItemStatus;
import com.example.taxassistant.domain.enums.ChecklistItemType;
import com.example.taxassistant.domain.enums.ClassificationStatus;
import com.example.taxassistant.domain.enums.FileProcessingStatus;
import com.example.taxassistant.domain.enums.MatchType;
import com.example.taxassistant.domain.enums.ReportPeriodType;
import com.example.taxassistant.domain.enums.Severity;
import com.example.taxassistant.domain.enums.TransactionType;
import com.example.taxassistant.domain.file.UploadedFile;
import com.example.taxassistant.domain.file.UploadedFileRepository;
import com.example.taxassistant.domain.ledger.LedgerEntry;
import com.example.taxassistant.domain.ledger.LedgerEntryRepository;
import com.example.taxassistant.domain.report.TaxReport;
import com.example.taxassistant.domain.report.TaxReportRepository;
import com.example.taxassistant.domain.transaction.Transaction;
import com.example.taxassistant.domain.transaction.TransactionRepository;
import com.example.taxassistant.domain.user.User;
import com.example.taxassistant.domain.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
class DomainRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRuleRepository categoryRuleRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private TaxReportRepository taxReportRepository;

    @Autowired
    private ChecklistItemRepository checklistItemRepository;

    @Test
    void persistsCoreDomainGraph() {
        User user = new User("owner@example.com", "$2a$10$hashed-password", "Owner");
        Business business = new Business("Sample Store");
        business.updateBookkeepingProfile(
                BusinessIndustryGroup.GROUP_B,
                false,
                LocalDate.of(2026, 1, 1),
                new BigDecimal("120000000.00"),
                BookkeepingType.SIMPLE_CANDIDATE
        );
        user.addBusiness(business);
        userRepository.saveAndFlush(user);

        Business savedBusiness = businessRepository.findAllByOwnerId(user.getId()).get(0);
        UploadedFile uploadedFile = uploadedFileRepository.saveAndFlush(new UploadedFile(
                savedBusiness,
                "transactions.csv",
                "local/2026/transactions.csv",
                "text/csv",
                1024
        ));
        uploadedFile.markParsed();

        CategoryRule categoryRule = categoryRuleRepository.saveAndFlush(new CategoryRule(
                savedBusiness,
                "coupang",
                MatchType.MERCHANT_CONTAINS,
                "supplies",
                TransactionType.EXPENSE,
                false
        ));

        Transaction transaction = new Transaction(
                savedBusiness,
                uploadedFile,
                LocalDate.of(2026, 5, 1),
                "coupang",
                "office supplies",
                new BigDecimal("33000.00"),
                TransactionType.EXPENSE
        );
        transaction.attachRawData(2, "{\"merchant\":\"coupang\"}");
        transaction.applyCategory("supplies", ClassificationStatus.AUTO_CLASSIFIED);
        Transaction savedTransaction = transactionRepository.saveAndFlush(transaction);

        LedgerEntry ledgerEntry = ledgerEntryRepository.saveAndFlush(new LedgerEntry(
                savedBusiness,
                savedTransaction,
                LocalDate.of(2026, 5, 1),
                "supplies",
                "office supplies",
                BigDecimal.ZERO,
                new BigDecimal("33000.00")
        ));

        TaxReport report = taxReportRepository.saveAndFlush(new TaxReport(
                savedBusiness,
                ReportPeriodType.MONTHLY,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                BigDecimal.ZERO,
                new BigDecimal("33000.00"),
                0
        ));

        ChecklistItem checklistItem = checklistItemRepository.saveAndFlush(new ChecklistItem(
                savedBusiness,
                savedTransaction,
                ChecklistItemType.MISSING_EVIDENCE,
                Severity.WARNING,
                "Evidence review",
                "Check whether supporting documents are attached."
        ));

        assertThat(user.getId()).isNotNull();
        assertThat(savedBusiness.getOwner().getId()).isEqualTo(user.getId());
        assertThat(uploadedFile.getProcessingStatus()).isEqualTo(FileProcessingStatus.PARSED);
        assertThat(categoryRuleRepository.findAllByBusinessIdAndActiveTrue(savedBusiness.getId()))
                .extracting(CategoryRule::getId)
                .containsExactly(categoryRule.getId());
        assertThat(transactionRepository.countByBusinessIdAndClassificationStatus(
                savedBusiness.getId(),
                ClassificationStatus.AUTO_CLASSIFIED
        )).isEqualTo(1);
        assertThat(ledgerEntry.getTransaction().getId()).isEqualTo(savedTransaction.getId());
        assertThat(report.getNotice()).isEqualTo(LegalNotice.REFERENCE_ONLY);
        assertThat(checklistItem.getStatus()).isEqualTo(ChecklistItemStatus.OPEN);
        assertThat(checklistItemRepository.findAllByBusinessIdAndStatus(
                savedBusiness.getId(),
                ChecklistItemStatus.OPEN
        )).hasSize(1);
    }

    @Test
    void userDefaultStringDoesNotExposeSensitiveFields() {
        User user = new User("owner@example.com", "sensitive-password-hash", "Owner");

        assertThat(user.toString())
                .doesNotContain("owner@example.com")
                .doesNotContain("sensitive-password-hash");
    }
}

