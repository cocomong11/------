package com.example.taxassistant.domain.category;

import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.common.BaseEntity;
import com.example.taxassistant.domain.enums.MatchType;
import com.example.taxassistant.domain.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Entity
@Table(name = "category_rules")
public class CategoryRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @NotBlank
    @Column(name = "keyword", nullable = false, length = 150)
    private String keyword;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 30)
    private MatchType matchType;

    @NotBlank
    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 30)
    private TransactionType transactionType;

    @Column(name = "requires_review", nullable = false)
    private boolean requiresReview;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected CategoryRule() {
    }

    public CategoryRule(
            Business business,
            String keyword,
            MatchType matchType,
            String categoryName,
            TransactionType transactionType,
            boolean requiresReview
    ) {
        this.business = business;
        this.keyword = keyword;
        this.matchType = matchType;
        this.categoryName = categoryName;
        this.transactionType = transactionType;
        this.requiresReview = requiresReview;
    }

    public UUID getId() {
        return id;
    }

    public Business getBusiness() {
        return business;
    }

    public String getKeyword() {
        return keyword;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public boolean isRequiresReview() {
        return requiresReview;
    }

    public boolean isActive() {
        return active;
    }
}

