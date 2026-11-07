package com.ledgerline.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.ledgerline.dto.AccountResponse;
import com.ledgerline.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private static final String ACCOUNT_FIELDS = """
            SELECT a.id, a.account_number, a.currency, a.balance_paise, a.status, a.created_at
            FROM accounts a
            """;

    private static final RowMapper<AccountResponse> ACCOUNT_ROW_MAPPER =
            (resultSet, rowNumber) -> mapAccount(resultSet);

    private final JdbcTemplate jdbcTemplate;
    private final AccountAuthorizationService accountAuthorizationService;

    public AccountService(
            JdbcTemplate jdbcTemplate,
            AccountAuthorizationService accountAuthorizationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.accountAuthorizationService = accountAuthorizationService;
    }

    public List<AccountResponse> listAuthorizedAccounts(Authentication authentication) {
        long userId = accountAuthorizationService.authenticatedUserId(authentication);
        return jdbcTemplate.query(
                ACCOUNT_FIELDS + """
                        JOIN account_permissions ap ON ap.account_id = a.id
                        WHERE ap.user_id = ?
                        ORDER BY a.created_at DESC, a.id DESC
                        """,
                ACCOUNT_ROW_MAPPER,
                userId);
    }

    public AccountResponse getAuthorizedAccount(Authentication authentication, long accountId) {
        accountAuthorizationService.requireViewAccount(authentication, accountId);
        return jdbcTemplate.query(
                        ACCOUNT_FIELDS + "WHERE a.id = ?",
                        ACCOUNT_ROW_MAPPER,
                        accountId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
    }

    private static AccountResponse mapAccount(ResultSet resultSet) throws SQLException {
        return new AccountResponse(
                resultSet.getLong("id"),
                resultSet.getString("account_number"),
                resultSet.getString("currency"),
                Long.toString(resultSet.getLong("balance_paise")),
                resultSet.getString("status"),
                resultSet.getTimestamp("created_at").toInstant());
    }
}
