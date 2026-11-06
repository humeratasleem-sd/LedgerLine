package com.ledgerline.service;

import java.util.List;

import com.ledgerline.dto.CurrentUserResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public CurrentUserResponse currentUser(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .sorted()
                .toList();
        return new CurrentUserResponse(authentication.getName(), roles);
    }
}
