-- ===========================================================================
-- V9: Khoi tao bang nhat ky he thong (system_logs) phuc vu kiem toan hoat dong
-- ===========================================================================

CREATE TABLE system_logs (
    id                 BIGINT            NOT NULL AUTO_INCREMENT,
    user_id            BIGINT            NULL     COMMENT 'ID nguoi thao tac (NULL neu la guest, webhook hoac scheduler)',
    username           VARCHAR(50)       NULL     COMMENT 'Snapshot username tai thoi diem thao tac',
    user_role          TINYINT UNSIGNED  NULL     COMMENT '0: admin, 1: manager, 2: customer, 3: staff_warehouse, 4: staff_sales, 5: staff_tech',
    module             TINYINT UNSIGNED  NOT NULL COMMENT '0: auth, 1: user, 2: catalog, 3: supplier, 4: purchase_order, 5: goods_receipt, 6: product_serial, 7: purchase_return, 8: order, 9: payment, 10: delivery, 11: warranty, 12: system',
    action             TINYINT UNSIGNED  NOT NULL COMMENT '0: login, 1: logout, 2: create, 3: update, 4: update_status, 5: delete, 6: assign_serial, 7: payment_confirm, 8: payment_webhook, 9: export, 10: scheduled_job',
    status             TINYINT UNSIGNED  NOT NULL DEFAULT 1 COMMENT '0: failed, 1: success',
    resource_type      VARCHAR(50)       NULL     COMMENT 'Loai tai nguyen tac dong: ORDER, GOODS_RECEIPT, WARRANTY_REQUEST, USER...',
    resource_id        BIGINT            NULL     COMMENT 'ID ban ghi bi tac dong',
    resource_code      VARCHAR(100)      NULL     COMMENT 'Ma chung tu/ban ghi (VD: DH-10001, PN-10002, WR-10001)',
    description        VARCHAR(500)      NOT NULL COMMENT 'Mo ta ngan gon thao tac hien thi tren giao dien quan tri',
    detail_json        TEXT              NULL     COMMENT 'Chi tiet thay doi (old/new values hoac metadata da che thong tin nhay cam)',
    ip_address         VARCHAR(45)       NULL     COMMENT 'Dia chi IPv4 hoac IPv6 cua client',
    user_agent         VARCHAR(500)      NULL     COMMENT 'Thong tin trinh duyet / thiet bi goi API',
    http_method        VARCHAR(10)       NULL     COMMENT 'GET, POST, PUT, PATCH, DELETE',
    request_uri        VARCHAR(255)      NULL     COMMENT 'Duong dan endpoint API',
    created_date       DATETIME(6)       NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_syslog_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_syslog_user_role CHECK (user_role IS NULL OR user_role IN (0, 1, 2, 3, 4, 5)),
    CONSTRAINT chk_syslog_module CHECK (module BETWEEN 0 AND 12),
    CONSTRAINT chk_syslog_action CHECK (action BETWEEN 0 AND 10),
    CONSTRAINT chk_syslog_status CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index phuc vu phan trang mac dinh theo thoi gian moi nhat
CREATE INDEX idx_syslog_created_date ON system_logs (created_date);

-- Index ghep phuc vu loc theo phan he + hanh dong + khoang thoi gian
CREATE INDEX idx_syslog_module_action_date ON system_logs (module, action, created_date);

-- Index ghep phuc vu tra cuu lich su thao tac cua mot nguoi dung
CREATE INDEX idx_syslog_user_date ON system_logs (user_id, created_date);

-- Index ghep phuc vu xem Audit Trail cua mot chung tu/ban ghi cu the
CREATE INDEX idx_syslog_resource ON system_logs (resource_type, resource_id, created_date);

-- Index ghep phuc vu giam sat cac thao tac that bai / canh bao bao mat
CREATE INDEX idx_syslog_status_date ON system_logs (status, created_date);

-- ===========================================================================
-- Du lieu mau Demo cho bang system_logs (IDs: 10001 - 10010)
-- ===========================================================================

INSERT INTO system_logs (
    id, user_id, username, user_role, module, action, status,
    resource_type, resource_id, resource_code, description, detail_json,
    ip_address, user_agent, http_method, request_uri, created_date
) VALUES
    (10001, 10001, 'demo_admin', 0, 0, 0, 1,
     'USER', 10001, 'demo_admin', 'Người dùng demo_admin đăng nhập hệ thống thành công', '{"username":"demo_admin","role":"ADMIN"}',
     '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'POST', '/api/auth/login', CURRENT_TIMESTAMP(6)),
    (10002, 10002, 'demo_manager', 1, 4, 4, 1,
     'PURCHASE_ORDER', 10001, 'DEMO-PO-001', 'Duyệt đơn đặt hàng mua DEMO-PO-001 từ PENDING sang APPROVED', '{"oldStatus":"PENDING","newStatus":"APPROVED"}',
     '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'PATCH', '/api/purchase-orders/10001/status', CURRENT_TIMESTAMP(6)),
    (10003, 10003, 'demo_warehouse', 3, 5, 2, 1,
     'GOODS_RECEIPT', 10001, 'DEMO-GR-001', 'Tạo phiếu nhập kho DEMO-GR-001 với trạng thái CONFIRMED', '{"purchaseOrderId":10001,"status":"CONFIRMED"}',
     '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'POST', '/api/goods-receipts', CURRENT_TIMESTAMP(6)),
    (10004, 10011, 'demo_customer01', 2, 8, 2, 1,
     'ORDER', 22001, 'DEMO-PAY-COD-01', 'Tạo đơn bán hàng mới DEMO-PAY-COD-01 với tổng thanh toán 4800000', '{"finalAmount":4800000,"type":"ONLINE","paymentMethod":"COD"}',
     '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'POST', '/api/orders', CURRENT_TIMESTAMP(6)),
    (10005, 10005, 'demo_sales', 4, 8, 6, 1,
     'ORDER', 22001, 'DEMO-PAY-COD-01', 'Quét gán serial đóng gói cho đơn hàng DEMO-PAY-COD-01', '{"serialCount":1,"serials":["DEMO-PAY-COD-SN01"]}',
     '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'POST', '/api/orders/22001/assign-serials', CURRENT_TIMESTAMP(6)),
    (10006, 10005, 'demo_sales', 4, 9, 7, 1,
     'PAYMENT', 22002, 'COD-DEMO-PAY-COD-DELIV', 'Xác nhận thu tiền COD cho đơn hàng DEMO-PAY-COD-DELIV', '{"orderId":22002,"orderCode":"DEMO-PAY-COD-DELIV","amount":700000}',
     '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'POST', '/api/payments/22002/cod/confirm', CURRENT_TIMESTAMP(6)),
    (10007, NULL, NULL, NULL, 9, 8, 1,
     'PAYMENT', 22003, '261123_22003_999999', 'Nhận Webhook Callback ZaloPay xác nhận thanh toán thành công cho giao dịch 261123_22003_999999', '{"appTransId":"261123_22003_999999","orderId":22003,"returnCode":1}',
     '113.160.92.10', 'ZaloPay-Webhook/2.0', 'POST', '/api/payments/zalopay/callback', CURRENT_TIMESTAMP(6)),
    (10008, NULL, NULL, NULL, 9, 8, 0,
     'PAYMENT', NULL, NULL, 'Cảnh báo: ZaloPay Webhook Callback có chữ ký MAC không hợp lệ', '{"reason":"mac not equal"}',
     '203.113.152.99', 'curl/8.4.0', 'POST', '/api/payments/zalopay/callback', CURRENT_TIMESTAMP(6)),
    (10009, 10007, 'demo_tech', 5, 11, 3, 1,
     'WARRANTY_REQUEST', 10001, 'DEMO-WR-001', 'Cập nhật xử lý kỹ thuật phiếu bảo hành DEMO-WR-001', '{"status":"IN_PROGRESS","technicianId":10007}',
     '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'PATCH', '/api/admin/warranty-requests/10001/technical', CURRENT_TIMESTAMP(6)),
    (10010, NULL, 'SYSTEM', NULL, 12, 10, 1,
     'REFRESH_TOKEN', NULL, 'RefreshTokenCleanupJob', 'Chạy tác vụ định kỳ dọn dẹp refresh token hết hạn', '{"deletedCount":0}',
     NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP(6));
