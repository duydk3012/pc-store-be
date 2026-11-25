package com.datn.pc_store.modules.payment.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ZaloPayUtilTest {

    private static final int APP_ID = 554;
    private static final String KEY1 = "8NdU5pG5R2spGHGhyO99HN1OhD8IQJBn";
    private static final String KEY2 = "uUfsWgfLkRLzq6W2uNXTCxrfxs51auny";

    @Test
    @DisplayName("hmacSHA256 sinh chuỗi hex 64 ký tự chuẩn")
    void testHmacSHA256() {
        String mac = ZaloPayUtil.hmacSHA256(KEY1, "554|261123_10001_123456|user_10|500000|1700000000000|{}|[]");
        assertNotNull(mac);
        assertEquals(64, mac.length());
        assertTrue(mac.matches("^[0-9a-f]{64}$"));
    }

    @Test
    @DisplayName("generateAppTransId sinh mã giao dịch đúng chuẩn yyMMdd_orderId_random")
    void testGenerateAppTransId() {
        String appTransId = ZaloPayUtil.generateAppTransId(10001L);
        assertNotNull(appTransId);
        assertTrue(appTransId.matches("^\\d{6}_10001_\\d{6}$"), "Định dạng phải là yyMMdd_10001_xxxxxx");
    }

    @Test
    @DisplayName("verifyCallbackMac xác thực chữ ký Callback bằng Key2 chính xác")
    void testVerifyCallbackMac() {
        String callbackData = "{\"app_id\":554,\"app_trans_id\":\"261123_10001_123456\",\"amount\":500000}";
        String validMac = ZaloPayUtil.hmacSHA256(KEY2, callbackData);

        assertTrue(ZaloPayUtil.verifyCallbackMac(callbackData, validMac, KEY2));
        assertFalse(ZaloPayUtil.verifyCallbackMac(callbackData, "invalid_mac", KEY2));
    }

    @Test
    @DisplayName("verifyRedirectChecksum xác thực chữ ký Redirect URL bằng Key2 chính xác")
    void testVerifyRedirectChecksum() {
        Map<String, String> params = new HashMap<>();
        params.put("appid", String.valueOf(APP_ID));
        params.put("apptransid", "261123_10001_123456");
        params.put("pmcid", "38");
        params.put("bankcode", "");
        params.put("amount", "500000");
        params.put("discountamount", "0");
        params.put("status", "1");

        String raw = "554|261123_10001_123456|38||500000|0|1";
        params.put("checksum", ZaloPayUtil.hmacSHA256(KEY2, raw));

        assertTrue(ZaloPayUtil.verifyRedirectChecksum(params, KEY2));

        params.put("amount", "1000"); // Giả lập sửa số tiền
        assertFalse(ZaloPayUtil.verifyRedirectChecksum(params, KEY2));
    }
}
