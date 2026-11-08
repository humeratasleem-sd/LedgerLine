package com.ledgerline.dto;

import java.time.Instant;

public record TransactionResponse(
        long id,
        String transactionType,
        String status,
        String amountPaise,
        String currency,
        Long sourceAccountId,
        Long destinationAccountId,
        String failureCode,
        String externalReference,
        Instant createdAt) {
}