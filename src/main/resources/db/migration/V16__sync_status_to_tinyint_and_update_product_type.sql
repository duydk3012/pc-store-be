-- V16__sync_status_to_tinyint_and_update_product_type.sql

-- 1. Sync users status to TINYINT
-- Phải DROP CHECK constraint trước khi UPDATE giá trị số sang cột status
ALTER TABLE users DROP CHECK chk_users_status;

UPDATE users SET status = '1' WHERE status = 'ACTIVE';
UPDATE users SET status = '0' WHERE status = 'INACTIVE';
UPDATE users SET status = '2' WHERE status = 'BANNED';

ALTER TABLE users MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '0: inactive, 1: active, 2: banned';
ALTER TABLE users ADD CONSTRAINT chk_users_status CHECK (status IN (0, 1, 2));

-- 2. Sync suppliers status to TINYINT
-- Phải DROP CHECK constraint trước khi UPDATE giá trị số sang cột status
ALTER TABLE suppliers DROP CHECK chk_suppliers_status;

UPDATE suppliers SET status = '1' WHERE status = 'ACTIVE';
UPDATE suppliers SET status = '0' WHERE status = 'INACTIVE';

ALTER TABLE suppliers MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '0: inactive, 1: active';
ALTER TABLE suppliers ADD CONSTRAINT chk_suppliers_status CHECK (status IN (0, 1));

-- 3. Update products product_type to 2 types: 0 - other, 1 - pc
-- Previous: 0: Component, 1: Laptop, 2: PC
-- New: 0: other, 1: pc
-- Đổi 1 (Laptop) thành 0 (other) trước, sau đó mới đổi 2 (PC) thành 1 (pc)
UPDATE products SET product_type = 0 WHERE product_type = 1;
UPDATE products SET product_type = 1 WHERE product_type = 2;

ALTER TABLE products MODIFY COLUMN product_type TINYINT NOT NULL DEFAULT 0 COMMENT '0: other, 1: pc';
