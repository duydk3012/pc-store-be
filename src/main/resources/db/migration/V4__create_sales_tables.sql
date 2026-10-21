<<<<<<<< HEAD:src/main/resources/db/migration/V4__create_sales_tables.sql
-- V4__create_sales_tables.sql
-- Phân hệ Sales, Orders, Invoices & Payments: carts, cart_items, orders, order_items, order_item_serial, payment_methods, invoices, payment
========
-- V7__create_sales_tables.sql
-- Module Sales: carts, cart_items, orders, order_items, order_item_serial
>>>>>>>> main:src/main/resources/db/migration/V7__create_sales_tables.sql

CREATE TABLE carts (
    id            BIGINT  NOT NULL AUTO_INCREMENT,
    customer_id   BIGINT  NOT NULL,
    quantity      INT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_carts_customer UNIQUE (customer_id),
    CONSTRAINT fk_carts_customer FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cart_items (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    cart_id             BIGINT       NOT NULL,
    product_id          BIGINT       NOT NULL,
    quantity            INT          NOT NULL DEFAULT 1,
    unit_price          BIGINT       NOT NULL DEFAULT 0,
    created_date        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_date  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT fk_ci_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT fk_ci_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ci_cart ON cart_items (cart_id);
CREATE INDEX idx_ci_product ON cart_items (product_id);

CREATE TABLE orders (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    code                VARCHAR(50)   NOT NULL,
    user_id             BIGINT        NOT NULL,
    type                TINYINT       NOT NULL DEFAULT 1 COMMENT '0: in_store, 1: online',
    total_amount        BIGINT        NOT NULL DEFAULT 0,
    discount_amount     BIGINT        NOT NULL DEFAULT 0,
    final_amount        BIGINT        NOT NULL DEFAULT 0,
    status              TINYINT       NOT NULL DEFAULT 0 COMMENT '0: pending, 1: confirmed, 2: rejected, 3: processing, 4: completed, 5: cancelled, 6: returned',
    address             VARCHAR(500)  NULL,
    created_by          BIGINT        NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by    BIGINT        NULL,
    last_modified_date  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_orders_code UNIQUE (code),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_orders_user ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_type ON orders (type);

CREATE TABLE order_items (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    order_id            BIGINT       NOT NULL,
    product_id          BIGINT       NOT NULL,
    quantity            INT          NOT NULL DEFAULT 1,
    unit_price          BIGINT       NOT NULL DEFAULT 0,
    discount_amount     BIGINT       NOT NULL DEFAULT 0,
    subtotal            BIGINT       NOT NULL DEFAULT 0,
    created_date        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_date  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_oi_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_oi_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_oi_order ON order_items (order_id);
CREATE INDEX idx_oi_product ON order_items (product_id);

CREATE TABLE order_item_serial (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    order_item_id      BIGINT       NOT NULL,
    product_serial_id  BIGINT       NOT NULL,
    created_date       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_ois_product_serial UNIQUE (product_serial_id),
    CONSTRAINT fk_ois_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_ois_product_serial FOREIGN KEY (product_serial_id) REFERENCES product_serials (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ois_order_item ON order_item_serial (order_item_id);

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
