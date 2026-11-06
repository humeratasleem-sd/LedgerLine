package com.ledgerline.config;

import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.ledgerline.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtConfig {

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${ledgerline.security.jwt.secret-base64}") String secretBase64,
            @Value("${ledgerline.security.jwt.issuer}") String issuer) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secretBase64);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("LEDGERLINE_JWT_SECRET_BASE64 must be valid Base64.", exception);
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException("LEDGERLINE_JWT_SECRET_BASE64 must decode to at least 32 bytes.");
        }

        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> requiredClaimsValidator = jwt -> {
            Object rolesClaim = jwt.getClaims().get("roles");
            boolean hasValidRoles = rolesClaim instanceof List<?> roles
                    && !roles.isEmpty()
                    && roles.stream().allMatch(role -> role instanceof String roleName
                            && isKnownRole(roleName));

            if (jwt.getSubject() == null || jwt.getSubject().isBlank()
                    || jwt.getExpiresAt() == null || !hasValidRoles) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Required JWT claims are missing or invalid.", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), requiredClaimsValidator));
        return decoder;
    }

    private boolean isKnownRole(String role) {
        for (Role knownRole : Role.values()) {
            if (knownRole.name().equals(role)) {
                return true;
            }
        }
        return false;
    }
}
