package com.ledgerline.controller;

import java.util.List;

import com.ledgerline.dto.AuditEventResponse;
import com.ledgerline.service.AuditEventService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditEventService auditEventService;

    public AuditController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @GetMapping("/events")
    public List<AuditEventResponse> latestEvents(
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return auditEventService.latestEvents(limit);
    }
}
