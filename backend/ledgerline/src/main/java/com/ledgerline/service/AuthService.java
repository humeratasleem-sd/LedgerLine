package com.ledgerline.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.ledgerline.dto.AuthenticatedUserResponse;
import com.ledgerline.dto.AuthenticationResponse;
import com.ledgerline.dto.LoginRequest;
import com.ledgerline.dto.RegisterRequest;
import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountStatus;
import com.ledgerline.enums.Role;
import com.ledgerline.exception.AuthenticationFailedException;
import com.ledgerline.exception.ResourceConflictException;
import com.ledgerline.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration accessTokenTtl;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${ledgerline.security.jwt.issuer}") String issuer,
            @Value("${ledgerline.security.jwt.access-token-ttl:PT15M}") Duration accessTokenTtl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
        if (accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException("JWT access-token TTL must be positive.");
        }
    }

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String fullName = request.fullName().trim();
        validateBcryptPasswordLength(request.password());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResourceConflictException("An account with this email already exists.");
        }

        User user = new User(
                fullName,
                email,
                passwordEncoder.encode(request.password()),
                Role.ACCOUNT_HOLDER,
                AccountStatus.ACTIVE);
        try {
            return issueToken(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            if (userRepository.existsByEmailIgnoreCase(email)) {
                throw new ResourceConflictException("An account with this email already exists.");
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new AuthenticationFailedException();
        }
        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(candidate -> candidate.getAccountStatus() == AccountStatus.ACTIVE)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(AuthenticationFailedException::new);
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthenticatedUserResponse currentUser(String subject) {
        long userId = parseUserId(subject);
        User user = userRepository.findById(userId)
                .filter(candidate -> candidate.getAccountStatus() == AccountStatus.ACTIVE)
                .orElseThrow(AuthenticationFailedException::new);
        return AuthenticatedUserResponse.from(user);
    }

    private AuthenticationResponse issueToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(accessTokenTtl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", List.of(user.getRole().name()))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthenticationResponse(
                token,
                "Bearer",
                expiresAt,
                AuthenticatedUserResponse.from(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static long parseUserId(String subject) {
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            throw new AuthenticationFailedException();
        }
    }

    private static void validateBcryptPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes.");
        }
    }
}
