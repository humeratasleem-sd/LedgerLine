package com.ledgerline.dto;

import java.util.List;

public record CurrentUserResponse(String subject, List<String> roles) {
    public CurrentUserResponse {
        roles = List.copyOf(roles);
    }
}
