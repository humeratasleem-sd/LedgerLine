package com.ledgerline.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountStatus;
import com.ledgerline.enums.Role;
import com.ledgerline.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import org.mockito.Mockito;

class SecurityConfigTest {

    @Test
    void authenticatedRoleIsLoadedFromTheUserRecordRatherThanTrustedFromJwt() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("123")
                .claim("roles", List.of("ADMIN"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .build();

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        User user = new User("Test User", "user@example.com", "hash", Role.AUDITOR, AccountStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", 123L);
        when(userRepository.findById(123L)).thenReturn(Optional.of(user));
        var authentication = new SecurityConfig().jwtAuthenticationConverter(userRepository).convert(jwt);

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());
        Set<String> roleAuthorities = authorities.stream()
                .filter(authority -> authority.startsWith("ROLE_"))
                .collect(Collectors.toSet());
        assertEquals(Set.of("ROLE_AUDITOR"), roleAuthorities);
    }

    @Test
    void disabledUserCannotAuthenticateWithPreviouslyIssuedJwt() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("123")
                .claim("roles", List.of("ACCOUNT_HOLDER"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .build();
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        User user = new User("Test User", "user@example.com", "hash", Role.ACCOUNT_HOLDER, AccountStatus.DISABLED);
        ReflectionTestUtils.setField(user, "id", 123L);
        when(userRepository.findById(123L)).thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class,
                () -> new SecurityConfig().jwtAuthenticationConverter(userRepository).convert(jwt));
    }
}
