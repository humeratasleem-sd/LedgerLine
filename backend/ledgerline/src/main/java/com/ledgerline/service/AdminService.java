package com.ledgerline.service;

import java.time.Instant;
import java.util.List;

import com.ledgerline.dto.AccountPermissionRequest;
import com.ledgerline.dto.AccountLifecycleRequest;
import com.ledgerline.dto.AccountStatusRequest;
import com.ledgerline.dto.AccountResponse;
import com.ledgerline.dto.AuthenticatedUserResponse;
import com.ledgerline.dto.CreateAccountRequest;
import com.ledgerline.dto.UserRoleRequest;
import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountStatus;
import com.ledgerline.enums.AccountLifecycleStatus;
import com.ledgerline.enums.Role;
import com.ledgerline.exception.ResourceConflictException;
import com.ledgerline.exception.ResourceNotFoundException;
import com.ledgerline.repository.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final AccountAuthorizationService accountAuthorizationService;
    private final AccountService accountService;

    public AdminService(
            JdbcTemplate jdbcTemplate,
            UserRepository userRepository,
            AccountAuthorizationService accountAuthorizationService,
            AccountService accountService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.accountAuthorizationService = accountAuthorizationService;
        this.accountService = accountService;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAccounts() {
        return accountService.listAllAccounts();
    }

    @Transactional(readOnly = true)
    public List<AuthenticatedUserResponse> listUsers() {
        return jdbcTemplate.query("""
                SELECT id, full_name, email, role, account_status, created_at
                FROM users
                ORDER BY id
                """, (resultSet, rowNumber) -> new AuthenticatedUserResponse(
                resultSet.getLong("id"),
                resultSet.getString("full_name"),
                resultSet.getString("email"),
                Role.valueOf(resultSet.getString("role")),
                AccountStatus.valueOf(resultSet.getString("account_status")),
                resultSet.getTimestamp("created_at").toInstant()));
    }

    @Transactional
    public AccountResponse createAccount(Authentication actor, CreateAccountRequest request) {
        long actorId = accountAuthorizationService.authenticatedUserId(actor);
        ensureActiveUser(request.ownerUserId());

        Integer duplicate = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE account_number = ?",
                Integer.class,
                request.accountNumber());
        if (duplicate != null && duplicate > 0) {
            throw new ResourceConflictException("An account with this account number already exists.");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                var statement = connection.prepareStatement("""
                        INSERT INTO accounts (account_number, currency, balance_paise, status)
                        VALUES (?, ?, 0, 'ACTIVE')
                        """, new String[] {"id"});
                statement.setString(1, request.accountNumber());
                statement.setString(2, request.currency());
                return statement;
            }, keyHolder);
        } catch (DuplicateKeyException exception) {
            throw new ResourceConflictException("An account with this account number already exists.");
        }
        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("The database did not return the new account ID.");
        }

        long accountId = generatedId.longValue();
        jdbcTemplate.update("""
                INSERT INTO account_permissions (account_id, user_id, permission)
                VALUES (?, ?, 'OWNER')
                """, accountId, request.ownerUserId());
        recordAdminAction(actorId, "ACCOUNT_CREATED", "ACCOUNT", Long.toString(accountId));
        Instant createdAt = jdbcTemplate.queryForObject(
                "SELECT created_at FROM accounts WHERE id = ?",
                java.sql.Timestamp.class,
                accountId).toInstant();
        return new AccountResponse(
                accountId, request.accountNumber(), request.currency(), "0", "ACTIVE", createdAt, "OWNER");
    }

    @Transactional
    public void updateAccountLifecycle(
            Authentication actor, long accountId, AccountLifecycleRequest request) {
        long actorId = accountAuthorizationService.authenticatedUserId(actor);
        var current = jdbcTemplate.query("""
                SELECT status, balance_paise
                FROM accounts
                WHERE id = ?
                FOR UPDATE
                """, resultSet -> resultSet.next()
                ? new AccountLifecycleState(
                        resultSet.getString("status"), resultSet.getLong("balance_paise"))
                : null, accountId);
        if (current == null) {
            throw new ResourceNotFoundException("Account not found.");
        }
        String requestedStatus = request.status().name();
        if (current.status().equals(requestedStatus)) {
            return;
        }
        if (current.status().equals(AccountLifecycleStatus.CLOSED.name())) {
            throw new ResourceConflictException("A closed account cannot be reopened or changed.");
        }
        if (request.status() == AccountLifecycleStatus.CLOSED && current.balancePaise() != 0) {
            throw new ResourceConflictException("An account must have a zero balance before it can be closed.");
        }
        jdbcTemplate.update("UPDATE accounts SET status = ? WHERE id = ?", requestedStatus, accountId);
        recordAdminAction(actorId, "ACCOUNT_STATUS_CHANGED", "ACCOUNT", Long.toString(accountId));
    }

    @Transactional
    public void assignAccountPermission(
            Authentication actor, long accountId, AccountPermissionRequest request) {
        long actorId = accountAuthorizationService.authenticatedUserId(actor);
        ensureActiveUser(request.userId());
        lockAccount(accountId);

        int updated = jdbcTemplate.update("""
                UPDATE account_permissions
                SET permission = ?
                WHERE account_id = ? AND user_id = ?
                """, request.permission().name(), accountId, request.userId());
        if (updated == 0 && !permissionExists(accountId, request.userId())) {
            jdbcTemplate.update("""
                    INSERT INTO account_permissions (account_id, user_id, permission)
                    VALUES (?, ?, ?)
                    """, accountId, request.userId(), request.permission().name());
        }
        recordAdminAction(actorId, "ACCOUNT_PERMISSION_CHANGED", "ACCOUNT", Long.toString(accountId));
    }

    @Transactional
    public void updateUserStatus(Authentication actor, long userId, AccountStatusRequest request) {
        long actorId = accountAuthorizationService.authenticatedUserId(actor);
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (target.getAccountStatus() == request.accountStatus()) {
            return;
        }
        if (actorId == userId && request.accountStatus() == AccountStatus.DISABLED) {
            throw new ResourceConflictException("You cannot disable your own administrator account.");
        }
        if (target.getRole() == Role.ADMIN && request.accountStatus() == AccountStatus.DISABLED) {
            List<Long> activeAdminIds = jdbcTemplate.queryForList(
                    "SELECT id FROM users WHERE role = 'ADMIN' AND account_status = 'ACTIVE' FOR UPDATE",
                    Long.class);
            if (activeAdminIds.size() <= 1) {
                throw new ResourceConflictException("The last active administrator cannot be disabled.");
            }
        }

        jdbcTemplate.update(
                "UPDATE users SET account_status = ?, active = ? WHERE id = ?",
                request.accountStatus().name(),
                request.accountStatus() == AccountStatus.ACTIVE,
                userId);
        recordAdminAction(actorId, "USER_STATUS_CHANGED", "USER", Long.toString(userId));
    }

    @Transactional
    public void updateUserRole(Authentication actor, long userId, UserRoleRequest request) {
        long actorId = accountAuthorizationService.authenticatedUserId(actor);
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (target.getRole() == request.role()) {
            return;
        }
        if (actorId == userId && target.getAccountStatus() == AccountStatus.ACTIVE
                && request.role() != Role.ADMIN) {
            throw new ResourceConflictException("You cannot remove your own active administrator role.");
        }
        if (target.getRole() == Role.ADMIN
                && target.getAccountStatus() == AccountStatus.ACTIVE
                && request.role() != Role.ADMIN) {
            List<Long> activeAdminIds = jdbcTemplate.queryForList(
                    "SELECT id FROM users WHERE role = 'ADMIN' AND account_status = 'ACTIVE' FOR UPDATE",
                    Long.class);
            if (activeAdminIds.size() <= 1) {
                throw new ResourceConflictException("The last active administrator cannot be demoted.");
            }
        }

        jdbcTemplate.update("UPDATE users SET role = ? WHERE id = ?", request.role().name(), userId);
        recordAdminAction(actorId, "USER_ROLE_CHANGED", "USER", Long.toString(userId));
    }

    private void ensureActiveUser(long userId) {
        userRepository.findById(userId)
                .filter(user -> user.getAccountStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active user not found."));
    }

    private void lockAccount(long accountId) {
        Long lockedId = jdbcTemplate.query("""
                SELECT id
                FROM accounts
                WHERE id = ?
                FOR UPDATE
                """, resultSet -> resultSet.next() ? resultSet.getLong("id") : null, accountId);
        if (lockedId == null) {
            throw new ResourceNotFoundException("Account not found.");
        }

    }

    private boolean permissionExists(long accountId, long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM account_permissions
                WHERE account_id = ? AND user_id = ?
                """, Integer.class, accountId, userId);
        return count != null && count > 0;
    }

    private void recordAdminAction(long actorId, String action, String entityType, String entityId) {
        jdbcTemplate.update("""
                INSERT INTO audit_events (actor_user_id, action, entity_type, entity_id)
                VALUES (?, ?, ?, ?)
                """, actorId, action, entityType, entityId);
    }

    private record AccountLifecycleState(String status, long balancePaise) {
    }
}
