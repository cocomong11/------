package com.example.taxassistant.transactions;

import com.example.taxassistant.category.CategoryRuleSeedService;
import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.category.CategoryRule;
import com.example.taxassistant.domain.category.CategoryRuleRepository;
import com.example.taxassistant.domain.enums.ClassificationStatus;
import com.example.taxassistant.domain.enums.MatchType;
import com.example.taxassistant.domain.transaction.Transaction;
import com.example.taxassistant.domain.transaction.TransactionRepository;
import com.example.taxassistant.security.ResourceOwnershipService;
import com.example.taxassistant.transactions.dto.ClassificationResultResponse;
import com.example.taxassistant.transactions.dto.TransactionResponse;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionClassificationService {

    private final TransactionRepository transactionRepository;
    private final CategoryRuleRepository categoryRuleRepository;
    private final CategoryRuleSeedService categoryRuleSeedService;
    private final ResourceOwnershipService ownershipService;

    public TransactionClassificationService(
            TransactionRepository transactionRepository,
            CategoryRuleRepository categoryRuleRepository,
            CategoryRuleSeedService categoryRuleSeedService,
            ResourceOwnershipService ownershipService
    ) {
        this.transactionRepository = transactionRepository;
        this.categoryRuleRepository = categoryRuleRepository;
        this.categoryRuleSeedService = categoryRuleSeedService;
        this.ownershipService = ownershipService;
    }

    @Transactional
    public ClassificationResultResponse classify(UUID userId, UUID businessId) {
        Business business = ownershipService.requireOwnedBusiness(userId, businessId);
        categoryRuleSeedService.seedDefaultsIfMissing(business);

        List<CategoryRule> rules = categoryRuleRepository.findAllByBusinessIdAndActiveTrue(businessId)
                .stream()
                .sorted(Comparator.comparingInt(this::priority))
                .toList();
        List<Transaction> transactions = transactionRepository
                .findAllByBusinessIdAndBusinessOwnerIdOrderByTransactionDateDescCreatedAtDesc(businessId, userId);

        int autoClassifiedCount = 0;
        int needsReviewCount = 0;
        for (Transaction transaction : transactions) {
            if (transaction.getClassificationStatus() == ClassificationStatus.USER_CONFIRMED) {
                continue;
            }

            CategoryRule matchedRule = findMatchedRule(transaction, rules);
            if (matchedRule == null) {
                transaction.applyCategory(null, ClassificationStatus.NEEDS_REVIEW);
                needsReviewCount++;
                continue;
            }

            ClassificationStatus status = matchedRule.isRequiresReview()
                    ? ClassificationStatus.NEEDS_REVIEW
                    : ClassificationStatus.AUTO_CLASSIFIED;
            transaction.applyCategory(matchedRule.getCategoryName(), status);
            if (status == ClassificationStatus.AUTO_CLASSIFIED) {
                autoClassifiedCount++;
            } else {
                needsReviewCount++;
            }
        }

        return new ClassificationResultResponse(
                transactions.size(),
                autoClassifiedCount,
                needsReviewCount,
                transactions.stream().map(TransactionResponse::from).toList()
        );
    }

    private CategoryRule findMatchedRule(Transaction transaction, List<CategoryRule> rules) {
        return rules.stream()
                .filter(rule -> rule.getTransactionType() == null
                        || rule.getTransactionType() == transaction.getTransactionType())
                .filter(rule -> matches(transaction, rule))
                .findFirst()
                .orElse(null);
    }

    private boolean matches(Transaction transaction, CategoryRule rule) {
        String keyword = normalize(rule.getKeyword());
        String merchant = normalize(transaction.getMerchantName());
        String description = normalize(transaction.getDescription());
        return switch (rule.getMatchType()) {
            case EXACT_MERCHANT -> !keyword.isBlank() && keyword.equals(merchant);
            case MERCHANT_CONTAINS -> !keyword.isBlank() && merchant.contains(keyword);
            case DESCRIPTION_CONTAINS -> !keyword.isBlank() && description.contains(keyword);
        };
    }

    private int priority(CategoryRule rule) {
        return switch (rule.getMatchType()) {
            case EXACT_MERCHANT -> 0;
            case MERCHANT_CONTAINS -> 1;
            case DESCRIPTION_CONTAINS -> 2;
        };
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
