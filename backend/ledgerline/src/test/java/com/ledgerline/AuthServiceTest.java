package com.ledgerline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.ledgerline.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AuthServiceTest {

    private final AuthService authService = new AuthService();

    @Test
    void currentUserReturnsSubjectAndSortedAuthorities() {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "user-123",
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_AUDITOR"),
                        new SimpleGrantedAuthority("ROLE_ACCOUNT_HOLDER")));

        var response = authService.currentUser(authentication);

        assertEquals("user-123", response.subject());
        assertEquals(List.of("ROLE_ACCOUNT_HOLDER", "ROLE_AUDITOR"), response.roles());
    }
}
