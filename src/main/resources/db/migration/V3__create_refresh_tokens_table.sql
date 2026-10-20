CREATE TABLE refresh_tokens (
                                id            BIGINT       NOT NULL AUTO_INCREMENT,
                                user_id       BIGINT       NOT NULL,
                                token_hash    VARCHAR(64)  NOT NULL,
                                expires_at    DATETIME(6)  NOT NULL,
                                revoked       TINYINT(1)   NOT NULL DEFAULT 0,
                                created_date  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                PRIMARY KEY (id),
                                CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
                                CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);