package com.ledgerline.dto;

import com.ledgerline.enums.AccountPermission;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AccountPermissionRequest(
        @NotNull @Positive Long userId,
        @NotNull AccountPermission permission) {
}
