package com.example.taxassistant.business;

import com.example.taxassistant.auth.AuthService;
import com.example.taxassistant.business.dto.BookkeepingPredictionResponse;
import com.example.taxassistant.business.dto.BusinessRequest;
import com.example.taxassistant.business.dto.BusinessResponse;
import com.example.taxassistant.business.dto.BusinessVerificationRequest;
import com.example.taxassistant.business.dto.BusinessVerificationResponse;
import com.example.taxassistant.category.CategoryRuleSeedService;
import com.example.taxassistant.common.error.BusinessException;
import com.example.taxassistant.common.error.ErrorCode;
import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.business.BusinessRepository;
import com.example.taxassistant.domain.enums.BookkeepingType;
import com.example.taxassistant.domain.enums.BusinessVerificationStatus;
import com.example.taxassistant.domain.user.User;
import com.example.taxassistant.domain.user.UserRepository;
import com.example.taxassistant.security.ResourceOwnershipService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final BookkeepingPredictionService predictionService;
    private final CategoryRuleSeedService categoryRuleSeedService;
    private final ResourceOwnershipService ownershipService;
    private final AuthService authService;
    private final BusinessVerificationProvider verificationProvider;

    public BusinessService(
            BusinessRepository businessRepository,
            UserRepository userRepository,
            BookkeepingPredictionService predictionService,
            CategoryRuleSeedService categoryRuleSeedService,
            ResourceOwnershipService ownershipService,
            AuthService authService,
            BusinessVerificationProvider verificationProvider
    ) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.predictionService = predictionService;
        this.categoryRuleSeedService = categoryRuleSeedService;
        this.ownershipService = ownershipService;
        this.authService = authService;
        this.verificationProvider = verificationProvider;
    }

    @Transactional
    public BusinessResponse create(UUID userId, BusinessRequest request) {
        requireAgreement(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        BookkeepingType bookkeepingType = predictionService.predictType(
                request.industryGroup(),
                request.professionalBusiness(),
                request.openedOn(),
                request.previousYearRevenue()
        );
        Business business = new Business(request.name().trim());
        business.updateBasicInfo(
                request.name().trim(),
                blankToNull(request.businessRegistrationNumber()),
                blankToNull(request.representativeName()),
                blankToNull(request.industryName())
        );
        business.updateBookkeepingProfile(
                request.industryGroup(),
                request.professionalBusiness(),
                request.hasEmployees(),
                blankToNull(request.taxationType()),
                request.openedOn(),
                request.previousYearRevenue(),
                bookkeepingType
        );
        user.addBusiness(business);
        Business savedBusiness = businessRepository.save(business);
        categoryRuleSeedService.seedDefaultsIfMissing(savedBusiness);
        return toResponse(savedBusiness);
    }

    @Transactional(readOnly = true)
    public List<BusinessResponse> findAll(UUID userId) {
        return businessRepository.findAllByOwnerId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BusinessResponse findOne(UUID userId, UUID businessId) {
        return toResponse(ownershipService.requireOwnedBusiness(userId, businessId));
    }

    @Transactional
    public BusinessResponse update(UUID userId, UUID businessId, BusinessRequest request) {
        Business business = ownershipService.requireOwnedBusiness(userId, businessId);
        BookkeepingType bookkeepingType = predictionService.predictType(
                request.industryGroup(),
                request.professionalBusiness(),
                request.openedOn(),
                request.previousYearRevenue()
        );
        business.updateBasicInfo(
                request.name().trim(),
                blankToNull(request.businessRegistrationNumber()),
                blankToNull(request.representativeName()),
                blankToNull(request.industryName())
        );
        business.updateBookkeepingProfile(
                request.industryGroup(),
                request.professionalBusiness(),
                request.hasEmployees(),
                blankToNull(request.taxationType()),
                request.openedOn(),
                request.previousYearRevenue(),
                bookkeepingType
        );
        business.updateVerificationStatus(BusinessVerificationStatus.NOT_STARTED);
        return toResponse(business);
    }

    @Transactional
    public BusinessVerificationResponse verify(UUID userId, UUID businessId, BusinessVerificationRequest request) {
        Business business = ownershipService.requireOwnedBusiness(userId, businessId);
        business.updateVerificationStatus(BusinessVerificationStatus.PENDING);
        BusinessVerificationProvider.VerificationResult result = verificationProvider.verify(business, request);
        business.updateVerificationStatus(result.status());
        return new BusinessVerificationResponse(
                business.getId(),
                result.status(),
                result.title(),
                result.message(),
                "입력값 기준 확인 결과이며, 최종 기장의무와 신고 책임은 사용자에게 있습니다."
        );
    }

    private BusinessResponse toResponse(Business business) {
        BookkeepingPredictionResponse prediction = predictionService.describe(business);
        return BusinessResponse.from(business, prediction);
    }

    private void requireAgreement(UUID userId) {
        if (!authService.hasRequiredAgreements(userId)) {
            throw new BusinessException(ErrorCode.AGREEMENT_REQUIRED);
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
