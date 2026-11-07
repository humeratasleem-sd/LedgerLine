package com.ledgerline.service;

import java.util.List;

import com.ledgerline.enums.AccountPermission;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AccountAuthorizationService {

    private static final String ACCOUNT_PERMISSION_QUERY = """
            SELECT COUNT(*)
            FROM account_permissions
            WHERE account_id = ?
              AND user_id = ?
              AND permission IN (%s)
            """;

    private final JdbcTemplate jdbcTemplate;

    public AccountAuthorizationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean canViewAccount(Authentication authentication, long accountId) {
        return hasPermission(authentication, accountId, List.of(
                AccountPermission.OWNER, AccountPermission.VIEW, AccountPermission.TRANSFER));
    }

    public boolean canTransferFromAccount(Authentication authentication, long accountId) {
        return hasPermission(authentication, accountId, List.of(
                AccountPermission.OWNER, AccountPermission.TRANSFER));
    }

    public void requireViewAccount(Authentication authentication, long accountId) {
        if (!canViewAccount(authentication, accountId)) {
            throw new AccessDeniedException("Account access denied.");
        }
    }

    public void requireTransferPermission(Authentication authentication, long accountId) {
        if (!canTransferFromAccount(authentication, accountId)) {
            throw new AccessDeniedException("Account access denied.");
        }
    }

    public long authenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required.");
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException("Account access denied.");
        }
    }

    private boolean hasPermission(
            Authentication authentication, long accountId, List<AccountPermission> permissions) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        long userId;
        try {
            userId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException exception) {
            return false;
        }

        String placeholders = String.join(", ", java.util.Collections.nCopies(permissions.size(), "?"));
        Integer matches = jdbcTemplate.queryForObject(
                ACCOUNT_PERMISSION_QUERY.formatted(placeholders),
                Integer.class,
                buildParameters(accountId, userId, permissions));
        return matches != null && matches > 0;
    }

    private static Object[] buildParameters(
            long accountId, long userId, List<AccountPermission> permissions) {
        Object[] parameters = new Object[permissions.size() + 2];
        parameters[0] = accountId;
        parameters[1] = userId;
        for (int index = 0; index < permissions.size(); index++) {
            parameters[index + 2] = permissions.get(index).name();
        }
        return parameters;
    }
}
