package com.example.taxassistant.business;

import com.example.taxassistant.business.dto.BookkeepingPredictionResponse;
import com.example.taxassistant.common.LegalNotice;
import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.enums.BookkeepingType;
import com.example.taxassistant.domain.enums.BusinessIndustryGroup;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class BookkeepingPredictionService {

    private static final BigDecimal GROUP_A_THRESHOLD = new BigDecimal("300000000");
    private static final BigDecimal GROUP_B_THRESHOLD = new BigDecimal("150000000");
    private static final BigDecimal GROUP_C_THRESHOLD = new BigDecimal("75000000");
    private static final String PREDICTION_NOTICE =
            "입력값 기준 예상 판정입니다. " + LegalNotice.REFERENCE_ONLY;

    private final Clock clock;

    public BookkeepingPredictionService() {
        this(Clock.systemDefaultZone());
    }

    BookkeepingPredictionService(Clock clock) {
        this.clock = clock;
    }

    public BookkeepingType predictType(
            BusinessIndustryGroup industryGroup,
            boolean professionalBusiness,
            LocalDate openedOn,
            BigDecimal previousYearRevenue
    ) {
        if (professionalBusiness) {
            return BookkeepingType.DOUBLE_ENTRY_REQUIRED;
        }
        if (isNewBusiness(openedOn)) {
            return BookkeepingType.SIMPLE_CANDIDATE;
        }
        BigDecimal threshold = thresholdFor(industryGroup);
        if (threshold == null || previousYearRevenue == null) {
            return BookkeepingType.NEEDS_REVIEW;
        }
        return previousYearRevenue.compareTo(threshold) < 0
                ? BookkeepingType.SIMPLE_CANDIDATE
                : BookkeepingType.DOUBLE_ENTRY_REQUIRED;
    }

    public BookkeepingPredictionResponse describe(Business business) {
        return describe(
                business.getIndustryGroup(),
                business.isProfessionalBusiness(),
                business.getOpenedOn(),
                business.getPreviousYearRevenue(),
                business.getBookkeepingType()
        );
    }

    public BookkeepingPredictionResponse describe(
            BusinessIndustryGroup industryGroup,
            boolean professionalBusiness,
            LocalDate openedOn,
            BigDecimal previousYearRevenue,
            BookkeepingType bookkeepingType
    ) {
        String title = switch (bookkeepingType) {
            case SIMPLE_CANDIDATE -> "간편장부 대상 가능성이 있습니다";
            case DOUBLE_ENTRY_REQUIRED -> "복식부기의무자 가능성이 있습니다";
            case NEEDS_REVIEW -> "추가 확인이 필요합니다";
        };
        String reason = reason(industryGroup, professionalBusiness, openedOn, previousYearRevenue, bookkeepingType);
        return new BookkeepingPredictionResponse(bookkeepingType, title, reason, PREDICTION_NOTICE);
    }

    private String reason(
            BusinessIndustryGroup industryGroup,
            boolean professionalBusiness,
            LocalDate openedOn,
            BigDecimal previousYearRevenue,
            BookkeepingType bookkeepingType
    ) {
        if (professionalBusiness) {
            return "전문직 사업자는 수입금액과 관계없이 복식부기의무자로 분류될 수 있습니다.";
        }
        if (isNewBusiness(openedOn)) {
            return "신규 사업자는 입력값 기준으로 간편장부 대상 가능성이 높습니다.";
        }
        BigDecimal threshold = thresholdFor(industryGroup);
        if (threshold == null || previousYearRevenue == null) {
            return "업종 그룹 또는 직전연도 수입금액이 부족해 예상 판정에 추가 확인이 필요합니다.";
        }
        String comparison = bookkeepingType == BookkeepingType.SIMPLE_CANDIDATE ? "미만" : "이상";
        return "직전연도 수입금액이 해당 업종 그룹 기준금액 " + threshold.toPlainString() + "원 " + comparison + "입니다.";
    }

    private boolean isNewBusiness(LocalDate openedOn) {
        return openedOn != null && openedOn.getYear() == LocalDate.now(clock).getYear();
    }

    private BigDecimal thresholdFor(BusinessIndustryGroup industryGroup) {
        return switch (industryGroup) {
            case GROUP_A -> GROUP_A_THRESHOLD;
            case GROUP_B -> GROUP_B_THRESHOLD;
            case GROUP_C -> GROUP_C_THRESHOLD;
            case UNKNOWN -> null;
        };
    }
}

