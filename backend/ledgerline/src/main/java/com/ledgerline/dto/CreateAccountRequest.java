package com.ledgerline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 34) @Pattern(regexp = "[A-Za-z0-9-]+") String accountNumber,
        @NotBlank @Pattern(regexp = "INR") String currency,
        @NotNull @Positive Long ownerUserId) {
}
