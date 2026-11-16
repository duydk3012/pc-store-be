-- ===========================================================================
-- V4: Dong nhat trang thai don hang (orders) va phieu tra hang NCC (purchase_returns)
-- 1. OrderStatus: PROCESSING (3) -> DELIVERING (3)
-- 2. PurchaseReturnStatus:
--    - RETURNED cu (2) -> COMPLETED (2)
--    - CANCELLED (3)
--    - DRAFT cu (4) duoc loai bo; status = 4 nay la RETURNED (Tra hang ve kho)
--    - Chuyen cac phieu hoan tra o trang thai DRAFT cu (4) ve PENDING (0)
-- ===========================================================================

-- 1. Chuyen cac phieu hoan tra nhap cu (status = 4 truoc day la DRAFT) sang PENDING (0)
UPDATE purchase_returns
SET status = 0
WHERE status = 4;

-- Dong bo serials cua cac phieu hoan tra do sang PENDING_RETURN (3) neu chua ket thuc
UPDATE product_serials ps
JOIN purchase_return_items pri ON pri.product_serial_id = ps.id
JOIN purchase_returns pr ON pr.id = pri.purchase_return_id
SET ps.status = 3
WHERE pr.status = 0
  AND ps.is_terminated = 0
  AND ps.status IN (0, 1, 2);

-- 2. Cap nhat COMMENT cho cot status trong bang orders
ALTER TABLE orders
    MODIFY COLUMN status TINYINT UNSIGNED NOT NULL DEFAULT 0
    COMMENT '0: pending, 1: confirmed, 2: rejected, 3: delivering, 4: completed, 5: cancelled, 6: returned';

-- 3. Cap nhat COMMENT cho cot status trong bang purchase_returns
ALTER TABLE purchase_returns
    MODIFY COLUMN status TINYINT UNSIGNED NOT NULL DEFAULT 0
    COMMENT '0: pending, 1: delivering, 2: completed, 3: cancelled, 4: returned';
