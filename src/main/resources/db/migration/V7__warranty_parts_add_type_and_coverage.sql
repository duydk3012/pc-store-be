-- Bổ sung phân loại 3 loại warranty part và thông tin giá/bảo hành.
-- type: 0 = WARRANTY (bảo hành), 1 = REPAIR (sửa chữa), 2 = UPGRADE (nâng cấp)

ALTER TABLE warranty_parts
    ADD COLUMN type TINYINT UNSIGNED NOT NULL DEFAULT 0
    COMMENT '0: WARRANTY, 1: REPAIR, 2: UPGRADE'
    AFTER warranty_request_id;

-- Cho phép product_id có thể NULL đối với loại REPAIR (chỉ tính công/phí sửa chữa, không gắn linh kiện cụ thể)
ALTER TABLE warranty_parts
    MODIFY COLUMN product_id BIGINT NULL;

ALTER TABLE warranty_parts
    ADD COLUMN original_price BIGINT NOT NULL DEFAULT 0
    COMMENT 'Giá gốc sản phẩm tại thời điểm tạo part'
    AFTER unit_cost;

ALTER TABLE warranty_parts
    ADD COLUMN warranty_coverage_percent INT NOT NULL DEFAULT 100
    COMMENT 'Phần trăm bảo hành áp dụng (type=WARRANTY). 100=miễn phí, 50=trả 50%'
    AFTER original_price;

ALTER TABLE warranty_parts
    ADD CONSTRAINT chk_warranty_parts_type CHECK (type IN (0, 1, 2));

ALTER TABLE warranty_parts
    ADD CONSTRAINT chk_warranty_coverage CHECK (warranty_coverage_percent BETWEEN 0 AND 100);

-- Thêm total_cost vào warranty_request để lưu tổng chi phí khách phải trả
ALTER TABLE warranty_request
    ADD COLUMN total_cost BIGINT NOT NULL DEFAULT 0
    COMMENT 'Tổng chi phí khách phải trả = SUM(unit_cost * quantity)'
    AFTER completed_at;
