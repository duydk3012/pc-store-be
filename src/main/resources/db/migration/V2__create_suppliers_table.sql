CREATE TABLE suppliers (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    code                VARCHAR(50)   NOT NULL,
    name                VARCHAR(100)  NOT NULL,
    contact_person      VARCHAR(255)  NULL,
    email               VARCHAR(100)  NOT NULL,
    phone_number        VARCHAR(20)   NULL,
    address             VARCHAR(500)  NULL,
    tax_code            VARCHAR(50)   NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    is_deleted          TINYINT(1)    NOT NULL DEFAULT 0,
    created_by          BIGINT        NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by    BIGINT        NULL,
    last_modified_date  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_suppliers_code UNIQUE (code),
    CONSTRAINT uk_suppliers_email UNIQUE (email),
    CONSTRAINT chk_suppliers_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_suppliers_status ON suppliers (status);
CREATE INDEX idx_suppliers_name ON suppliers (name);
CREATE INDEX idx_suppliers_phone ON suppliers (phone_number);
CREATE INDEX idx_suppliers_tax_code ON suppliers (tax_code);
