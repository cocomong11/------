package com.example.taxassistant.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.taxassistant.common.error.BusinessException;
import com.example.taxassistant.common.error.ErrorCode;
import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.enums.ReportPeriodType;
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
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@Import(ResourceOwnershipService.class)
class ResourceOwnershipServiceTest {

    @Autowired
    private ResourceOwnershipService ownershipService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private TaxReportRepository taxReportRepository;

    @Test
    void verifiesOwnershipForAllProtectedResources() {
        User owner = new User("owner@example.com", "password-hash", "Owner");
        Business business = new Business("Owner Business");
        owner.addBusiness(business);
        User other = new User("other@example.com", "password-hash", "Other");
        userRepository.saveAndFlush(owner);
        userRepository.saveAndFlush(other);

        UploadedFile uploadedFile = uploadedFileRepository.saveAndFlush(new UploadedFile(
                business,
                "transactions.csv",
                "owner/transactions.csv",
                "text/csv",
                100
        ));
        Transaction transaction = transactionRepository.saveAndFlush(new Transaction(
                business,
                uploadedFile,
                LocalDate.of(2026, 5, 1),
                "거래처",
                "매출",
                new BigDecimal("10000"),
                TransactionType.INCOME
        ));
        LedgerEntry ledgerEntry = ledgerEntryRepository.saveAndFlush(new LedgerEntry(
                business,
                transaction,
                LocalDate.of(2026, 5, 1),
                "매출",
                "매출",
                new BigDecimal("10000"),
                BigDecimal.ZERO
        ));
        TaxReport taxReport = taxReportRepository.saveAndFlush(new TaxReport(
                business,
                ReportPeriodType.MONTHLY,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                0
        ));

        assertThatCode(() -> ownershipService.requireOwnedBusiness(owner.getId(), business.getId()))
                .doesNotThrowAnyException();
        assertThatCode(() -> ownershipService.requireOwnedTransaction(owner.getId(), transaction.getId()))
                .doesNotThrowAnyException();
        assertThatCode(() -> ownershipService.requireOwnedUploadedFile(owner.getId(), uploadedFile.getId()))
                .doesNotThrowAnyException();
        assertThatCode(() -> ownershipService.requireOwnedLedgerEntry(owner.getId(), ledgerEntry.getId()))
                .doesNotThrowAnyException();
        assertThatCode(() -> ownershipService.requireOwnedTaxReport(owner.getId(), taxReport.getId()))
                .doesNotThrowAnyException();

        assertForbidden(() -> ownershipService.requireOwnedBusiness(other.getId(), business.getId()));
        assertForbidden(() -> ownershipService.requireOwnedTransaction(other.getId(), transaction.getId()));
        assertForbidden(() -> ownershipService.requireOwnedUploadedFile(other.getId(), uploadedFile.getId()));
        assertForbidden(() -> ownershipService.requireOwnedLedgerEntry(other.getId(), ledgerEntry.getId()));
        assertForbidden(() -> ownershipService.requireOwnedTaxReport(other.getId(), taxReport.getId()));
    }

    private void assertForbidden(ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
