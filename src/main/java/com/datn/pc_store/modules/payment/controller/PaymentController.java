package com.datn.pc_store.modules.payment.controller;

import com.datn.pc_store.common.util.SecurityUtils;
import com.datn.pc_store.modules.payment.dto.*;
import com.datn.pc_store.modules.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments (COD & ZaloPay)", description = "Quản lý thanh toán đơn hàng với 2 phương thức: COD và ZaloPay Sandbox (AppID 554)")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/methods")
    @Operation(summary = "Danh sách phương thức thanh toán",
            description = "Lấy danh sách các phương thức thanh toán đang hoạt động trong hệ thống (COD, ZALOPAY).")
    public ResponseEntity<List<PaymentMethodResponse>> getActiveMethods() {
        return ResponseEntity.ok(paymentService.getActivePaymentMethods());
    }

    @PostMapping("/cod")
    @Operation(summary = "Đăng ký thanh toán khi nhận hàng (COD)",
            description = "Chọn phương thức thanh toán COD cho đơn hàng. Khi đơn hàng giao thành công (COMPLETED), hệ thống tự động ghi nhận đã thanh toán.")
    public ResponseEntity<PaymentResponse> createCodPayment(@Valid @RequestBody CodPaymentRequest request) {
        return ResponseEntity.ok(paymentService.createCodPayment(request, SecurityUtils.getCurrentUserId(), SecurityUtils.isStaffOrAdmin()));
    }

    @PostMapping("/{orderId}/cod/confirm")
    @Operation(summary = "Xác nhận đã thu tiền mặt COD (Nhân viên/Admin)",
            description = "Nhân viên hoặc Quản trị viên xác nhận đã thu tiền COD của đơn hàng.")
    public ResponseEntity<PaymentResponse> confirmCodPayment(
            @Parameter(description = "ID đơn hàng") @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(paymentService.confirmCodPayment(orderId, SecurityUtils.getCurrentUserId(), SecurityUtils.isStaffOrAdmin()));
    }

    @PostMapping("/zalopay/create")
    @Operation(summary = "Tạo giao dịch thanh toán ZaloPay Sandbox",
            description = "Gọi API /v2/create của ZaloPay Sandbox (AppID 554) để sinh link thanh toán (orderUrl) và mã QR cho đơn hàng.")
    public ResponseEntity<ZaloPayCreateResponse> createZaloPayOrder(@Valid @RequestBody ZaloPayCreateRequest request) {
        return ResponseEntity.ok(paymentService.createZaloPayOrder(request, SecurityUtils.getCurrentUserId(), SecurityUtils.isStaffOrAdmin()));
    }

    @PostMapping("/zalopay/callback")
    @Operation(summary = "Webhook Callback từ ZaloPay Server (Server-to-Server)",
            description = "Endpoint public để ZaloPay Server gọi POST thông báo kết quả thanh toán. Xác thực chữ ký MAC bằng Key2.")
    public ResponseEntity<Map<String, Object>> handleZaloPayCallback(@RequestBody ZaloPayCallbackRequest callbackRequest) {
        return ResponseEntity.ok(paymentService.handleZaloPayCallback(callbackRequest));
    }

    @GetMapping("/zalopay/return")
    @Operation(summary = "Xử lý Return/Redirect từ cổng ZaloPay",
            description = "Nhận các query params khi ZaloPay redirect người dùng về (appid, apptransid, status, checksum...), kiểm tra chữ ký Key2 và đồng bộ trạng thái từ ZaloPay /v2/query.")
    public ResponseEntity<ZaloPayStatusResponse> handleZaloPayReturn(
            @Parameter(hidden = true) @RequestParam Map<String, String> queryParams
    ) {
        return ResponseEntity.ok(paymentService.handleZaloPayReturn(queryParams));
    }

    @GetMapping("/zalopay/status/{appTransId}")
    @Operation(summary = "Truy vấn và đồng bộ trạng thái giao dịch ZaloPay theo app_trans_id",
            description = "Chủ động gọi API /v2/query của ZaloPay Sandbox bằng Key1 để kiểm tra và cập nhật trạng thái thanh toán vào DB.")
    public ResponseEntity<ZaloPayStatusResponse> queryZaloPayStatus(
            @Parameter(description = "Mã giao dịch ZaloPay (app_trans_id)", example = "261123_10001_583920")
            @PathVariable String appTransId
    ) {
        return ResponseEntity.ok(paymentService.queryAndSyncZaloPayStatus(appTransId));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Xem lịch sử thanh toán của đơn hàng",
            description = "Trả về danh sách các bản ghi thanh toán của đơn hàng theo ID đơn hàng.")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrder(
            @Parameter(description = "ID đơn hàng") @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(paymentService.getPaymentsByOrderId(orderId, SecurityUtils.getCurrentUserId(), SecurityUtils.isStaffOrAdmin()));
    }
}
