package com.ledgerline.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTest {

    @Test
    void jwtRoleNamesBecomeSpringRoleAuthorities() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("user-123")
                .claim("roles", List.of("ADMIN", "AUDITOR"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .build();

        var authentication = new SecurityConfig().jwtAuthenticationConverter().convert(jwt);

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());
        Set<String> roleAuthorities = authorities.stream()
                .filter(authority -> authority.startsWith("ROLE_"))
                .collect(Collectors.toSet());
        assertEquals(Set.of("ROLE_ADMIN", "ROLE_AUDITOR"), roleAuthorities);
    }
}
