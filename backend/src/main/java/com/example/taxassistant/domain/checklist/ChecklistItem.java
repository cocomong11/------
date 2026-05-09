package com.example.taxassistant.domain.checklist;

import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.common.BaseEntity;
import com.example.taxassistant.domain.enums.ChecklistItemStatus;
import com.example.taxassistant.domain.enums.ChecklistItemType;
import com.example.taxassistant.domain.enums.Severity;
import com.example.taxassistant.domain.transaction.Transaction;
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
@Table(name = "checklist_items")
public class ChecklistItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 40)
    private ChecklistItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ChecklistItemStatus status = ChecklistItemStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private Severity severity;

    @NotBlank
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    protected ChecklistItem() {
    }

    public ChecklistItem(
            Business business,
            Transaction transaction,
            ChecklistItemType itemType,
            Severity severity,
            String title,
            String description
    ) {
        this.business = business;
        this.transaction = transaction;
        this.itemType = itemType;
        this.severity = severity;
        this.title = title;
        this.description = description;
    }

    public void resolve() {
        this.status = ChecklistItemStatus.RESOLVED;
    }

    public UUID getId() {
        return id;
    }

    public Business getBusiness() {
        return business;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public ChecklistItemType getItemType() {
        return itemType;
    }

    public ChecklistItemStatus getStatus() {
        return status;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}

