-- V8__create_invoice_and_payment_tables.sql
-- Module Invoices & Payments: payment_methods, invoices, payment

CREATE TABLE payment_methods (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    code                VARCHAR(50)   NOT NULL,
    name                VARCHAR(255)  NOT NULL,
    type                TINYINT       NOT NULL DEFAULT 0 COMMENT '0: cash, 1: bank_transfer',
    bank_name           VARCHAR(255)  NULL,
    account_number      VARCHAR(100)  NULL,
    status              TINYINT       NOT NULL DEFAULT 1 COMMENT '0: inactive, 1: active',
    is_deleted          TINYINT(1)    NOT NULL DEFAULT 0,
    created_by          BIGINT        NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by    BIGINT        NULL,
    last_modified_date  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_pm_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pm_status ON payment_methods (status);

CREATE TABLE invoices (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    code                VARCHAR(50)   NOT NULL,
    order_id            BIGINT        NOT NULL,
    invoice_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    total_amount        BIGINT        NOT NULL DEFAULT 0,
    discount_amount     BIGINT        NOT NULL DEFAULT 0,
    tax_amount          BIGINT        NOT NULL DEFAULT 0,
    amount              BIGINT        NOT NULL DEFAULT 0,
    status              TINYINT       NOT NULL DEFAULT 0 COMMENT '0: pending, 1: canceled, 2: paid',
    created_by          BIGINT        NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by    BIGINT        NULL,
    last_modified_date  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_invoices_code UNIQUE (code),
    CONSTRAINT fk_invoices_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_invoices_order ON invoices (order_id);
CREATE INDEX idx_invoices_status ON invoices (status);

CREATE TABLE payment (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    order_id            BIGINT        NOT NULL,
    payment_method_id   BIGINT        NOT NULL,
    amount              BIGINT        NOT NULL DEFAULT 0,
    transaction_code    VARCHAR(255)  NULL,
    status              TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '0: fail, 1: success',
    paid_at             DATETIME(6)   NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_date  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_tx_code UNIQUE (transaction_code),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_payment_method FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_payment_order ON payment (order_id);
CREATE INDEX idx_payment_method ON payment (payment_method_id);
CREATE INDEX idx_payment_status ON payment (status);
