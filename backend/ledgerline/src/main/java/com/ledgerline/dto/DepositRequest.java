package com.ledgerline.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DepositRequest(
        @NotNull @Positive Long destinationAccountId,
        @NotNull @Positive Long amountPaise,
        @NotBlank @Size(max = 128) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:/-]{0,127}")
                String externalReference,
        @AssertTrue boolean externalSettlementVerified) {
}