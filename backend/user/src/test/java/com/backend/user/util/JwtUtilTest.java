package com.backend.user.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

class JwtUtilTest {

    private static final String TEST_SECRET =
            Base64.getEncoder().encodeToString("this-is-a-32-byte-test-secret!!".getBytes());

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        try (MockedStatic<SecretReader> secretReader = mockStatic(SecretReader.class)) {
            secretReader.when(() -> SecretReader.readSecret("JWT_SECRET_FILE", "JWT_SECRET"))
                    .thenReturn(TEST_SECRET);
            jwtUtil = new JwtUtil();
        }
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600_000L);
    }

    @Test
    void generateToken_thenValidate_roundTripsUserIdAndEmail() {
        String token = jwtUtil.generateToken("user-123", "user@example.com");

        assertThat(token).isNotBlank();

        Claims claims = jwtUtil.validateAndGetClaims(token);

        assertThat(claims.getSubject()).isEqualTo("user-123");
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void generateToken_expirationReflectsConfiguredExpirationMs() {
        String token = jwtUtil.generateToken("user-123", "user@example.com");

        Claims claims = jwtUtil.validateAndGetClaims(token);

        long deltaMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(deltaMs).isEqualTo(3600_000L);
    }

    @Test
    void validateAndGetClaims_expiredToken_throwsExpiredJwtException() {
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -1000L);
        String token = jwtUtil.generateToken("user-123", "user@example.com");

        assertThatThrownBy(() -> jwtUtil.validateAndGetClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void validateAndGetClaims_tokenSignedWithDifferentSecret_throwsSignatureException() {
        JwtUtil otherJwtUtil;
        String differentSecret =
                Base64.getEncoder().encodeToString("a-completely-different-secret!!".getBytes());
        try (MockedStatic<SecretReader> secretReader = mockStatic(SecretReader.class)) {
            secretReader.when(() -> SecretReader.readSecret("JWT_SECRET_FILE", "JWT_SECRET"))
                    .thenReturn(differentSecret);
            otherJwtUtil = new JwtUtil();
        }
        ReflectionTestUtils.setField(otherJwtUtil, "expirationMs", 3600_000L);

        String tokenFromOtherSecret = otherJwtUtil.generateToken("user-123", "user@example.com");

        assertThatThrownBy(() -> jwtUtil.validateAndGetClaims(tokenFromOtherSecret))
                .isInstanceOf(SignatureException.class);
    }
}
