package com.ledgerline.dto;

import java.time.Instant;

import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountStatus;
import com.ledgerline.enums.Role;

public record AuthenticatedUserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        AccountStatus accountStatus,
        Instant createdAt) {

    public static AuthenticatedUserResponse from(User user) {
        return new AuthenticatedUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getAccountStatus(),
                user.getCreatedAt());
    }
}
