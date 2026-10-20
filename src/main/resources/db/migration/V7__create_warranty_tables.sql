-- V7__create_warranty_tables.sql
-- Module Warranty: warranty_policy, warranty, warranty_request, warranty_parts, warranty_part_serial

CREATE TABLE warranty_policy (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    name                VARCHAR(255)  NOT NULL,
    duration_days       INT           NOT NULL DEFAULT 365,
    description         VARCHAR(500)  NULL,
    target_type         VARCHAR(50)   NULL COMMENT 'CATEGORY, PRODUCT',
    target_id           BIGINT        NULL,
    status              TINYINT       NOT NULL DEFAULT 1 COMMENT '0: inactive, 1: active',
    is_deleted          TINYINT(1)    NOT NULL DEFAULT 0,
    created_by          BIGINT        NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by    BIGINT        NULL,
    last_modified_date  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_wp_target ON warranty_policy (target_type, target_id);
CREATE INDEX idx_wp_status ON warranty_policy (status);

CREATE TABLE warranty (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    order_item_serial_id  BIGINT        NULL,
    warranty_policy_id    BIGINT        NOT NULL,
    start_date            DATETIME(6)   NOT NULL,
    end_date              DATETIME(6)   NOT NULL,
    status                TINYINT       NOT NULL DEFAULT 1 COMMENT '0: inactive, 1: active',
    note                  VARCHAR(500)  NULL,
    created_by            BIGINT        NULL,
    created_date          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by      BIGINT        NULL,
    last_modified_date    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_warranty_ois FOREIGN KEY (order_item_serial_id) REFERENCES order_item_serial (id) ON DELETE SET NULL,
    CONSTRAINT fk_warranty_policy FOREIGN KEY (warranty_policy_id) REFERENCES warranty_policy (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_warranty_ois ON warranty (order_item_serial_id);
CREATE INDEX idx_warranty_policy ON warranty (warranty_policy_id);
CREATE INDEX idx_warranty_status ON warranty (status);

CREATE TABLE warranty_request (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    request_code        VARCHAR(50)   NOT NULL,
    warranty_id         BIGINT        NOT NULL,
    customer_id         BIGINT        NOT NULL,
    technician_id       BIGINT        NULL,
    issue_description   VARCHAR(500)  NULL,
    inspection_notes    VARCHAR(500)  NULL,
    solution            VARCHAR(500)  NULL,
    status              TINYINT       NOT NULL DEFAULT 0 COMMENT '0: pending, 1: in_progress, 2: rejected, 3: completed',
    completed_at        DATETIME(6)   NULL,
    created_by          BIGINT        NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by    BIGINT        NULL,
    last_modified_date  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_wr_code UNIQUE (request_code),
    CONSTRAINT fk_wr_warranty FOREIGN KEY (warranty_id) REFERENCES warranty (id),
    CONSTRAINT fk_wr_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_wr_technician FOREIGN KEY (technician_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_wr_warranty ON warranty_request (warranty_id);
CREATE INDEX idx_wr_customer ON warranty_request (customer_id);
CREATE INDEX idx_wr_technician ON warranty_request (technician_id);
CREATE INDEX idx_wr_status ON warranty_request (status);

CREATE TABLE warranty_parts (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    warranty_request_id  BIGINT       NOT NULL,
    product_id           BIGINT       NOT NULL,
    quantity             INT          NOT NULL DEFAULT 1,
    unit_cost            BIGINT       NOT NULL DEFAULT 0,
    status               TINYINT      NOT NULL DEFAULT 0 COMMENT '0: pending, 1: reserved, 2: installed, 3: returned, 4: cancelled',
    created_by           BIGINT       NULL,
    created_date         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by     BIGINT       NULL,
    last_modified_date   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_wp_request FOREIGN KEY (warranty_request_id) REFERENCES warranty_request (id) ON DELETE CASCADE,
    CONSTRAINT fk_wp_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_wp_request ON warranty_parts (warranty_request_id);
CREATE INDEX idx_wp_product ON warranty_parts (product_id);
CREATE INDEX idx_wp_status ON warranty_parts (status);

CREATE TABLE warranty_part_serial (
    id                 BIGINT  NOT NULL AUTO_INCREMENT,
    warranty_part_id   BIGINT  NOT NULL,
    product_serial_id  BIGINT  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_wps_part FOREIGN KEY (warranty_part_id) REFERENCES warranty_parts (id) ON DELETE CASCADE,
    CONSTRAINT fk_wps_serial FOREIGN KEY (product_serial_id) REFERENCES product_serials (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_wps_part ON warranty_part_serial (warranty_part_id);
CREATE INDEX idx_wps_serial ON warranty_part_serial (product_serial_id);
