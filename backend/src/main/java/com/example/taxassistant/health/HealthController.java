package com.example.taxassistant.health;

import com.example.taxassistant.common.LegalNotice;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("UP", Instant.now(), LegalNotice.REFERENCE_ONLY);
    }
}

