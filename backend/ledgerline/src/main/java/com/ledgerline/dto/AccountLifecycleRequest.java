package com.ledgerline.dto;

import com.ledgerline.enums.AccountLifecycleStatus;
import jakarta.validation.constraints.NotNull;

public record AccountLifecycleRequest(@NotNull AccountLifecycleStatus status) {
}