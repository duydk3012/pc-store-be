CREATE TABLE IF NOT EXISTS goods_receipts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NULL,
    purchase_order_id BIGINT NOT NULL,
    received_at TIMESTAMP NULL,
    received_by BIGINT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by BIGINT NULL,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE goods_receipts ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE goods_receipts ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS receipt_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    goods_receipt_id BIGINT NOT NULL,
    purchase_order_item_id BIGINT NOT NULL,
    actual_unit_cost BIGINT NOT NULL DEFAULT 0,
    quantity_received INT NOT NULL DEFAULT 0,
    quantity_accepted INT NOT NULL DEFAULT 0,
    quantity_defective INT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by BIGINT NULL,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE receipt_items ALTER COLUMN actual_unit_cost SET DEFAULT 0;
ALTER TABLE receipt_items ALTER COLUMN quantity_accepted SET DEFAULT 0;
ALTER TABLE receipt_items ALTER COLUMN quantity_defective SET DEFAULT 0;
ALTER TABLE receipt_items ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE receipt_items ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS product_serials (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    receipt_item_id BIGINT NULL,
    serial_number VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 2,
    is_terminated BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by BIGINT NULL,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE product_serials ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE product_serials ALTER COLUMN last_modified_date SET DEFAULT CURRENT_TIMESTAMP;
