-- V1__create_identity_tables.sql
-- Phân hệ Identity & Access Management (User & Auth): users, refresh_tokens

CREATE TABLE users (
    id                           BIGINT        NOT NULL AUTO_INCREMENT,
    username                     VARCHAR(50)   NOT NULL,
    password_hash                VARCHAR(255)  NOT NULL,
    full_name                    VARCHAR(100)  NOT NULL,
    email                        VARCHAR(255)  NOT NULL,
    phone_number                 VARCHAR(15)   NULL,
    address                      VARCHAR(500)  NULL,
    citizen_identification_card  VARCHAR(20)   NULL,
    role                         VARCHAR(30)   NOT NULL,
    status                       VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    token_version                INT           NOT NULL DEFAULT 0,
    is_deleted                   TINYINT(1)    NOT NULL DEFAULT 0,
    created_by                   BIGINT        NULL,
    created_date                 DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by             BIGINT        NULL,
    last_modified_date           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email),
    CONSTRAINT chk_users_role    CHECK (role IN ('ADMIN','MANAGER','CUSTOMER','STAFF_WAREHOUSE','STAFF_SALES')),
    CONSTRAINT chk_users_status  CHECK (status IN ('ACTIVE','INACTIVE','BANNED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_role_status ON users (role, status);
CREATE INDEX idx_users_phone ON users (phone_number);

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
