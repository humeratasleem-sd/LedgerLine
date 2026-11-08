package com.ledgerline.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ledgerline.dto.DepositRequest;
import com.ledgerline.dto.TransactionResponse;
import com.ledgerline.dto.TransferRequest;
import com.ledgerline.dto.WithdrawalRequest;
import com.ledgerline.exception.ResourceConflictException;
import com.ledgerline.exception.ResourceNotFoundException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoneyMovementService {

    private static final String CLEARING_ACCOUNT_CODE = "EXTERNAL_CLEARING";

    private final JdbcTemplate jdbcTemplate;
    private final AccountAuthorizationService accountAuthorizationService;

    public MoneyMovementService(
            JdbcTemplate jdbcTemplate,
            AccountAuthorizationService accountAuthorizationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.accountAuthorizationService = accountAuthorizationService;
    }

    @Transactional
    public TransactionResult transfer(
            Authentication authentication, TransferRequest request, String idempotencyKey) {
        requireRole(authentication, "ACCOUNT_HOLDER");
        long userId = accountAuthorizationService.authenticatedUserId(authentication);
        return post(new Posting(
                userId,
                "TRANSFER",
                request.amountPaise(),
                request.sourceAccountId(),
                request.destinationAccountId(),
                null), idempotencyKey, authentication);
    }

    @Transactional
    public TransactionResult deposit(
            Authentication authentication, DepositRequest request, String idempotencyKey) {
        requireRole(authentication, "ADMIN");
        if (!request.externalSettlementVerified()) {
            throw new IllegalArgumentException("An administrator must attest to the external settlement.");
        }
        long userId = accountAuthorizationService.authenticatedUserId(authentication);
        return post(new Posting(
                userId,
                "DEPOSIT",
                request.amountPaise(),
                null,
                request.destinationAccountId(),
                request.externalReference()), idempotencyKey, authentication);
    }

    @Transactional
    public TransactionResult withdraw(
            Authentication authentication, WithdrawalRequest request, String idempotencyKey) {
        requireRole(authentication, "ADMIN");
        if (!request.externalSettlementVerified()) {
            throw new IllegalArgumentException("An administrator must attest to the external settlement.");
        }
        long userId = accountAuthorizationService.authenticatedUserId(authentication);
        return post(new Posting(
                userId,
                "WITHDRAWAL",
                request.amountPaise(),
                request.sourceAccountId(),
                null,
                request.externalReference()), idempotencyKey, authentication);
    }

    private TransactionResult post(Posting posting, String idempotencyKey, Authentication authentication) {
        validateIdempotencyKey(idempotencyKey);
        String requestHash = requestHash(posting);
        try {
            jdbcTemplate.update("""
                    INSERT INTO idempotency_keys (user_id, idempotency_key, request_hash)
                    VALUES (?, ?, ?)
                    """, posting.userId(), idempotencyKey, requestHash);
        } catch (DuplicateKeyException exception) {
            return existingRequest(posting.userId(), idempotencyKey, requestHash);
        }

        List<Long> accountIds = accountIds(posting);
        Map<Long, LockedAccount> accounts = lockAccounts(accountIds);
        LockedAccount source = posting.sourceAccountId() == null
                ? null : requireAccount(accounts, posting.sourceAccountId());
        LockedAccount destination = posting.destinationAccountId() == null
                ? null : requireAccount(accounts, posting.destinationAccountId());

        if (posting.transactionType().equals("TRANSFER")) {
            accountAuthorizationService.requireTransferPermission(
                    authentication, posting.sourceAccountId());
        }

        String failureCode = refusalCode(posting, source, destination);
        Long sourceBalance = source == null ? null : source.balancePaise();
        Long destinationBalance = destination == null ? null : destination.balancePaise();
        if (failureCode == null) {
            try {
                if (source != null) {
                    sourceBalance = Math.subtractExact(source.balancePaise(), posting.amountPaise());
                }
                if (destination != null) {
                    destinationBalance = Math.addExact(destination.balancePaise(), posting.amountPaise());
                }
            } catch (ArithmeticException exception) {
                failureCode = "BALANCE_LIMIT_EXCEEDED";
            }
        }

        boolean accepted = failureCode == null;
        String transactionCurrency = source != null ? source.currency() : destination.currency();
        long transactionId = insertTransaction(posting, accepted, failureCode, transactionCurrency);
        if (accepted) {
            reserveExternalSettlement(posting, transactionId);
            applyBalances(posting, sourceBalance, destinationBalance);
            insertLedgerEntries(posting, transactionId);
            verifyBalancedPosting(transactionId, posting.amountPaise());
        }

        String action = accepted ? "TRANSACTION_COMPLETED" : "TRANSACTION_REFUSED";
        jdbcTemplate.update("""
                INSERT INTO audit_events (actor_user_id, action, entity_type, entity_id, request_id)
                VALUES (?, ?, 'TRANSACTION', ?, ?)
                """, posting.userId(), action, Long.toString(transactionId), idempotencyKey);
        int linked = jdbcTemplate.update(
                "UPDATE idempotency_keys SET transaction_id = ? WHERE user_id = ? AND idempotency_key = ?",
                transactionId, posting.userId(), idempotencyKey);
        if (linked != 1) {
            throw new IllegalStateException("The idempotency record could not be linked to its transaction.");
        }

        return new TransactionResult(loadTransaction(transactionId), accepted);
    }

    private TransactionResult existingRequest(long userId, String idempotencyKey, String requestHash) {
        ExistingIdempotency existing = jdbcTemplate.query("""
                SELECT request_hash, transaction_id
                FROM idempotency_keys
                WHERE user_id = ? AND idempotency_key = ?
                FOR UPDATE
                """, resultSet -> resultSet.next()
                ? mapExistingIdempotency(resultSet)
                : null, userId, idempotencyKey);
        if (existing == null) {
            throw new ResourceConflictException("The idempotency key is currently unavailable. Retry the request.");
        }
        if (!MessageDigest.isEqual(
                existing.requestHash().getBytes(StandardCharsets.US_ASCII),
                requestHash.getBytes(StandardCharsets.US_ASCII))) {
            throw new ResourceConflictException("This idempotency key was already used for a different request.");
        }
        if (existing.transactionId() == null) {
            throw new ResourceConflictException("The previous request has no completed outcome. Use a new key.");
        }
        TransactionResponse response = loadTransaction(existing.transactionId());
        return new TransactionResult(response, response.status().equals("COMPLETED"));
    }

    private static ExistingIdempotency mapExistingIdempotency(java.sql.ResultSet resultSet)
            throws java.sql.SQLException {
        long transactionId = resultSet.getLong("transaction_id");
        boolean transactionIdIsNull = resultSet.wasNull();
        String requestHash = resultSet.getString("request_hash");
        return new ExistingIdempotency(
                requestHash,
                transactionIdIsNull ? null : transactionId);
    }

    private void reserveExternalSettlement(Posting posting, long transactionId) {
        if (posting.externalReference() == null) {
            return;
        }
        try {
            jdbcTemplate.update("""
                    INSERT INTO external_settlements (external_reference, transaction_id)
                    VALUES (?, ?)
                    """, posting.externalReference(), transactionId);
        } catch (DuplicateKeyException exception) {
            throw new ResourceConflictException(
                    "This external settlement reference has already been posted.");
        }
    }

    private Map<Long, LockedAccount> lockAccounts(List<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(accountIds.size(), "?"));
        List<LockedAccount> locked = jdbcTemplate.query("""
                SELECT id, balance_paise, currency, status
                FROM accounts
                WHERE id IN (%s)
                ORDER BY id
                FOR UPDATE
                """.formatted(placeholders),
                (resultSet, rowNumber) -> new LockedAccount(
                        resultSet.getLong("id"),
                        resultSet.getLong("balance_paise"),
                        resultSet.getString("currency").trim(),
                        resultSet.getString("status")),
                accountIds.toArray());
        Map<Long, LockedAccount> byId = new HashMap<>();
        for (LockedAccount account : locked) {
            byId.put(account.id(), account);
        }
        return byId;
    }

    private static List<Long> accountIds(Posting posting) {
        List<Long> ids = new ArrayList<>();
        if (posting.sourceAccountId() != null) {
            ids.add(posting.sourceAccountId());
        }
        if (posting.destinationAccountId() != null) {
            ids.add(posting.destinationAccountId());
        }
        return ids.stream().distinct().sorted(Comparator.naturalOrder()).toList();
    }

    private static LockedAccount requireAccount(Map<Long, LockedAccount> accounts, long accountId) {
        LockedAccount account = accounts.get(accountId);
        if (account == null) {
            throw new ResourceNotFoundException("Account not found.");
        }
        return account;
    }

    private static String refusalCode(Posting posting, LockedAccount source, LockedAccount destination) {
        if (posting.transactionType().equals("TRANSFER")
                && Objects.equals(posting.sourceAccountId(), posting.destinationAccountId())) {
            return "SAME_ACCOUNT";
        }
        if (source != null && !source.status().equals("ACTIVE")) {
            return "ACCOUNT_NOT_ACTIVE";
        }
        if (destination != null && !destination.status().equals("ACTIVE")) {
            return "ACCOUNT_NOT_ACTIVE";
        }
        if (source != null && destination != null && !source.currency().equals(destination.currency())) {
            return "CURRENCY_MISMATCH";
        }
        if ((source != null && !source.currency().equals("INR"))
                || (destination != null && !destination.currency().equals("INR"))) {
            return "UNSUPPORTED_CURRENCY";
        }
        if (source != null && source.balancePaise() < posting.amountPaise()) {
            return "INSUFFICIENT_FUNDS";
        }
        try {
            if (destination != null) {
                Math.addExact(destination.balancePaise(), posting.amountPaise());
            }
        } catch (ArithmeticException exception) {
            return "BALANCE_LIMIT_EXCEEDED";
        }
        return null;
    }

    private long insertTransaction(
            Posting posting, boolean accepted, String failureCode, String transactionCurrency) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO transactions (
                        initiating_user_id, amount_paise, transaction_type, source_account_id,
                        destination_account_id, failure_code, external_reference, currency, status, completed_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6))
                    """, new String[] {"id"});
            statement.setLong(1, posting.userId());
            statement.setLong(2, posting.amountPaise());
            statement.setString(3, posting.transactionType());
            setNullableLong(statement, 4, posting.sourceAccountId());
            setNullableLong(statement, 5, posting.destinationAccountId());
            statement.setString(6, failureCode);
            statement.setString(7, posting.externalReference());
            statement.setString(8, transactionCurrency);
            statement.setString(9, accepted ? "COMPLETED" : "FAILED");
            return statement;
        }, keyHolder);
        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("The database did not return the transaction ID.");
        }
        return generatedId.longValue();
    }

    private void applyBalances(Posting posting, Long sourceBalance, Long destinationBalance) {
        if (sourceBalance != null) {
            jdbcTemplate.update(
                    "UPDATE accounts SET balance_paise = ? WHERE id = ?",
                    sourceBalance, posting.sourceAccountId());
        }
        if (destinationBalance != null) {
            jdbcTemplate.update(
                    "UPDATE accounts SET balance_paise = ? WHERE id = ?",
                    destinationBalance, posting.destinationAccountId());
        }
    }

    private void insertLedgerEntries(Posting posting, long transactionId) {
        Long systemAccountId = null;
        if (!posting.transactionType().equals("TRANSFER")) {
            systemAccountId = jdbcTemplate.queryForObject(
                    "SELECT id FROM system_ledger_accounts WHERE code = ?",
                    Long.class,
                    CLEARING_ACCOUNT_CODE);
        }

        switch (posting.transactionType()) {
            case "TRANSFER" -> {
                insertLedgerEntry(transactionId, posting.sourceAccountId(), null, "DEBIT", posting.amountPaise());
                insertLedgerEntry(
                        transactionId, posting.destinationAccountId(), null, "CREDIT", posting.amountPaise());
            }
            case "DEPOSIT" -> {
                insertLedgerEntry(transactionId, null, systemAccountId, "DEBIT", posting.amountPaise());
                insertLedgerEntry(
                        transactionId, posting.destinationAccountId(), null, "CREDIT", posting.amountPaise());
            }
            case "WITHDRAWAL" -> {
                insertLedgerEntry(
                        transactionId, posting.sourceAccountId(), null, "DEBIT", posting.amountPaise());
                insertLedgerEntry(transactionId, null, systemAccountId, "CREDIT", posting.amountPaise());
            }
            default -> throw new IllegalStateException("Unsupported transaction type.");
        }
    }

    private void insertLedgerEntry(
            long transactionId, Long accountId, Long systemAccountId, String entryType, long amountPaise) {
        jdbcTemplate.update("""
                INSERT INTO ledger_entries (
                    transaction_id, account_id, system_account_id, entry_type, amount_paise
                ) VALUES (?, ?, ?, ?, ?)
                """, transactionId, accountId, systemAccountId, entryType, amountPaise);
    }

    private void verifyBalancedPosting(long transactionId, long expectedAmountPaise) {
        Integer entryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ledger_entries WHERE transaction_id = ?",
                Integer.class,
                transactionId);
        BigDecimal debits = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount_paise), 0)
                FROM ledger_entries
                WHERE transaction_id = ? AND entry_type = 'DEBIT'
                """, BigDecimal.class, transactionId);
        BigDecimal credits = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount_paise), 0)
                FROM ledger_entries
                WHERE transaction_id = ? AND entry_type = 'CREDIT'
                """, BigDecimal.class, transactionId);
        BigDecimal expected = BigDecimal.valueOf(expectedAmountPaise);
        if (entryCount == null || entryCount != 2 || debits == null || credits == null
                || debits.compareTo(expected) != 0 || credits.compareTo(expected) != 0) {
            throw new IllegalStateException("The transaction does not have a balanced double-entry posting.");
        }
    }

    private TransactionResponse loadTransaction(long transactionId) {
        return jdbcTemplate.queryForObject("""
                SELECT id, transaction_type, status, amount_paise, currency,
                       source_account_id, destination_account_id, failure_code,
                       external_reference, created_at
                FROM transactions
                WHERE id = ?
                """, (resultSet, rowNumber) -> new TransactionResponse(
                resultSet.getLong("id"),
                resultSet.getString("transaction_type"),
                resultSet.getString("status"),
                Long.toString(resultSet.getLong("amount_paise")),
                resultSet.getString("currency").trim(),
                nullableLong(resultSet, "source_account_id"),
                nullableLong(resultSet, "destination_account_id"),
                resultSet.getString("failure_code"),
                resultSet.getString("external_reference"),
                resultSet.getTimestamp("created_at").toInstant()), transactionId);
    }

    private static Long nullableLong(java.sql.ResultSet resultSet, String column) throws java.sql.SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value)
            throws java.sql.SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static void validateIdempotencyKey(String key) {
        if (key == null || !key.matches("[\\x21-\\x7E]{8,255}")) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must contain 8 to 255 printable ASCII characters without spaces.");
        }
    }

    private static String requestHash(Posting posting) {
        String canonical = String.join("|",
                Long.toString(posting.userId()),
                posting.transactionType(),
                Long.toString(posting.amountPaise()),
                String.valueOf(posting.sourceAccountId()),
                String.valueOf(posting.destinationAccountId()),
                String.valueOf(posting.externalReference()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private static void requireRole(Authentication authentication, String expectedRole) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream()
                        .noneMatch(authority -> authority.getAuthority().equals("ROLE_" + expectedRole))) {
            throw new AccessDeniedException("The required role is not available.");
        }
    }

    private record Posting(
            long userId,
            String transactionType,
            long amountPaise,
            Long sourceAccountId,
            Long destinationAccountId,
            String externalReference) {
    }

    private record LockedAccount(long id, long balancePaise, String currency, String status) {
    }

    private record ExistingIdempotency(String requestHash, Long transactionId) {
    }
}