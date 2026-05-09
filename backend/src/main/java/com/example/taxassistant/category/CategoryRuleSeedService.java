package com.example.taxassistant.category;

import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.category.CategoryRule;
import com.example.taxassistant.domain.category.CategoryRuleRepository;
import com.example.taxassistant.domain.enums.MatchType;
import com.example.taxassistant.domain.enums.TransactionType;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryRuleSeedService {

    private static final List<DefaultCategoryRuleSeed> DEFAULT_RULES = List.of(
            expenseMerchant("쿠팡", "소모품비"),
            expenseMerchant("다이소", "소모품비"),
            expenseDescription("문구", "소모품비"),
            expenseMerchant("네이버광고", "광고선전비"),
            expenseMerchant("카카오광고", "광고선전비"),
            expenseMerchant("인스타그램", "광고선전비"),
            expenseMerchant("배달의민족", "지급수수료"),
            expenseMerchant("요기요", "지급수수료"),
            expenseMerchant("쿠팡이츠", "지급수수료"),
            expenseMerchant("SK주유소", "차량유지비"),
            expenseMerchant("GS칼텍스", "차량유지비"),
            expenseMerchant("현대오일뱅크", "차량유지비"),
            expenseMerchant("KT", "통신비"),
            expenseMerchant("SKT", "통신비"),
            expenseMerchant("LGU+", "통신비"),
            expenseMerchant("카카오T", "여비교통비"),
            expenseDescription("택시", "여비교통비"),
            expenseDescription("임대료", "임차료"),
            expenseDescription("월세", "임차료"),
            expenseDescription("전기요금", "수도광열비"),
            expenseDescription("수도요금", "수도광열비"),
            expenseDescription("도시가스", "수도광열비"),
            reviewMerchant("국민연금", "보험료 또는 복리후생비 후보"),
            reviewMerchant("건강보험", "보험료 또는 복리후생비 후보")
    );

    private final CategoryRuleRepository categoryRuleRepository;

    public CategoryRuleSeedService(CategoryRuleRepository categoryRuleRepository) {
        this.categoryRuleRepository = categoryRuleRepository;
    }

    @Transactional
    public void seedDefaultsIfMissing(Business business) {
        List<CategoryRule> existingRules = categoryRuleRepository.findAllByBusinessId(business.getId());
        Set<String> existingKeys = existingRules.stream()
                .map(rule -> key(rule.getKeyword(), rule.getMatchType(), rule.getCategoryName()))
                .collect(Collectors.toSet());

        List<CategoryRule> missingRules = DEFAULT_RULES.stream()
                .filter(rule -> !existingKeys.contains(key(rule.keyword(), rule.matchType(), rule.categoryName())))
                .map(rule -> new CategoryRule(
                        business,
                        rule.keyword(),
                        rule.matchType(),
                        rule.categoryName(),
                        rule.transactionType(),
                        rule.requiresReview()
                ))
                .toList();

        if (!missingRules.isEmpty()) {
            categoryRuleRepository.saveAll(missingRules);
        }
    }

    private static DefaultCategoryRuleSeed expenseMerchant(String keyword, String categoryName) {
        return new DefaultCategoryRuleSeed(
                keyword,
                MatchType.MERCHANT_CONTAINS,
                categoryName,
                TransactionType.EXPENSE,
                false
        );
    }

    private static DefaultCategoryRuleSeed expenseDescription(String keyword, String categoryName) {
        return new DefaultCategoryRuleSeed(
                keyword,
                MatchType.DESCRIPTION_CONTAINS,
                categoryName,
                TransactionType.EXPENSE,
                false
        );
    }

    private static DefaultCategoryRuleSeed reviewMerchant(String keyword, String categoryName) {
        return new DefaultCategoryRuleSeed(
                keyword,
                MatchType.MERCHANT_CONTAINS,
                categoryName,
                TransactionType.EXPENSE,
                true
        );
    }

    private String key(String keyword, MatchType matchType, String categoryName) {
        return keyword.toLowerCase(Locale.ROOT) + "|" + matchType.name() + "|" + categoryName;
    }
}

