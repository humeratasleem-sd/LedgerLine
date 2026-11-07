package com.ledgerline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import com.ledgerline.dto.LoginRequest;
import com.ledgerline.dto.RegisterRequest;
import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountStatus;
import com.ledgerline.enums.Role;
import com.ledgerline.exception.AuthenticationFailedException;
import com.ledgerline.exception.ResourceConflictException;
import com.ledgerline.repository.UserRepository;
import com.ledgerline.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtEncoder jwtEncoder;

    private BCryptPasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(
                userRepository, passwordEncoder, jwtEncoder, "ledgerline", Duration.ofMinutes(15));
    }

    private void stubJwtEncoding() {
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenAnswer(invocation -> {
            JwtEncoderParameters parameters = invocation.getArgument(0);
            return Jwt.withTokenValue("signed-test-token")
                    .header("alg", "HS256")
                    .claims(claims -> claims.putAll(parameters.getClaims().getClaims()))
                    .build();
        });
    }

    @Test
    void registrationCreatesAccountHolderAndStoresOnlyBcryptHash() {
        stubJwtEncoding();
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 41L);
            return saved;
        });

        var response = authService.register(new RegisterRequest(
                "  Example Person ", " Person@Example.com ", "correct horse battery"));

        var saved = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertEquals("Example Person", saved.getValue().getFullName());
        assertEquals("person@example.com", saved.getValue().getEmail());
        assertEquals(Role.ACCOUNT_HOLDER, saved.getValue().getRole());
        assertEquals(AccountStatus.ACTIVE, saved.getValue().getAccountStatus());
        assertTrue(passwordEncoder.matches("correct horse battery", saved.getValue().getPasswordHash()));
        assertFalse(saved.getValue().getPasswordHash().contains("correct horse battery"));
        assertEquals("signed-test-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(Role.ACCOUNT_HOLDER, response.user().role());
        assertFalse(response.toString().contains(saved.getValue().getPasswordHash()));
    }

    @Test
    void duplicateEmailRegistrationIsRejected() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);

        assertThrows(ResourceConflictException.class,
                () -> authService.register(new RegisterRequest(
                        "Example Person", "person@example.com", "correct horse battery")));
    }

    @Test
    void successfulLoginIssuesJwtForActiveUser() {
        stubJwtEncoding();
        User user = user(41L, "person@example.com", "correct horse battery", Role.ACCOUNT_HOLDER);
        when(userRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));

        var response = authService.login(new LoginRequest("PERSON@example.com", "correct horse battery"));

        assertEquals("signed-test-token", response.accessToken());
        assertEquals(Role.ACCOUNT_HOLDER, response.user().role());
    }

    @Test
    void incorrectPasswordAndDisabledUsersCannotLogin() {
        User activeUser = user(41L, "person@example.com", "correct horse battery", Role.ACCOUNT_HOLDER);
        when(userRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(activeUser));

        assertThrows(AuthenticationFailedException.class,
                () -> authService.login(new LoginRequest("person@example.com", "incorrect password")));

        User disabledUser = new User(
                "Example Person",
                "person@example.com",
                passwordEncoder.encode("correct horse battery"),
                Role.ACCOUNT_HOLDER,
                AccountStatus.DISABLED);
        when(userRepository.findByEmailIgnoreCase("disabled@example.com")).thenReturn(Optional.of(disabledUser));
        assertThrows(AuthenticationFailedException.class,
                () -> authService.login(new LoginRequest("disabled@example.com", "correct horse battery")));
    }

    @Test
    void currentUserResolvesSubjectFromDatabaseAndDoesNotAcceptInvalidId() {
        User user = user(41L, "person@example.com", "correct horse battery", Role.AUDITOR);
        when(userRepository.findById(41L)).thenReturn(Optional.of(user));

        var response = authService.currentUser("41");

        assertEquals("person@example.com", response.email());
        assertEquals(Role.AUDITOR, response.role());
        assertThrows(AuthenticationFailedException.class, () -> authService.currentUser("email@example.com"));
    }

    @Test
    void registrationRejectsPasswordsBeyondBcryptByteLimit() {
        String multibytePassword = "é".repeat(37);

        assertThrows(IllegalArgumentException.class, () -> authService.register(
                new RegisterRequest("Example Person", "person@example.com", multibytePassword)));
    }

    private User user(long id, String email, String rawPassword, Role role) {
        User user = new User(
                "Example Person", email, passwordEncoder.encode(rawPassword), role, AccountStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        return user;
    }
}
