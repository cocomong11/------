package com.example.taxassistant.domain.agreement;

import com.example.taxassistant.domain.common.BaseEntity;
import com.example.taxassistant.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agreements")
public class Agreement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "terms_version", nullable = false, length = 30)
    private String termsVersion;

    @Column(name = "privacy_version", nullable = false, length = 30)
    private String privacyVersion;

    @Column(name = "business_info_consent_version", nullable = false, length = 30)
    private String businessInfoConsentVersion;

    @Column(name = "tax_data_consent_version", nullable = false, length = 30)
    private String taxDataConsentVersion;

    @Column(name = "reference_notice_version", nullable = false, length = 30)
    private String referenceNoticeVersion;

    @Column(name = "agreed_at", nullable = false)
    private Instant agreedAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    protected Agreement() {
    }

    public Agreement(
            User user,
            String termsVersion,
            String privacyVersion,
            String businessInfoConsentVersion,
            String taxDataConsentVersion,
            String referenceNoticeVersion,
            String ipAddress
    ) {
        this.user = user;
        this.termsVersion = termsVersion;
        this.privacyVersion = privacyVersion;
        this.businessInfoConsentVersion = businessInfoConsentVersion;
        this.taxDataConsentVersion = taxDataConsentVersion;
        this.referenceNoticeVersion = referenceNoticeVersion;
        this.ipAddress = ipAddress;
        this.agreedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Instant getAgreedAt() {
        return agreedAt;
    }
}
