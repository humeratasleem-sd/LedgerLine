package com.ledgerline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ledgerline.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();

    @Test
    void bcryptHashMatchesOnlyTheOriginalPassword() {
        String password = "example-test-password";
        String hash = passwordEncoder.encode(password);

        assertNotEquals(password, hash);
        assertTrue(passwordEncoder.matches(password, hash));
        assertFalse(passwordEncoder.matches("different-password", hash));
    }
}
