package com.example.taxassistant.business;

import com.example.taxassistant.business.dto.BusinessVerificationRequest;
import com.example.taxassistant.domain.business.Business;
import com.example.taxassistant.domain.enums.BusinessVerificationStatus;
import org.springframework.stereotype.Component;

@Component
public class MockBusinessVerificationProvider implements BusinessVerificationProvider {

    @Override
    public VerificationResult verify(Business business, BusinessVerificationRequest request) {
        String registrationNumber = normalize(request.businessRegistrationNumber());
        if (registrationNumber.length() != 10) {
            return new VerificationResult(
                    BusinessVerificationStatus.NEEDS_REVIEW,
                    "검토가 필요합니다",
                    "사업자등록번호 형식을 다시 확인해주세요."
            );
        }
        if (registrationNumber.endsWith("00000")) {
            return new VerificationResult(
                    BusinessVerificationStatus.FAILED,
                    "검증에 실패했습니다",
                    "폐업 또는 유효하지 않은 사업자로 확인되는 예시 번호입니다."
            );
        }
        if (mismatch(business.getRepresentativeName(), request.representativeName())
                || business.getOpenedOn() == null
                || !business.getOpenedOn().equals(request.openedOn())) {
            return new VerificationResult(
                    BusinessVerificationStatus.NEEDS_REVIEW,
                    "입력값 확인이 필요합니다",
                    "대표자명 또는 개업일자가 등록된 사업자 정보와 다릅니다."
            );
        }
        return new VerificationResult(
                BusinessVerificationStatus.VERIFIED,
                "사업자 검증 완료",
                "입력한 사업자 정보 형식이 정상으로 확인되었습니다."
        );
    }

    private boolean mismatch(String savedValue, String requestedValue) {
        if (savedValue == null || savedValue.isBlank()) {
            return false;
        }
        return !savedValue.trim().equals(requestedValue.trim());
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}
