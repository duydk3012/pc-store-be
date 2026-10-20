-- V3__create_catalog_tables.sql
-- Module Catalog: categories, brands, products, category_products, building_pc, product_price

CREATE TABLE categories (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    name                VARCHAR(100)  NOT NULL,
    slug                VARCHAR(100)  NOT NULL,
    description         VARCHAR(500)  NULL,
    status              TINYINT       NOT NULL DEFAULT 1 COMMENT '0: inactive, 1: active',
    parent_id           BIGINT        NULL,
    is_deleted          TINYINT(1)    NOT NULL DEFAULT 0,
    created_by          BIGINT        NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by    BIGINT        NULL,
    last_modified_date  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_slug UNIQUE (slug),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_categories_parent ON categories (parent_id);
CREATE INDEX idx_categories_status ON categories (status);

CREATE TABLE brands (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    name                VARCHAR(100)  NOT NULL,
    slug                VARCHAR(100)  NOT NULL,
    description         VARCHAR(500)  NULL,
    status              TINYINT       NOT NULL DEFAULT 1 COMMENT '0: inactive, 1: active',
    is_deleted          TINYINT(1)    NOT NULL DEFAULT 0,
    created_by          BIGINT        NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by    BIGINT        NULL,
    last_modified_date  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_brands_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_brands_status ON brands (status);

CREATE TABLE products (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    name                VARCHAR(255)  NOT NULL,
    code                VARCHAR(50)   NOT NULL,
    description         TEXT          NULL,
    specification       TEXT          NULL,
    product_type        TINYINT       NOT NULL DEFAULT 0 COMMENT '0: Component, 1: Laptop, 2: PC',
    status              TINYINT       NOT NULL DEFAULT 1 COMMENT '0: inactive, 1: active, 2: out_of_stock',
    slug                VARCHAR(255)  NULL,
    image_url           VARCHAR(500)  NULL,
    brand_id            BIGINT        NULL,
    is_deleted          TINYINT(1)    NOT NULL DEFAULT 0,
    created_by          BIGINT        NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_by    BIGINT        NULL,
    last_modified_date  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_products_code UNIQUE (code),
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_products_brand ON products (brand_id);
CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_products_type ON products (product_type);
CREATE INDEX idx_products_slug ON products (slug);

CREATE TABLE category_products (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    product_id          BIGINT        NOT NULL,
    category_id         BIGINT        NOT NULL,
    is_primary          TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '0: false, 1: true',
    PRIMARY KEY (id),
    CONSTRAINT uk_category_products UNIQUE (product_id, category_id),
    CONSTRAINT fk_cp_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_cp_category ON category_products (category_id);

CREATE TABLE building_pc (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    pc_id               BIGINT        NOT NULL,
    product_id          BIGINT        NOT NULL,
    quantity            INT           NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_bpc_pc FOREIGN KEY (pc_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_bpc_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_bpc_pc ON building_pc (pc_id);
CREATE INDEX idx_bpc_product ON building_pc (product_id);

CREATE TABLE product_price (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    product_id          BIGINT        NOT NULL,
    price               BIGINT        NOT NULL COMMENT 'Price in VND',
    effective_from      DATETIME(6)   NULL,
    effective_to        DATETIME(6)   NULL,
    is_present_price    TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '0: false, 1: true',
    created_by          BIGINT        NULL,
    created_date        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_pp_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pp_product ON product_price (product_id);
CREATE INDEX idx_pp_is_present ON product_price (is_present_price);
