package com.backend.sensor_data.util;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private final SecretKey key;

    public JwtUtil() {
        String secret = SecretReader.readSecret("JWT_SECRET_FILE", "JWT_SECRET");
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Claims validateAndGetClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}