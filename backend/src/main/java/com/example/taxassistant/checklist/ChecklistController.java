package com.example.taxassistant.checklist;

import com.example.taxassistant.checklist.dto.ChecklistResponse;
import com.example.taxassistant.security.UserPrincipal;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/businesses/{businessId}/checklist")
public class ChecklistController {

    private final ChecklistService checklistService;

    public ChecklistController(ChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @GetMapping
    public ChecklistResponse checklist(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID businessId
    ) {
        return checklistService.generate(principal.getId(), businessId);
    }
}
