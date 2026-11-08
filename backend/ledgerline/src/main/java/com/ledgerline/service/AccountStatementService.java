package com.ledgerline.service;

import java.util.List;

import com.ledgerline.dto.TransactionResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountStatementService {

    private final JdbcTemplate jdbcTemplate;
    private final AccountAuthorizationService accountAuthorizationService;

    public AccountStatementService(
            JdbcTemplate jdbcTemplate,
            AccountAuthorizationService accountAuthorizationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.accountAuthorizationService = accountAuthorizationService;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> latestForAccount(
            Authentication authentication, long accountId, int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("The statement limit must be between 1 and 100.");
        }
        accountAuthorizationService.requireViewAccount(authentication, accountId);
        return jdbcTemplate.query("""
                SELECT id, transaction_type, status, amount_paise, currency,
                       source_account_id, destination_account_id, failure_code,
                       external_reference, created_at
                FROM transactions
                WHERE source_account_id = ? OR destination_account_id = ?
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """, (resultSet, rowNumber) -> {
            long sourceId = resultSet.getLong("source_account_id");
            Long sourceAccountId = resultSet.wasNull() ? null : sourceId;
            long destinationId = resultSet.getLong("destination_account_id");
            Long destinationAccountId = resultSet.wasNull() ? null : destinationId;
            return new TransactionResponse(
                    resultSet.getLong("id"),
                    resultSet.getString("transaction_type"),
                    resultSet.getString("status"),
                    Long.toString(resultSet.getLong("amount_paise")),
                    resultSet.getString("currency").trim(),
                    sourceAccountId,
                    destinationAccountId,
                    resultSet.getString("failure_code"),
                    resultSet.getString("external_reference"),
                    resultSet.getTimestamp("created_at").toInstant());
        }, accountId, accountId, limit);
    }
}