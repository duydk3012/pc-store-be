package com.datn.pc_store.modules.payment.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Tiện ích mã hóa HMAC-SHA256 và định dạng dữ liệu chuẩn ZaloPay API v2.
 */
public final class ZaloPayUtil {

    private static final DateTimeFormatter YY_MM_DD_FMT =
            DateTimeFormatter.ofPattern("yyMMdd");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private ZaloPayUtil() {}

    /**
     * Tính mã băm HMAC-SHA256 trả về chuỗi hex chữ thường (64 ký tự).
     */
    public static String hmacSHA256(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi khi tạo chữ ký HMAC-SHA256 cho ZaloPay", e);
        }
    }

    /**
     * Sinh mã giao dịch chuẩn ZaloPay: yyMMdd_orderId_random6digits (theo múi giờ GMT+7).
     */
    public static String generateAppTransId(Long orderId) {
        String datePrefix = ZonedDateTime.now(VN_ZONE).format(YY_MM_DD_FMT);
        int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return datePrefix + "_" + orderId + "_" + random;
    }

    /**
     * Tạo chữ ký MAC cho API tạo đơn ZaloPay (/v2/create) bằng Key1.
     * Công thức: hmac_input = app_id + "|" + app_trans_id + "|" + app_user + "|" + amount + "|" + app_time + "|" + embed_data + "|" + item
     */
    public static String buildCreateOrderMac(
            int appId,
            String appTransId,
            String appUser,
            long amount,
            long appTime,
            String embedData,
            String item,
            String key1
    ) {
        String data = appId + "|"
                + appTransId + "|"
                + appUser + "|"
                + amount + "|"
                + appTime + "|"
                + embedData + "|"
                + item;
        return hmacSHA256(key1, data);
    }

    /**
     * Tạo chữ ký MAC cho API truy vấn trạng thái đơn ZaloPay (/v2/query) bằng Key1.
     * Công thức: hmac_input = app_id + "|" + app_trans_id + "|" + key1
     */
    public static String buildQueryOrderMac(int appId, String appTransId, String key1) {
        String data = appId + "|" + appTransId + "|" + key1;
        return hmacSHA256(key1, data);
    }

    /**
     * Xác thực chữ ký MAC từ Webhook Callback của ZaloPay bằng Key2.
     */
    public static boolean verifyCallbackMac(String data, String requestMac, String key2) {
        if (data == null || requestMac == null || requestMac.isBlank() || key2 == null) {
            return false;
        }
        String computed = hmacSHA256(key2, data);
        return computed.equalsIgnoreCase(requestMac.trim());
    }

    /**
     * Xác thực chữ ký checksum khi ZaloPay redirect người dùng về Redirect URL bằng Key2.
     * Công thức: checksumData = appid + "|" + apptransid + "|" + pmcid + "|" + bankcode + "|" + amount + "|" + discountamount + "|" + status
     */
    public static boolean verifyRedirectChecksum(Map<String, String> params, String key2) {
        if (params == null || key2 == null) {
            return false;
        }
        String checksum = params.get("checksum");
        if (checksum == null || checksum.isBlank()) {
            return false;
        }
        String appid = params.getOrDefault("appid", "");
        String apptransid = params.getOrDefault("apptransid", "");
        String pmcid = params.getOrDefault("pmcid", "");
        String bankcode = params.getOrDefault("bankcode", "");
        String amount = params.getOrDefault("amount", "");
        String discountamount = params.getOrDefault("discountamount", "");
        String status = params.getOrDefault("status", "");

        String data = appid + "|"
                + apptransid + "|"
                + pmcid + "|"
                + bankcode + "|"
                + amount + "|"
                + discountamount + "|"
                + status;
        String computed = hmacSHA256(key2, data);
        return computed.equalsIgnoreCase(checksum.trim());
    }
}
