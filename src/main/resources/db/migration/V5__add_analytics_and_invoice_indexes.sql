-- ===========================================================================
-- V5: Bo sung Index toi uu hoa truy van Analytics & Backfill du lieu Hoa don
-- ===========================================================================

-- 1. Index cho bang invoices (Tra cuu doanh thu ke toan va hoach toan thue)
CREATE INDEX idx_invoices_status_date ON invoices (status, invoice_date);

-- 2. Index cho bang orders (Tra cuu van hanh don hang va thoi gian hoan tat)
CREATE INDEX idx_orders_status_mod_date ON orders (status, last_modified_date);
CREATE INDEX idx_orders_status_create_date ON orders (status, created_date);

-- 3. Index cho bang product_serials (Tra cuu trang thai ton kho va thanh ly)
CREATE INDEX idx_ps_status_terminated ON product_serials (status, is_terminated);

-- 4. Index cho bang receipt_items
CREATE INDEX idx_ri_receipt_product ON receipt_items (goods_receipt_id, product_id);

-- 5. Backfill Hoa don cho cac don hang COMPLETED trong qua khu chua co hoa don
INSERT INTO invoices (code, order_id, invoice_date, total_amount, discount_amount, tax_amount, amount, status, created_date, last_modified_date)
SELECT 
    CONCAT('HD-HIST-', o.id) AS code,
    o.id AS order_id,
    o.last_modified_date AS invoice_date,
    o.total_amount,
    o.discount_amount,
    0 AS tax_amount,
    o.final_amount AS amount,
    2 AS status, -- 2: PAID
    o.created_date,
    o.last_modified_date
FROM orders o
LEFT JOIN invoices i ON o.id = i.order_id
WHERE o.status = 4 -- 4: COMPLETED
  AND i.id IS NULL;
