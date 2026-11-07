package com.ledgerline.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountStatus;
import com.ledgerline.enums.Role;
import com.ledgerline.repository.UserRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.Mockito;

class AdminBootstrapTest {

    @Test
    void bootstrapCreatesBcryptHashedAdminOnlyWhenNoAdminExists() throws Exception {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            AdminBootstrap bootstrap = new AdminBootstrap(
                    userRepository,
                    passwordEncoder,
                    jdbcTemplate,
                    validatorFactory.getValidator(),
                    "Initial Admin",
                    "Admin@Example.com",
                    "a-strong-bootstrap-password");

            bootstrap.run(new DefaultApplicationArguments(new String[0]));

            var userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
            verify(userRepository).saveAndFlush(userCaptor.capture());
            assertEquals(Role.ADMIN, userCaptor.getValue().getRole());
            assertEquals(AccountStatus.ACTIVE, userCaptor.getValue().getAccountStatus());
            assertTrue(passwordEncoder.matches(
                    "a-strong-bootstrap-password", userCaptor.getValue().getPasswordHash()));
            verify(jdbcTemplate).update(
                    org.mockito.ArgumentMatchers.contains("ADMIN_BOOTSTRAPPED"), eq("1"));
        }
    }

    @Test
    void bootstrapDoesNotCreateAnotherAdminWhenAnAdminAlreadyExists() throws Exception {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            AdminBootstrap bootstrap = new AdminBootstrap(
                    userRepository,
                    new BCryptPasswordEncoder(),
                    Mockito.mock(JdbcTemplate.class),
                    validatorFactory.getValidator(),
                    "",
                    "",
                    "");

            bootstrap.run(new DefaultApplicationArguments(new String[0]));
        }

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userRepository, never()).existsByEmailIgnoreCase(any());
    }

    @Test
    void enabledBootstrapRejectsInvalidConfigurationRatherThanSilentlySkipping() throws Exception {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            AdminBootstrap bootstrap = new AdminBootstrap(
                    userRepository,
                    new BCryptPasswordEncoder(),
                    Mockito.mock(JdbcTemplate.class),
                    validatorFactory.getValidator(),
                    "",
                    "not-an-email",
                    "short");

            assertThrows(IllegalStateException.class,
                    () -> bootstrap.run(new DefaultApplicationArguments(new String[0])));
        }
    }
}
