package com.example.taxassistant.business;

import com.example.taxassistant.business.dto.BusinessRequest;
import com.example.taxassistant.business.dto.BusinessResponse;
import com.example.taxassistant.business.dto.BusinessVerificationRequest;
import com.example.taxassistant.business.dto.BusinessVerificationResponse;
import com.example.taxassistant.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/businesses")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @PostMapping
    public BusinessResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BusinessRequest request
    ) {
        return businessService.create(principal.getId(), request);
    }

    @GetMapping
    public List<BusinessResponse> findAll(@AuthenticationPrincipal UserPrincipal principal) {
        return businessService.findAll(principal.getId());
    }

    @GetMapping("/{id}")
    public BusinessResponse findOne(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return businessService.findOne(principal.getId(), id);
    }

    @PatchMapping("/{id}")
    public BusinessResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody BusinessRequest request
    ) {
        return businessService.update(principal.getId(), id, request);
    }

    @PostMapping("/{id}/verify")
    public BusinessVerificationResponse verify(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody BusinessVerificationRequest request
    ) {
        return businessService.verify(principal.getId(), id, request);
    }
}
