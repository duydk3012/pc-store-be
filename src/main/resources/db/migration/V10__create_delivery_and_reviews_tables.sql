-- V10__create_delivery_and_reviews_tables.sql
-- Module Delivery, Product Reviews & Favorites: delivery, product_reviews, product_favorites

CREATE TABLE delivery (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    resource_type   VARCHAR(30)   NOT NULL COMMENT 'ORDER, PURCHASE_RETURN',
    resource_id     BIGINT        NOT NULL,
    tracking_code   VARCHAR(100)  NULL,
    carrier_name    VARCHAR(255)  NULL,
    shipping_fee    BIGINT        NOT NULL DEFAULT 0,
    status          TINYINT       NOT NULL DEFAULT 0 COMMENT '0: picked_up, 1: in_transit, 2: delivered, 3: returned',
    shipped_date    DATETIME(6)   NULL,
    shipped_by      BIGINT        NULL,
    delivered_date  DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_delivery_shipped_by FOREIGN KEY (shipped_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_delivery_resource_type CHECK (resource_type IN ('ORDER', 'PURCHASE_RETURN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_delivery_resource ON delivery (resource_type, resource_id);
CREATE INDEX idx_delivery_tracking ON delivery (tracking_code);
CREATE INDEX idx_delivery_status ON delivery (status);

CREATE TABLE product_reviews (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    product_id   BIGINT        NOT NULL,
    customer_id  BIGINT        NOT NULL,
    rating       TINYINT       NOT NULL,
    title        VARCHAR(100)  NULL,
    content      TEXT          NULL,
    status       TINYINT       NOT NULL DEFAULT 1 COMMENT '0: hidden, 1: visible',
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_pr_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_pr_customer FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pr_product_user ON product_reviews (product_id, customer_id);
CREATE INDEX idx_pr_status ON product_reviews (status);

CREATE TABLE product_favorites (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    customer_id  BIGINT       NOT NULL,
    product_id   BIGINT       NOT NULL,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_pf_customer_product UNIQUE (customer_id, product_id),
    CONSTRAINT fk_pf_customer FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_pf_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pf_product ON product_favorites (product_id);
