package com.datn.pc_store.modules.auth.security;

import com.datn.pc_store.modules.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    @Getter
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET phải có ít nhất 32 ký tự (hiện tại: " + keyBytes.length + "). Kiểm tra file .env");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .claim("tv", user.getTokenVersion())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public int extractTokenVersion(String token) {
        Integer tv = parse(token).get("tv", Integer.class);
        return tv == null ? -1 : tv;
    }

    public boolean isValid(String token) {
        return parseClaimsIfValid(token).isPresent();
    }

    /**
     * Parse và xác thực chữ ký JWT một lần duy nhất để tránh lặp chi phí HMAC ở JwtAuthFilter.
     */
    public java.util.Optional<Claims> parseClaimsIfValid(String token) {
        try {
            return java.util.Optional.of(parse(token));
        } catch (JwtException | IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
    }

    public int extractTokenVersion(Claims claims) {
        Integer tv = claims.get("tv", Integer.class);
        return tv == null ? -1 : tv;
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }

}
