package com.ledgerline.dto;

import com.ledgerline.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record AccountStatusRequest(@NotNull AccountStatus accountStatus) {
}
