-- ===========================================================================
-- V8: Chuan hoa 2 phuong thuc thanh toan COD va ZALOPAY, trang thai payment
--     va bo sung du lieu Demo phuc vu kiem thu luong thanh toan (IDs 22001-22005)
-- ===========================================================================

-- 1. Mo rong trang thai bang payment: 0 = PENDING (Cho thanh toan), 1 = SUCCESS (Thanh cong), 2 = FAILED (That bai / Huy)
ALTER TABLE payment DROP CHECK chk_payment_status;

ALTER TABLE payment
    MODIFY COLUMN status TINYINT UNSIGNED NOT NULL DEFAULT 0
    COMMENT '0: pending, 1: success, 2: failed',
    ADD CONSTRAINT chk_payment_status CHECK (status BETWEEN 0 AND 2);

-- 2. Chuan hoa 2 phuong thuc thanh toan chinh trong he thong: COD (10001) va ZALOPAY (10002)
INSERT INTO payment_methods (id, code, name, type, bank_name, account_number, status, is_deleted, created_date, last_modified_date)
VALUES
    (10001, 'COD', 'Thanh toán khi nhận hàng (COD)', 0, NULL, NULL, 1, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (10002, 'ZALOPAY', 'Thanh toán trực tuyến ZaloPay', 1, 'ZaloPay Sandbox', '554', 1, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
    code = VALUES(code),
    name = VALUES(name),
    type = VALUES(type),
    bank_name = VALUES(bank_name),
    account_number = VALUES(account_number),
    status = 1,
    is_deleted = 0,
    last_modified_date = CURRENT_TIMESTAMP(6);

-- 3. Chuyen cac giao dich demo cu (10003..10010) ve ZALOPAY (10002) va an cac phuong thuc demo du thua
UPDATE payment
SET payment_method_id = 10002
WHERE payment_method_id NOT IN (10001, 10002);

UPDATE payment_methods
SET status = 0,
    is_deleted = 1,
    last_modified_date = CURRENT_TIMESTAMP(6)
WHERE id NOT IN (10001, 10002);

-- 4. Backfill ban ghi payment cho cac don hang COMPLETED (status = 4) chua co ban ghi thanh toan
INSERT INTO payment (order_id, payment_method_id, amount, transaction_code, status, paid_at, created_date, last_modified_date)
SELECT
    o.id AS order_id,
    10001 AS payment_method_id,
    o.final_amount AS amount,
    CONCAT('COD-HIST-', o.id) AS transaction_code,
    1 AS status,
    o.last_modified_date AS paid_at,
    o.created_date,
    o.last_modified_date
FROM orders o
LEFT JOIN payment p ON o.id = p.order_id
WHERE o.status = 4
  AND p.id IS NULL;

-- 5. Bo sung ban ghi payment PENDING cho 2 don hang demo bao hanh (21001, 21002) tu V3 neu chua co
INSERT INTO payment (id, order_id, payment_method_id, amount, transaction_code, status, paid_at, created_date, last_modified_date)
SELECT 21001, 21001, 10001, 2000000, 'COD-DEMO-BH-DIRECT', 0, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
WHERE EXISTS (SELECT 1 FROM orders WHERE id = 21001)
  AND NOT EXISTS (SELECT 1 FROM payment WHERE order_id = 21001);

INSERT INTO payment (id, order_id, payment_method_id, amount, transaction_code, status, paid_at, created_date, last_modified_date)
SELECT 21002, 21002, 10001, 1000000, 'COD-DEMO-BH-DELIVERY', 0, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
WHERE EXISTS (SELECT 1 FROM orders WHERE id = 21002)
  AND NOT EXISTS (SELECT 1 FROM payment WHERE order_id = 21002);

-- ===========================================================================
-- 6. DU LIEU DEMO CHUYEN BIET CHO MODULE THANH TOAN (COD & ZALOPAY SANDBOX)
--    Reserved IDs: 22001 - 22005 (Khach hang: demo_customer01, user_id = 10011)
-- ===========================================================================

-- 6.1. Product Serials phuc vu cac don hang demo thanh toan
--      - 22001: Gắn với đơn COD chờ xác nhận (status = 6: PACKING)
--      - 22002: Gắn với đơn COD đang giao hàng (status = 7: DELIVERING)
--      - 22003: Gắn với đơn ZaloPay chờ thanh toán (status = 6: PACKING)
--      - 22004, 22005: Serial dự phòng sẵn trong kho (status = 2: IN_STOCK) để test tạo đơn mới & gán serial
INSERT INTO product_serials (id, product_id, receipt_item_id, serial_number, status, is_terminated, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (22001, 10001, 10001, 'DEMO-PAY-COD-SN01', 6, 0, 10003, CURRENT_TIMESTAMP(6), 10003, CURRENT_TIMESTAMP(6)),
    (22002, 10003, 10003, 'DEMO-PAY-COD-SN02', 7, 0, 10003, CURRENT_TIMESTAMP(6), 10003, CURRENT_TIMESTAMP(6)),
    (22003, 10003, 10003, 'DEMO-PAY-ZP-SN01',  6, 0, 10003, CURRENT_TIMESTAMP(6), 10003, CURRENT_TIMESTAMP(6)),
    (22004, 10001, 10001, 'DEMO-PAY-SPARE-01', 2, 0, 10003, CURRENT_TIMESTAMP(6), 10003, CURRENT_TIMESTAMP(6)),
    (22005, 10003, 10003, 'DEMO-PAY-SPARE-02', 2, 0, 10003, CURRENT_TIMESTAMP(6), 10003, CURRENT_TIMESTAMP(6));

-- 6.2. Cac don hang Demo cho 4 kich ban thanh toan:
--      - Order 22001 (DEMO-PAY-COD-01): Đơn COD ở trạng thái PENDING (0), chờ nhân viên xác nhận thu tiền hoặc chuyển sang ZaloPay
--      - Order 22002 (DEMO-PAY-COD-DELIV): Đơn COD ở trạng thái DELIVERING (3), sẵn sàng để khách bấm hoàn tất nhận hàng (/complete)
--      - Order 22003 (DEMO-PAY-ZALOPAY-01): Đơn ZaloPay ở trạng thái PENDING (0), sẵn sàng gọi /api/payments/zalopay/create hoặc test Webhook Callback
--      - Order 22004 (DEMO-PAY-CANCEL-01): Đơn ZaloPay ở trạng thái PENDING (0), dùng để test hủy đơn -> giao dịch thanh toán tự động chuyển sang FAILED (2)
INSERT INTO orders (id, code, user_id, type, total_amount, discount_amount, final_amount, status, address, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (22001, 'DEMO-PAY-COD-01',     10011, 1, 4800000, 0,     4800000, 0, '[REC:Nguyễn Minh An|TEL:0901000011|NOTE:Demo thanh toán COD chờ xác nhận] 11 Đường Demo, Cầu Giấy, Hà Nội', 10011, CURRENT_TIMESTAMP(6), 10011, CURRENT_TIMESTAMP(6)),
    (22002, 'DEMO-PAY-COD-DELIV',  10011, 1, 720000,  20000, 700000,  3, '[REC:Nguyễn Minh An|TEL:0901000011|NOTE:Demo đơn COD đang giao chờ khách nhận] 11 Đường Demo, Cầu Giấy, Hà Nội', 10011, CURRENT_TIMESTAMP(6), 10005, CURRENT_TIMESTAMP(6)),
    (22003, 'DEMO-PAY-ZALOPAY-01', 10011, 1, 720000,  0,     720000,  0, '[REC:Nguyễn Minh An|TEL:0901000011|NOTE:Demo thanh toán trực tuyến ZaloPay Sandbox] 11 Đường Demo, Cầu Giấy, Hà Nội', 10011, CURRENT_TIMESTAMP(6), 10011, CURRENT_TIMESTAMP(6)),
    (22004, 'DEMO-PAY-CANCEL-01',  10011, 1, 720000,  0,     720000,  0, '[REC:Nguyễn Minh An|TEL:0901000011|NOTE:Demo hủy đơn hàng khi chưa thanh toán] 11 Đường Demo, Cầu Giấy, Hà Nội', 10011, CURRENT_TIMESTAMP(6), 10011, CURRENT_TIMESTAMP(6));

-- 6.3. Chi tiet mat hang (order_items) cua 4 don hang demo
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, discount_amount, subtotal, created_date, last_modified_date) VALUES
    (22001, 22001, 10001, 1, 4800000, 0,     4800000, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (22002, 22002, 10003, 1, 720000,  20000, 700000,  CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (22003, 22003, 10003, 1, 720000,  0,     720000,  CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (22004, 22004, 10003, 1, 720000,  0,     720000,  CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

-- 6.4. Gan serial (order_item_serial) cho cac don hang demo
INSERT INTO order_item_serial (id, order_item_id, product_serial_id, created_date) VALUES
    (22001, 22001, 22001, CURRENT_TIMESTAMP(6)),
    (22002, 22002, 22002, CURRENT_TIMESTAMP(6)),
    (22003, 22003, 22003, CURRENT_TIMESTAMP(6));

-- 6.5. Ban ghi thanh toan (payment) khoi tao o trang thai PENDING (0) cho 4 don hang demo
INSERT INTO payment (id, order_id, payment_method_id, amount, transaction_code, status, paid_at, created_date, last_modified_date) VALUES
    (22001, 22001, 10001, 4800000, 'COD-DEMO-PAY-COD-01',    0, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (22002, 22002, 10001, 700000,  'COD-DEMO-PAY-COD-DELIV', 0, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (22003, 22003, 10002, 720000,  '261123_22003_999999',    0, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (22004, 22004, 10002, 720000,  '261123_22004_888888',    0, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
