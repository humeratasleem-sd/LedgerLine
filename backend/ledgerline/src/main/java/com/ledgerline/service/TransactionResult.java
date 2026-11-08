package com.ledgerline.service;

import com.ledgerline.dto.TransactionResponse;

public record TransactionResult(TransactionResponse transaction, boolean accepted) {
}