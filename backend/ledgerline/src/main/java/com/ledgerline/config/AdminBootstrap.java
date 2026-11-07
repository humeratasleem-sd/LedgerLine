package com.ledgerline.config;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.ledgerline.dto.RegisterRequest;
import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountStatus;
import com.ledgerline.enums.Role;
import com.ledgerline.repository.UserRepository;
import jakarta.validation.Validator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "ledgerline.admin.bootstrap.enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final Validator validator;
    private final String fullName;
    private final String email;
    private final String password;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate,
            Validator validator,
            @Value("${ledgerline.admin.bootstrap.full-name:}") String fullName,
            @Value("${ledgerline.admin.bootstrap.email:}") String email,
            @Value("${ledgerline.admin.bootstrap.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.validator = validator;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        RegisterRequest bootstrapRequest = new RegisterRequest(fullName, email, password);
        if (!validator.validate(bootstrapRequest).isEmpty()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "Administrator bootstrap is enabled, but its name, email, or password is invalid.");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalStateException(
                    "Administrator bootstrap email belongs to an existing non-administrator account.");
        }

        User admin = userRepository.saveAndFlush(new User(
                fullName.trim(),
                normalizedEmail,
                passwordEncoder.encode(password),
                Role.ADMIN,
                AccountStatus.ACTIVE));
        jdbcTemplate.update("""
                INSERT INTO audit_events (actor_user_id, action, entity_type, entity_id)
                VALUES (NULL, 'ADMIN_BOOTSTRAPPED', 'USER', ?)
                """, admin.getId().toString());
    }
}
