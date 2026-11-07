package com.ledgerline.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtConfigTest {

    private static final String ISSUER = "ledgerline";
    private static final byte[] KEY_BYTES = new byte[32];

    @Test
    void decoderAcceptsExpectedIssuerAndRequiredClaims() {
        JwtDecoder decoder = decoder();
        Instant now = Instant.now();
        String token = encode(claims(now, now.plusSeconds(60), List.of("ACCOUNT_HOLDER")));

        var decoded = decoder.decode(token);

        assertEquals("user-123", decoded.getSubject());
        assertEquals(ISSUER, decoded.getClaims().get("iss"));
    }

    @Test
    void decoderRejectsMissingExpiryAndUnknownRole() {
        JwtDecoder decoder = decoder();
        Instant now = Instant.now();

        assertThrows(JwtException.class,
                () -> decoder.decode(encode(claims(now, null, List.of("ACCOUNT_HOLDER")))));
        assertThrows(JwtException.class,
                () -> decoder.decode(encode(claims(now, now.plusSeconds(60), List.of("SUPERUSER")))));
    }

    @Test
    void decoderRejectsExpiredAndTamperedTokens() {
        JwtDecoder decoder = decoder();
        Instant now = Instant.now();
        String expired = encode(claims(now.minusSeconds(120), now.minusSeconds(60), List.of("ACCOUNT_HOLDER")));
        String valid = encode(claims(now, now.plusSeconds(60), List.of("ACCOUNT_HOLDER")));
        int signatureStart = valid.lastIndexOf('.') + 1;
        char replacement = valid.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = valid.substring(0, signatureStart) + replacement + valid.substring(signatureStart + 1);

        assertThrows(JwtException.class, () -> decoder.decode(expired));
        assertThrows(JwtException.class, () -> decoder.decode(tampered));
    }

    @Test
    void decoderRejectsSecretsShorterThanThirtyTwoBytes() {
        String shortSecret = Base64.getEncoder().encodeToString(new byte[31]);

        assertThrows(IllegalStateException.class,
                () -> new JwtConfig().jwtDecoder(shortSecret, ISSUER));
    }

    private static JwtDecoder decoder() {
        String secret = Base64.getEncoder().encodeToString(KEY_BYTES);
        return new JwtConfig().jwtDecoder(secret, ISSUER);
    }

    private static JwtClaimsSet claims(Instant issuedAt, Instant expiresAt, List<String> roles) {
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject("user-123")
                .claim("roles", roles)
                .issuedAt(issuedAt);
        if (expiresAt != null) {
            builder.expiresAt(expiresAt);
        }
        return builder.build();
    }

    private static String encode(JwtClaimsSet claims) {
        SecretKey secretKey = new SecretKeySpec(KEY_BYTES, "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
