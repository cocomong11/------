package com.example.taxassistant.transactions;

import com.example.taxassistant.domain.category.CategoryRule;
import com.example.taxassistant.domain.category.CategoryRuleRepository;
import com.example.taxassistant.domain.enums.ClassificationStatus;
import com.example.taxassistant.domain.enums.MatchType;
import com.example.taxassistant.domain.transaction.Transaction;
import com.example.taxassistant.domain.transaction.TransactionRepository;
import com.example.taxassistant.security.ResourceOwnershipService;
import com.example.taxassistant.transactions.dto.TransactionResponse;
import com.example.taxassistant.transactions.dto.TransactionUpdateRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRuleRepository categoryRuleRepository;
    private final ResourceOwnershipService ownershipService;

    public TransactionService(
            TransactionRepository transactionRepository,
            CategoryRuleRepository categoryRuleRepository,
            ResourceOwnershipService ownershipService
    ) {
        this.transactionRepository = transactionRepository;
        this.categoryRuleRepository = categoryRuleRepository;
        this.ownershipService = ownershipService;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findAll(UUID userId, UUID businessId) {
        ownershipService.requireOwnedBusiness(userId, businessId);
        return transactionRepository.findAllByBusinessIdAndBusinessOwnerIdOrderByTransactionDateDescCreatedAtDesc(
                        businessId,
                        userId
                )
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional
    public TransactionResponse update(UUID userId, UUID transactionId, TransactionUpdateRequest request) {
        Transaction transaction = ownershipService.requireOwnedTransaction(userId, transactionId);

        String categoryName = blankToNull(request.categoryName());
        transaction.applyCategory(categoryName, categoryName == null
                ? ClassificationStatus.NEEDS_REVIEW
                : ClassificationStatus.USER_CONFIRMED);
        transaction.updateMemo(blankToNull(request.userMemo()));

        if (categoryName != null) {
            savePersonalRule(transaction, categoryName);
        }

        return TransactionResponse.from(transaction);
    }

    private void savePersonalRule(Transaction transaction, String categoryName) {
        String keyword = blankToNull(transaction.getMerchantName());
        MatchType matchType = MatchType.EXACT_MERCHANT;
        if (keyword == null) {
            keyword = blankToNull(transaction.getDescription());
            matchType = MatchType.DESCRIPTION_CONTAINS;
        }
        if (keyword == null) {
            return;
        }

        boolean exists = categoryRuleRepository
                .findFirstByBusinessIdAndKeywordIgnoreCaseAndActiveTrue(transaction.getBusiness().getId(), keyword)
                .filter(rule -> rule.getCategoryName().equals(categoryName))
                .isPresent();
        if (exists) {
            return;
        }

        categoryRuleRepository.save(new CategoryRule(
                transaction.getBusiness(),
                keyword,
                matchType,
                categoryName,
                transaction.getTransactionType(),
                false
        ));
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
