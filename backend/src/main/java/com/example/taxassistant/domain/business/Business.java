package com.example.taxassistant.domain.business;

import com.example.taxassistant.domain.common.BaseEntity;
import com.example.taxassistant.domain.enums.BookkeepingType;
import com.example.taxassistant.domain.enums.BusinessIndustryGroup;
import com.example.taxassistant.domain.user.User;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "businesses")
public class Business extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @NotBlank
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "business_registration_number", length = 30)
    private String businessRegistrationNumber;

    @Column(name = "industry_name", length = 150)
    private String industryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "industry_group", nullable = false, length = 30)
    private BusinessIndustryGroup industryGroup = BusinessIndustryGroup.UNKNOWN;

    @Column(name = "professional_business", nullable = false)
    private boolean professionalBusiness;

    @Column(name = "opened_on")
    private LocalDate openedOn;

    @Column(name = "previous_year_revenue", precision = 18, scale = 2)
    private BigDecimal previousYearRevenue;

    @Enumerated(EnumType.STRING)
    @Column(name = "bookkeeping_type", nullable = false, length = 30)
    private BookkeepingType bookkeepingType = BookkeepingType.NEEDS_REVIEW;

    protected Business() {
    }

    public Business(String name) {
        this.name = name;
    }

    public void assignOwner(User owner) {
        this.owner = owner;
    }

    public void updateBasicInfo(
            String name,
            String businessRegistrationNumber,
            String industryName
    ) {
        this.name = name;
        this.businessRegistrationNumber = businessRegistrationNumber;
        this.industryName = industryName;
    }

    public void updateBookkeepingProfile(
            BusinessIndustryGroup industryGroup,
            boolean professionalBusiness,
            LocalDate openedOn,
            BigDecimal previousYearRevenue,
            BookkeepingType bookkeepingType
    ) {
        this.industryGroup = industryGroup;
        this.professionalBusiness = professionalBusiness;
        this.openedOn = openedOn;
        this.previousYearRevenue = previousYearRevenue;
        this.bookkeepingType = bookkeepingType;
    }

    public UUID getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getBusinessRegistrationNumber() {
        return businessRegistrationNumber;
    }

    public String getIndustryName() {
        return industryName;
    }

    public BusinessIndustryGroup getIndustryGroup() {
        return industryGroup;
    }

    public boolean isProfessionalBusiness() {
        return professionalBusiness;
    }

    public LocalDate getOpenedOn() {
        return openedOn;
    }

    public BigDecimal getPreviousYearRevenue() {
        return previousYearRevenue;
    }

    public BookkeepingType getBookkeepingType() {
        return bookkeepingType;
    }
}
