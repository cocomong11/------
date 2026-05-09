package com.example.taxassistant.transactions;

import com.example.taxassistant.security.UserPrincipal;
import com.example.taxassistant.transactions.dto.ClassificationResultResponse;
import com.example.taxassistant.transactions.dto.TransactionResponse;
import com.example.taxassistant.transactions.dto.TransactionUpdateRequest;
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
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionClassificationService classificationService;

    public TransactionController(
            TransactionService transactionService,
            TransactionClassificationService classificationService
    ) {
        this.transactionService = transactionService;
        this.classificationService = classificationService;
    }

    @GetMapping("/businesses/{businessId}/transactions")
    public List<TransactionResponse> findAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID businessId
    ) {
        return transactionService.findAll(principal.getId(), businessId);
    }

    @PostMapping("/businesses/{businessId}/classify-transactions")
    public ClassificationResultResponse classify(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID businessId
    ) {
        return classificationService.classify(principal.getId(), businessId);
    }

    @PatchMapping("/transactions/{id}")
    public TransactionResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody TransactionUpdateRequest request
    ) {
        return transactionService.update(principal.getId(), id, request);
    }
}

