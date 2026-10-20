package com.datn.pc_store.modules.auth.service;

import com.datn.pc_store.modules.auth.entity.RefreshToken;
import com.datn.pc_store.modules.auth.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.repository = repository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /** Tạo token mới, lưu hash, trả về chuỗi gốc (chỉ trả cho client một lần) **/
    public String issue(Long userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken entity = new RefreshToken();
        entity.setUserId(userId);
        entity.setTokenHash(hash(raw));
        entity.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        repository.save(entity);
        return raw;
    }

    public Optional<RefreshToken> findByRaw(String raw) {
        return repository.findByTokenHash(hash(raw));
    }

    @Transactional
    public void revokeAllByUser(Long userId) {
        repository.revokeAllByUserId(userId);
    }

    @Transactional
    public int deleteExpired() {
        return repository.deleteExpired(Instant.now());
    }

    public static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
