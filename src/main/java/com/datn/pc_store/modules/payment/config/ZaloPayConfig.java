package com.datn.pc_store.modules.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.zalopay")
@Getter
@Setter
public class ZaloPayConfig {

    /**
     * AppID tích hợp ZaloPay Sandbox (mặc định: 554)
     */
    private int appId = 554;

    /**
     * Key1 dùng để tạo chữ ký MAC khi gọi API Tạo đơn (/v2/create) và Truy vấn trạng thái (/v2/query)
     */
    private String key1 = "";

    /**
     * Key2 dùng để xác thực chữ ký MAC từ ZaloPay Callback (Webhook) và Redirect Checksum
     */
    private String key2 = "";

    /**
     * Endpoint tạo đơn thanh toán ZaloPay Sandbox
     */
    private String createUrl = "https://sb-openapi.zalopay.vn/v2/create";

    /**
     * Endpoint truy vấn trạng thái thanh toán ZaloPay Sandbox
     */
    private String queryUrl = "https://sb-openapi.zalopay.vn/v2/query";

    /**
     * URL nhận callback Server-to-Server từ ZaloPay (tùy chọn nếu dùng ngrok/public domain)
     */
    private String callbackUrl = "";

    /**
     * URL điều hướng người dùng về Frontend sau khi thanh toán trên cổng ZaloPay
     */
    private String redirectUrl = "http://localhost:5173/payment/zalopay-return";
}
