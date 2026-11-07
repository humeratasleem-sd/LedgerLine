package com.ledgerline.dto;

import com.ledgerline.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UserRoleRequest(@NotNull Role role) {
}
