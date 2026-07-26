package com.backend.sensor_data.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

/**
 * Covers JwtUtil's constructor (secret loading via SecretReader) and
 * validateAndGetClaims()'s behavior across valid, expired, malformed, and
 * wrong-signature tokens.
 *
 * The constructor reads JWT_SECRET_FILE / JWT_SECRET via SecretReader, which
 * reads real environment variables -- so this uses system-stubs-jupiter
 * (already added for SecretReaderTest) to control that value per test.
 *
 * NOTE: JwtUtil.key is built from `secret.getBytes()` directly (not base64-
 * decoded), so the env var value here is used as the literal UTF-8 secret
 * bytes -- it just needs to be >= 32 bytes for HS256's minimum key length.
 */
@ExtendWith(SystemStubsExtension.class)
class JwtUtilTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    private static final String TEST_SECRET = "this-is-a-32-byte-plus-test-secret-value!!";
    private static final String OTHER_SECRET = "a-completely-different-32-byte-plus-secret!";

    private JwtUtil jwtUtil;
    private SecretKey testKey;

    @BeforeEach
    void setUp() {
        environmentVariables.set("JWT_SECRET_FILE", null);
        environmentVariables.set("JWT_SECRET", TEST_SECRET);

        jwtUtil = new JwtUtil();
        testKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
    }

    private String buildToken(SecretKey signingKey, Date expiration, String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(Instant.now()))
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    // ---------------- valid token ----------------

    @Test
    void validateAndGetClaims_validToken_returnsClaimsWithCorrectSubject() {
        String token = buildToken(testKey, Date.from(Instant.now().plusSeconds(3600)), "user-123");

        Claims claims = jwtUtil.validateAndGetClaims(token);

        assertThat(claims.getSubject()).isEqualTo("user-123");
    }

    // ---------------- expired token ----------------

    @Test
    void validateAndGetClaims_expiredToken_throwsExpiredJwtException() {
        String expiredToken = buildToken(testKey, Date.from(Instant.now().minusSeconds(3600)), "user-123");

        assertThatThrownBy(() -> jwtUtil.validateAndGetClaims(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    // ---------------- malformed token ----------------

    @Test
    void validateAndGetClaims_malformedToken_throwsJwtException() {
        assertThatThrownBy(() -> jwtUtil.validateAndGetClaims("not-a-valid-jwt-at-all"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateAndGetClaims_emptyToken_throwsIllegalArgumentException() {
        // jjwt's parser runs an Assert.hasText() precondition before any JWT-specific
        // parsing, so an empty string fails here rather than as a JwtException.
        assertThatThrownBy(() -> jwtUtil.validateAndGetClaims(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------- wrong signing key ----------------

    @Test
    void validateAndGetClaims_tokenSignedWithDifferentKey_throwsSignatureException() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(OTHER_SECRET.getBytes());
        String token = buildToken(wrongKey, Date.from(Instant.now().plusSeconds(3600)), "user-123");

        assertThatThrownBy(() -> jwtUtil.validateAndGetClaims(token))
                .isInstanceOf(SignatureException.class);
    }

    // ---------------- tampered token ----------------

    @Test
    void validateAndGetClaims_tamperedPayload_throwsJwtException() {
        String token = buildToken(testKey, Date.from(Instant.now().plusSeconds(3600)), "user-123");
        // Flip a character in the payload segment to invalidate the signature.
        String[] parts = token.split("\\.");
        String tamperedPayload = new StringBuilder(parts[1]).reverse().toString();
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThatThrownBy(() -> jwtUtil.validateAndGetClaims(tamperedToken))
                .isInstanceOf(JwtException.class);
    }
}
