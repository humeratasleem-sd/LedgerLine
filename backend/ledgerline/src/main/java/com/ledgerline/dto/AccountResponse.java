package com.ledgerline.dto;

import java.time.Instant;

public record AccountResponse(
        Long id,
        String accountNumber,
        String currency,
        String balancePaise,
        String status,
        Instant createdAt,
        String permission) {
}
