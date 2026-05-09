package com.example.taxassistant.business;

import com.example.taxassistant.business.dto.BookkeepingPredictionResponse;
import com.example.taxassistant.business.dto.BusinessRequest;
import com.example.taxassistant.business.dto.BusinessResponse;
import com.example.taxassistant.category.CategoryRuleSeedService;
import com.example.taxassistant.common.error.BusinessException;
import com.example.taxassistant.common.error.ErrorCode;
import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.business.BusinessRepository;
import com.example.taxassistant.domain.enums.BookkeepingType;
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

    public BusinessService(
            BusinessRepository businessRepository,
            UserRepository userRepository,
            BookkeepingPredictionService predictionService,
            CategoryRuleSeedService categoryRuleSeedService,
            ResourceOwnershipService ownershipService
    ) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.predictionService = predictionService;
        this.categoryRuleSeedService = categoryRuleSeedService;
        this.ownershipService = ownershipService;
    }

    @Transactional
    public BusinessResponse create(UUID userId, BusinessRequest request) {
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
                blankToNull(request.industryName())
        );
        business.updateBookkeepingProfile(
                request.industryGroup(),
                request.professionalBusiness(),
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
                blankToNull(request.industryName())
        );
        business.updateBookkeepingProfile(
                request.industryGroup(),
                request.professionalBusiness(),
                request.openedOn(),
                request.previousYearRevenue(),
                bookkeepingType
        );
        return toResponse(business);
    }

    private BusinessResponse toResponse(Business business) {
        BookkeepingPredictionResponse prediction = predictionService.describe(business);
        return BusinessResponse.from(business, prediction);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
