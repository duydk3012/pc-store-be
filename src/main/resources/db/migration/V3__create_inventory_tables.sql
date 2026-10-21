-- V3__create_inventory_tables.sql
-- Phân hệ Inventory & Purchasing: suppliers, purchase_orders, purchase_order_items, goods_receipts, receipt_items, product_serials, purchase_returns, purchase_return_items, delivery

CREATE TABLE suppliers (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    code                VARCHAR(50)   NOT NULL,
    name                VARCHAR(100)  NOT NULL,
    contact_person      VARCHAR(255)  NULL,
    email               VARCHAR(100)  NOT NULL,
    phone_number        VARCHAR(20)   NULL,
    address             VARCHAR(500)  NULL,
    tax_code            VARCHAR(50)   NULL,
    status              TINYINT       NOT NULL DEFAULT 1 COMMENT '0: inactive, 1: active',
    is_deleted          TINYINT(1)    NOT NULL DEFAULT 0,
    created_by          BIGINT        NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by    BIGINT        NULL,
    last_modified_date  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_suppliers_code UNIQUE (code),
    CONSTRAINT uk_suppliers_email UNIQUE (email),
    CONSTRAINT chk_suppliers_status CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_suppliers_status ON suppliers (status);
CREATE INDEX idx_suppliers_name ON suppliers (name);
CREATE INDEX idx_suppliers_phone ON suppliers (phone_number);
CREATE INDEX idx_suppliers_tax_code ON suppliers (tax_code);

CREATE TABLE purchase_orders (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    code                VARCHAR(50)   NOT NULL,
    name                VARCHAR(100)  NULL,
    supplier_id         BIGINT        NOT NULL,
    order_date          DATETIME(6)   NULL,
    expected_date       DATETIME(6)   NULL,
    status              TINYINT       NOT NULL DEFAULT 0,
    description         TEXT          NULL,
    total_amount        BIGINT        NOT NULL DEFAULT 0,
    is_deleted          TINYINT(1)    NOT NULL DEFAULT 0,
    created_by          BIGINT        NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by    BIGINT        NULL,
    last_modified_date  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_po_code UNIQUE (code),
    CONSTRAINT fk_po_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_po_supplier ON purchase_orders (supplier_id);
CREATE INDEX idx_po_status ON purchase_orders (status);

CREATE TABLE purchase_order_items (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    purchase_order_id   BIGINT        NOT NULL,
    product_id          BIGINT        NOT NULL,
    quantity_order      INT           NOT NULL DEFAULT 0,
    unit_cost           BIGINT        NOT NULL DEFAULT 0,
    quantity_received   INT           NOT NULL DEFAULT 0,
    subtotal            BIGINT        NOT NULL DEFAULT 0,
    created_by          BIGINT        NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by    BIGINT        NULL,
    last_modified_date  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_poi_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_poi_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_poi_order ON purchase_order_items (purchase_order_id);
CREATE INDEX idx_poi_product ON purchase_order_items (product_id);

CREATE TABLE goods_receipts (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    code                VARCHAR(50)   NOT NULL,
    name                VARCHAR(100)  NULL,
    purchase_order_id   BIGINT        NOT NULL,
    received_at         DATETIME(6)   NULL,
    received_by         BIGINT        NULL,
    status              TINYINT       NOT NULL DEFAULT 0 COMMENT '0: pending, 1: confirmed, 2: rejected, 3: cancelled',
    created_by          BIGINT        NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by    BIGINT        NULL,
    last_modified_date  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_gr_code UNIQUE (code),
    CONSTRAINT fk_gr_purchase_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id),
    CONSTRAINT fk_gr_received_by FOREIGN KEY (received_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_gr_purchase_order ON goods_receipts (purchase_order_id);
CREATE INDEX idx_gr_received_by ON goods_receipts (received_by);
CREATE INDEX idx_gr_status ON goods_receipts (status);

CREATE TABLE receipt_items (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    goods_receipt_id        BIGINT        NOT NULL,
    purchase_order_item_id  BIGINT        NOT NULL,
    actual_unit_cost        BIGINT        NOT NULL DEFAULT 0,
    quantity_received       INT           NOT NULL DEFAULT 0,
    quantity_accepted       INT           NOT NULL DEFAULT 0,
    quantity_defective      INT           NOT NULL DEFAULT 0,
    created_by              BIGINT        NULL,
    created_date            DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by        BIGINT        NULL,
    last_modified_date      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_ri_receipt FOREIGN KEY (goods_receipt_id) REFERENCES goods_receipts (id) ON DELETE CASCADE,
    CONSTRAINT fk_ri_poi FOREIGN KEY (purchase_order_item_id) REFERENCES purchase_order_items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ri_receipt ON receipt_items (goods_receipt_id);
CREATE INDEX idx_ri_poi ON receipt_items (purchase_order_item_id);

CREATE TABLE product_serials (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    product_id          BIGINT        NOT NULL,
    receipt_item_id     BIGINT        NULL,
    serial_number       VARCHAR(100)  NOT NULL,
    status              TINYINT       NOT NULL DEFAULT 2 COMMENT '0: good_condition, 1: defective, 2: in_stock, 3: pending_return, 4: returning, 5: returned, 6: packing, 7: delivering, 8: sold, 9: using_by_technical',
    created_by          BIGINT        NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by    BIGINT        NULL,
    last_modified_date  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_ps_serial_number UNIQUE (serial_number),
    CONSTRAINT fk_ps_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_ps_receipt_item FOREIGN KEY (receipt_item_id) REFERENCES receipt_items (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ps_product ON product_serials (product_id);
CREATE INDEX idx_ps_receipt_item ON product_serials (receipt_item_id);
CREATE INDEX idx_ps_status ON product_serials (status);

CREATE TABLE purchase_returns (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    code                VARCHAR(50)   NOT NULL,
    name                VARCHAR(255)  NULL,
    supplier_id         BIGINT        NOT NULL,
    reason              VARCHAR(255)  NULL,
    description         VARCHAR(500)  NULL,
    return_date         DATETIME(6)   NULL,
    total_amount        BIGINT        NOT NULL DEFAULT 0,
    status              TINYINT       NOT NULL DEFAULT 0 COMMENT '0: pending, 1: delivering, 2: returned, 3: cancelled',
    created_by          BIGINT        NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by    BIGINT        NULL,
    last_modified_date  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_pr_code UNIQUE (code),
    CONSTRAINT fk_pr_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pr_supplier ON purchase_returns (supplier_id);
CREATE INDEX idx_pr_status ON purchase_returns (status);

CREATE TABLE purchase_return_items (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    purchase_return_id  BIGINT        NOT NULL,
    product_serial_id   BIGINT        NULL,
    unit_cost           BIGINT        NOT NULL DEFAULT 0,
    created_by          BIGINT        NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by    BIGINT        NULL,
    last_modified_date  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_pri_return FOREIGN KEY (purchase_return_id) REFERENCES purchase_returns (id) ON DELETE CASCADE,
    CONSTRAINT fk_pri_serial FOREIGN KEY (product_serial_id) REFERENCES product_serials (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pri_return ON purchase_return_items (purchase_return_id);
CREATE INDEX idx_pri_serial ON purchase_return_items (product_serial_id);

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
