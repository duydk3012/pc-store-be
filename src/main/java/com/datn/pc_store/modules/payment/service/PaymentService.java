package com.datn.pc_store.modules.payment.service;

import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.order.entity.Order;
import com.datn.pc_store.modules.order.entity.OrderItem;
import com.datn.pc_store.modules.order.enums.OrderStatus;
import com.datn.pc_store.modules.order.repository.OrderRepository;
import com.datn.pc_store.modules.payment.config.ZaloPayConfig;
import com.datn.pc_store.modules.payment.dto.*;
import com.datn.pc_store.modules.payment.entity.Payment;
import com.datn.pc_store.modules.payment.entity.PaymentMethodEntity;
import com.datn.pc_store.modules.payment.enums.PaymentMethodCode;
import com.datn.pc_store.modules.payment.enums.PaymentMethodStatus;
import com.datn.pc_store.modules.payment.enums.PaymentMethodType;
import com.datn.pc_store.modules.payment.enums.PaymentStatus;
import com.datn.pc_store.modules.payment.repository.PaymentMethodRepository;
import com.datn.pc_store.modules.payment.repository.PaymentRepository;
import com.datn.pc_store.modules.payment.util.ZaloPayUtil;
import com.datn.pc_store.modules.systemlog.dto.SystemLogCreateCommand;
import com.datn.pc_store.modules.systemlog.enums.SystemLogAction;
import com.datn.pc_store.modules.systemlog.enums.SystemLogModule;
import com.datn.pc_store.modules.systemlog.enums.SystemLogStatus;
import com.datn.pc_store.modules.systemlog.event.SystemLogPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderRepository orderRepository;
    private final ZaloPayConfig zaloPayConfig;
    private final SystemLogPublisher systemLogPublisher;

    // ========================================================================
    // 1. Danh sách phương thức thanh toán (COD & ZALOPAY)
    // ========================================================================

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getActivePaymentMethods() {
        List<PaymentMethodEntity> methods = paymentMethodRepository
                .findAllByStatusOrderByIdAsc(PaymentMethodStatus.ACTIVE);
        return methods.stream()
                .map(this::toMethodResponse)
                .toList();
    }

    // ========================================================================
    // 2. Thanh toán COD (Thanh toán khi nhận hàng)
    // ========================================================================

    /**
     * Khởi tạo bản ghi thanh toán mặc định khi vừa tạo đơn hàng.
     */
    @Transactional
    public Payment initPaymentForNewOrder(Order order, PaymentMethodCode methodCode) {
        PaymentMethodCode effectiveCode = methodCode != null ? methodCode : PaymentMethodCode.COD;
        PaymentMethodEntity method = getOrCreatePaymentMethod(effectiveCode);

        String txCode = effectiveCode == PaymentMethodCode.COD
                ? "COD-" + order.getCode()
                : null;

        Payment payment = Payment.builder()
                .orderId(order.getId())
                .paymentMethodId(method.getId())
                .paymentMethod(method)
                .amount(order.getFinalAmount())
                .transactionCode(txCode)
                .status(PaymentStatus.PENDING)
                .build();

        return paymentRepository.save(payment);
    }

    /**
     * Đăng ký / chuyển phương thức thanh toán của đơn hàng sang COD.
     */
    @Transactional
    public PaymentResponse createCodPayment(CodPaymentRequest request, Long currentUserId, boolean isStaffOrAdmin) {
        Order order = getAndValidateOrderForPayment(request.getOrderId(), currentUserId, isStaffOrAdmin);
        PaymentMethodEntity codMethod = getOrCreatePaymentMethod(PaymentMethodCode.COD);

        List<Payment> existingPayments = paymentRepository.findAllByOrderIdOrderByIdDesc(order.getId());
        Payment targetPayment = null;

        for (Payment p : existingPayments) {
            if (p.getStatus() == PaymentStatus.PENDING) {
                if (targetPayment == null) {
                    targetPayment = p;
                } else {
                    p.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(p);
                }
            }
        }

        String codTxCode = "COD-" + order.getCode();
        if (targetPayment != null) {
            targetPayment.setPaymentMethodId(codMethod.getId());
            targetPayment.setPaymentMethod(codMethod);
            targetPayment.setAmount(order.getFinalAmount());
            targetPayment.setTransactionCode(codTxCode);
            targetPayment.setStatus(PaymentStatus.PENDING);
            targetPayment = paymentRepository.save(targetPayment);
        } else {
            targetPayment = Payment.builder()
                    .orderId(order.getId())
                    .paymentMethodId(codMethod.getId())
                    .paymentMethod(codMethod)
                    .amount(order.getFinalAmount())
                    .transactionCode(codTxCode)
                    .status(PaymentStatus.PENDING)
                    .build();
            targetPayment = paymentRepository.save(targetPayment);
        }

        log.info("Đã đăng ký phương thức thanh toán COD cho đơn hàng id={}, code={}", order.getId(), order.getCode());
        return toPaymentResponse(targetPayment, order.getCode());
    }

    /**
     * Xác nhận đã thu tiền COD cho đơn hàng (dành cho Nhân viên/Admin hoặc khi đơn hàng hoàn tất giao).
     */
    @Transactional
    public PaymentResponse confirmCodPayment(Long orderId, Long currentUserId, boolean isStaffOrAdmin) {
        if (!isStaffOrAdmin) {
            throw new AccessDeniedException("Chỉ Nhân viên hoặc Quản trị viên mới có quyền xác nhận thu tiền COD.");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", orderId));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REJECTED) {
            throw new BadRequestException("Không thể xác nhận thanh toán cho đơn hàng đã hủy hoặc bị từ chối.");
        }

        Payment payment = completePaymentForOrder(order);
        if (systemLogPublisher != null && payment != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .module(SystemLogModule.PAYMENT)
                    .action(SystemLogAction.PAYMENT_CONFIRM)
                    .resourceType("PAYMENT")
                    .resourceId(payment.getId())
                    .resourceCode(payment.getTransactionCode())
                    .description("Xác nhận thu tiền COD cho đơn hàng " + order.getCode())
                    .detailMap(Map.of(
                            "orderId", order.getId(),
                            "orderCode", order.getCode(),
                            "amount", payment.getAmount()
                    ))
                    .build());
        }
        return toPaymentResponse(payment, order.getCode());
    }

    /**
     * Tự động hoàn tất bản ghi thanh toán khi đơn hàng chuyển sang COMPLETED (khách đã nhận hàng và trả tiền).
     */
    @Transactional
    public Payment completePaymentForOrder(Order order) {
        if (order == null || order.getId() == null) {
            return null;
        }

        List<Payment> payments = paymentRepository.findAllByOrderIdOrderByIdDesc(order.getId());
        for (Payment p : payments) {
            if (p.getStatus() == PaymentStatus.SUCCESS) {
                return p; // Đã thanh toán thành công trước đó (ví dụ qua ZaloPay)
            }
        }

        PaymentMethodEntity codMethod = getOrCreatePaymentMethod(PaymentMethodCode.COD);
        Instant now = Instant.now();

        for (Payment p : payments) {
            if (p.getStatus() == PaymentStatus.PENDING) {
                p.setStatus(PaymentStatus.SUCCESS);
                p.setPaidAt(now);
                if (p.getTransactionCode() == null || p.getTransactionCode().isBlank()) {
                    p.setTransactionCode("COD-" + order.getCode());
                }
                Payment saved = paymentRepository.save(p);
                log.info("Đã tự động xác nhận hoàn tất thanh toán id={} cho đơn hàng id={}", saved.getId(), order.getId());
                return saved;
            }
        }

        // Nếu đơn hàng chưa có bản ghi payment nào -> tạo bản ghi COD SUCCESS
        String txCode = "COD-" + order.getCode();
        if (paymentRepository.existsByTransactionCode(txCode)) {
            txCode = "COD-" + order.getCode() + "-" + System.currentTimeMillis() % 100000;
        }
        Payment newPayment = Payment.builder()
                .orderId(order.getId())
                .paymentMethodId(codMethod.getId())
                .paymentMethod(codMethod)
                .amount(order.getFinalAmount())
                .transactionCode(txCode)
                .status(PaymentStatus.SUCCESS)
                .paidAt(now)
                .build();

        Payment saved = paymentRepository.save(newPayment);
        log.info("Đã tạo bản ghi thanh toán COD hoàn tất id={} cho đơn hàng id={}", saved.getId(), order.getId());
        return saved;
    }

    /**
     * Hủy các giao dịch thanh toán đang chờ (PENDING -> FAILED) khi đơn hàng bị hủy/từ chối.
     */
    @Transactional
    public void cancelPendingPaymentsForOrder(Long orderId) {
        if (orderId == null) return;
        List<Payment> payments = paymentRepository.findAllByOrderIdOrderByIdDesc(orderId);
        for (Payment p : payments) {
            if (p.getStatus() == PaymentStatus.PENDING) {
                p.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(p);
            }
        }
    }

    // ========================================================================
    // 3. Thanh toán trực tuyến ZaloPay Sandbox (/v2/create)
    // ========================================================================

    /**
     * Tạo giao dịch thanh toán trên cổng ZaloPay Sandbox (AppID 554).
     */
    @Transactional
    public ZaloPayCreateResponse createZaloPayOrder(ZaloPayCreateRequest request, Long currentUserId, boolean isStaffOrAdmin) {
        Order order = getAndValidateOrderForPayment(request.getOrderId(), currentUserId, isStaffOrAdmin);
        PaymentMethodEntity zaloPayMethod = getOrCreatePaymentMethod(PaymentMethodCode.ZALOPAY);

        int appId = zaloPayConfig.getAppId();
        String appTransId = ZaloPayUtil.generateAppTransId(order.getId());
        String appUser = "user_" + order.getUserId();
        long appTime = System.currentTimeMillis();
        long amount = order.getFinalAmount();

        String redirectUrl = (request.getRedirectUrl() != null && !request.getRedirectUrl().isBlank())
                ? request.getRedirectUrl().trim()
                : zaloPayConfig.getRedirectUrl();

        String embedDataJson = buildEmbedDataJson(redirectUrl, order);
        String itemJson = buildOrderItemsJson(order);
        String description = "PC Store - Thanh toan don hang #" + order.getCode();
        String bankCode = (request.getBankCode() != null) ? request.getBankCode().trim() : "";

        String mac = ZaloPayUtil.buildCreateOrderMac(
                appId, appTransId, appUser, amount, appTime, embedDataJson, itemJson, zaloPayConfig.getKey1()
        );

        Map<String, String> formParams = new LinkedHashMap<>();
        formParams.put("app_id", String.valueOf(appId));
        formParams.put("app_user", appUser);
        formParams.put("app_trans_id", appTransId);
        formParams.put("app_time", String.valueOf(appTime));
        formParams.put("amount", String.valueOf(amount));
        formParams.put("item", itemJson);
        formParams.put("description", description);
        formParams.put("embed_data", embedDataJson);
        formParams.put("bank_code", bankCode);
        formParams.put("mac", mac);

        if (zaloPayConfig.getCallbackUrl() != null && !zaloPayConfig.getCallbackUrl().isBlank()) {
            formParams.put("callback_url", zaloPayConfig.getCallbackUrl().trim());
        }

        JsonNode zaloResponse = postFormToZaloPay(zaloPayConfig.getCreateUrl(), formParams);
        int returnCode = zaloResponse.path("return_code").asInt(-1);
        String returnMessage = zaloResponse.path("return_message").asText("Không rõ lỗi từ ZaloPay");
        String subReturnMessage = zaloResponse.path("sub_return_message").asText("");

        if (returnCode != 1) {
            String detail = subReturnMessage.isBlank() ? returnMessage : (returnMessage + " (" + subReturnMessage + ")");
            log.warn("Tạo giao dịch ZaloPay thất bại cho đơn hàng id={}, returnCode={}, msg={}",
                    order.getId(), returnCode, detail);
            throw new BadRequestException("Không thể khởi tạo giao dịch ZaloPay: " + detail);
        }

        String orderUrl = zaloResponse.path("order_url").asText(null);
        String zpTransToken = zaloResponse.path("zp_trans_token").asText(null);
        String qrCode = zaloResponse.path("qr_code").asText(null);

        // Nếu đơn hàng có bản ghi PENDING COD mặc định chưa có mã giao dịch ZaloPay, cập nhật hoặc tạo mới
        Payment payment = Payment.builder()
                .orderId(order.getId())
                .paymentMethodId(zaloPayMethod.getId())
                .paymentMethod(zaloPayMethod)
                .amount(amount)
                .transactionCode(appTransId)
                .status(PaymentStatus.PENDING)
                .build();
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Đã tạo giao dịch ZaloPay thành công cho đơn hàng id={}, appTransId={}, paymentId={}",
                order.getId(), appTransId, savedPayment.getId());

        return ZaloPayCreateResponse.builder()
                .paymentId(savedPayment.getId())
                .orderId(order.getId())
                .orderCode(order.getCode())
                .appTransId(appTransId)
                .amount(amount)
                .orderUrl(orderUrl)
                .zpTransToken(zpTransToken)
                .qrCode(qrCode)
                .returnCode(returnCode)
                .returnMessage(returnMessage)
                .build();
    }

    // ========================================================================
    // 4. ZaloPay Webhook Callback (Server-to-Server, xác thực bằng Key2)
    // ========================================================================

    /**
     * Xử lý callback POST từ ZaloPay Server khi khách hàng thanh toán thành công.
     * Phản hồi theo chuẩn ZaloPay: {"return_code": 1, "return_message": "success"}
     */
    @Transactional
    public Map<String, Object> handleZaloPayCallback(ZaloPayCallbackRequest callbackRequest) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (callbackRequest == null || callbackRequest.getData() == null || callbackRequest.getMac() == null) {
            result.put("return_code", -1);
            result.put("return_message", "invalid callback payload");
            return result;
        }

        boolean validMac = ZaloPayUtil.verifyCallbackMac(
                callbackRequest.getData(),
                callbackRequest.getMac(),
                zaloPayConfig.getKey2()
        );

        if (!validMac) {
            log.warn("ZaloPay Callback: Chữ ký MAC không hợp lệ!");
            if (systemLogPublisher != null) {
                systemLogPublisher.publish(SystemLogCreateCommand.builder()
                        .module(SystemLogModule.PAYMENT)
                        .action(SystemLogAction.PAYMENT_WEBHOOK)
                        .status(SystemLogStatus.FAILED)
                        .resourceType("PAYMENT")
                        .description("Cảnh báo: ZaloPay Webhook Callback có chữ ký MAC không hợp lệ")
                        .detailMap(Map.of("reason", "mac not equal"))
                        .build());
            }
            result.put("return_code", -1);
            result.put("return_message", "mac not equal");
            return result;
        }

        try {
            JsonNode dataNode = OBJECT_MAPPER.readTree(callbackRequest.getData());
            String appTransId = dataNode.path("app_trans_id").asText(null);
            long serverTime = dataNode.path("server_time").asLong(0L);

            if (appTransId == null || appTransId.isBlank()) {
                result.put("return_code", 0);
                result.put("return_message", "missing app_trans_id");
                return result;
            }

            Optional<Payment> paymentOpt = paymentRepository.findByTransactionCode(appTransId);
            if (paymentOpt.isEmpty()) {
                log.warn("ZaloPay Callback: Không tìm thấy bản ghi payment với app_trans_id={}", appTransId);
                result.put("return_code", 0);
                result.put("return_message", "payment not found");
                return result;
            }

            Payment payment = paymentOpt.get();
            Instant paidAt = serverTime > 0 ? Instant.ofEpochMilli(serverTime) : Instant.now();
            markPaymentSuccessAndCancelOtherPending(payment, paidAt);

            log.info("ZaloPay Callback: Xác nhận thanh toán thành công cho app_trans_id={}, orderId={}",
                    appTransId, payment.getOrderId());
            if (systemLogPublisher != null) {
                systemLogPublisher.publish(SystemLogCreateCommand.builder()
                        .module(SystemLogModule.PAYMENT)
                        .action(SystemLogAction.PAYMENT_WEBHOOK)
                        .status(SystemLogStatus.SUCCESS)
                        .resourceType("PAYMENT")
                        .resourceId(payment.getId())
                        .resourceCode(appTransId)
                        .description("Nhận Webhook Callback ZaloPay xác nhận thanh toán thành công cho giao dịch " + appTransId)
                        .detailMap(Map.of(
                                "appTransId", appTransId,
                                "orderId", payment.getOrderId(),
                                "amount", payment.getAmount()
                        ))
                        .build());
            }

            result.put("return_code", 1);
            result.put("return_message", "success");
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi xử lý ZaloPay Callback: {}", e.getMessage(), e);
            result.put("return_code", 0);
            result.put("return_message", e.getMessage());
            return result;
        }
    }

    // ========================================================================
    // 5. Truy vấn trạng thái giao dịch ZaloPay (/v2/query) & Return URL
    // ========================================================================

    /**
     * Xử lý khi ZaloPay redirect người dùng về Return URL (kèm apptransid, status, checksum...)
     * và chủ động truy vấn API /v2/query của ZaloPay để đồng bộ trạng thái chính xác vào DB.
     */
    @Transactional
    public ZaloPayStatusResponse handleZaloPayReturn(Map<String, String> queryParams) {
        String appTransId = queryParams.getOrDefault("apptransid", queryParams.get("app_trans_id"));
        if (appTransId == null || appTransId.isBlank()) {
            throw new BadRequestException("Thiếu mã giao dịch apptransid từ ZaloPay.");
        }

        Boolean checksumValid = null;
        if (queryParams.containsKey("checksum")) {
            checksumValid = ZaloPayUtil.verifyRedirectChecksum(queryParams, zaloPayConfig.getKey2());
            if (!checksumValid) {
                log.warn("ZaloPay Return: Checksum trên URL redirect không khớp cho apptransid={}", appTransId);
            }
        }

        ZaloPayStatusResponse statusResponse = queryAndSyncZaloPayStatus(appTransId);
        statusResponse.setChecksumValid(checksumValid);
        return statusResponse;
    }

    /**
     * Chủ động gọi API /v2/query của ZaloPay Sandbox để kiểm tra trạng thái giao dịch
     * và đồng bộ trạng thái bản ghi Payment trong cơ sở dữ liệu.
     */
    @Transactional
    public ZaloPayStatusResponse queryAndSyncZaloPayStatus(String appTransId) {
        if (appTransId == null || appTransId.isBlank()) {
            throw new BadRequestException("Mã giao dịch app_trans_id không được để trống.");
        }

        Payment payment = paymentRepository.findByTransactionCode(appTransId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Giao dịch thanh toán", "appTransId", appTransId));

        Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
        String orderCode = order != null ? order.getCode() : null;

        // Nếu giao dịch trong DB đã SUCCESS, trả kết quả luôn không cần gọi lại hoặc vẫn trả thông tin đầy đủ
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return ZaloPayStatusResponse.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .orderCode(orderCode)
                    .appTransId(payment.getTransactionCode())
                    .amount(payment.getAmount())
                    .paymentStatus(PaymentStatus.SUCCESS)
                    .returnCode(1)
                    .returnMessage("Giao dịch đã được thanh toán thành công.")
                    .processing(false)
                    .paid(true)
                    .paidAt(payment.getPaidAt())
                    .build();
        }

        int appId = zaloPayConfig.getAppId();
        String mac = ZaloPayUtil.buildQueryOrderMac(appId, payment.getTransactionCode(), zaloPayConfig.getKey1());

        Map<String, String> formParams = new LinkedHashMap<>();
        formParams.put("app_id", String.valueOf(appId));
        formParams.put("app_trans_id", payment.getTransactionCode());
        formParams.put("mac", mac);

        JsonNode zaloResponse = postFormToZaloPay(zaloPayConfig.getQueryUrl(), formParams);
        int returnCode = zaloResponse.path("return_code").asInt(3);
        String returnMessage = zaloResponse.path("return_message").asText("");
        boolean isProcessing = zaloResponse.path("is_processing").asBoolean(false);
        long zpTransId = zaloResponse.path("zp_trans_id").asLong(0L);

        if (returnCode == 1) {
            // Giao dịch thành công trên ZaloPay -> cập nhật DB sang SUCCESS
            markPaymentSuccessAndCancelOtherPending(payment, Instant.now());
            log.info("Đồng bộ ZaloPay Query: Thanh toán thành công cho appTransId={}, orderId={}",
                    payment.getTransactionCode(), payment.getOrderId());
        } else if (returnCode == 2 && !isProcessing) {
            // Giao dịch thất bại trên ZaloPay
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.info("Đồng bộ ZaloPay Query: Giao dịch thất bại cho appTransId={}, returnMessage={}",
                    payment.getTransactionCode(), returnMessage);
        }

        return ZaloPayStatusResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .orderCode(orderCode)
                .appTransId(payment.getTransactionCode())
                .zpTransId(zpTransId > 0 ? zpTransId : null)
                .amount(payment.getAmount())
                .paymentStatus(payment.getStatus())
                .returnCode(returnCode)
                .returnMessage(returnMessage)
                .processing(isProcessing)
                .paid(payment.getStatus() == PaymentStatus.SUCCESS)
                .paidAt(payment.getPaidAt())
                .build();
    }

    // ========================================================================
    // 6. Tra cứu lịch sử thanh toán của đơn hàng
    // ========================================================================

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrderId(Long orderId, Long currentUserId, boolean isStaffOrAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", orderId));

        if (!isStaffOrAdmin && !order.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không có quyền xem thông tin thanh toán của đơn hàng này.");
        }

        return paymentRepository.findAllByOrderIdOrderByIdDesc(orderId)
                .stream()
                .map(p -> toPaymentResponse(p, order.getCode()))
                .toList();
    }

    // ========================================================================
    // Private helpers
    // ========================================================================

    private Order getAndValidateOrderForPayment(Long orderId, Long currentUserId, boolean isStaffOrAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", orderId));

        if (!isStaffOrAdmin && !order.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thanh toán cho đơn hàng này.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.REJECTED
                || order.getStatus() == OrderStatus.RETURNED) {
            throw new BadRequestException("Đơn hàng ở trạng thái '" + order.getStatus() + "' không thể thanh toán.");
        }

        if (paymentRepository.existsByOrderIdAndStatus(order.getId(), PaymentStatus.SUCCESS)) {
            throw new BadRequestException("Đơn hàng này đã được thanh toán thành công.");
        }

        return order;
    }

    private void markPaymentSuccessAndCancelOtherPending(Payment targetPayment, Instant paidAt) {
        if (targetPayment.getStatus() != PaymentStatus.SUCCESS) {
            targetPayment.setStatus(PaymentStatus.SUCCESS);
            targetPayment.setPaidAt(paidAt != null ? paidAt : Instant.now());
            paymentRepository.save(targetPayment);
        }

        List<Payment> allForOrder = paymentRepository.findAllByOrderIdOrderByIdDesc(targetPayment.getOrderId());
        for (Payment other : allForOrder) {
            if (!other.getId().equals(targetPayment.getId()) && other.getStatus() == PaymentStatus.PENDING) {
                other.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(other);
            }
        }
    }

    private PaymentMethodEntity getOrCreatePaymentMethod(PaymentMethodCode code) {
        Optional<PaymentMethodEntity> existing = paymentMethodRepository.findByCodeIgnoreCase(code.name());
        if (existing.isPresent()) {
            return existing.get();
        }

        PaymentMethodEntity created;
        if (code == PaymentMethodCode.COD) {
            created = PaymentMethodEntity.builder()
                    .code("COD")
                    .name("Thanh toán khi nhận hàng (COD)")
                    .type(PaymentMethodType.CASH)
                    .status(PaymentMethodStatus.ACTIVE)
                    .isDeleted(false)
                    .build();
        } else {
            created = PaymentMethodEntity.builder()
                    .code("ZALOPAY")
                    .name("Thanh toán trực tuyến ZaloPay")
                    .type(PaymentMethodType.ONLINE)
                    .bankName("ZaloPay Sandbox")
                    .accountNumber(String.valueOf(zaloPayConfig.getAppId()))
                    .status(PaymentMethodStatus.ACTIVE)
                    .isDeleted(false)
                    .build();
        }
        return paymentMethodRepository.save(created);
    }

    private String buildEmbedDataJson(String redirectUrl, Order order) {
        try {
            Map<String, Object> embed = new LinkedHashMap<>();
            if (redirectUrl != null && !redirectUrl.isBlank()) {
                embed.put("redirecturl", redirectUrl);
            }
            embed.put("orderId", order.getId());
            embed.put("orderCode", order.getCode());
            return OBJECT_MAPPER.writeValueAsString(embed);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildOrderItemsJson(Order order) {
        try {
            List<Map<String, Object>> items = new ArrayList<>();
            if (order.getItems() != null) {
                for (OrderItem oi : order.getItems()) {
                    Map<String, Object> itemMap = new LinkedHashMap<>();
                    itemMap.put("itemid", String.valueOf(oi.getProductId()));
                    itemMap.put("itemname", oi.getProduct() != null ? oi.getProduct().getName() : ("SP-" + oi.getProductId()));
                    itemMap.put("itemprice", oi.getUnitPrice());
                    itemMap.put("itemquantity", oi.getQuantity());
                    items.add(itemMap);
                }
            }
            return OBJECT_MAPPER.writeValueAsString(items);
        } catch (Exception e) {
            return "[]";
        }
    }

    protected JsonNode postFormToZaloPay(String url, Map<String, String> formParams) {
        try {
            StringBuilder formBody = new StringBuilder();
            for (Map.Entry<String, String> entry : formParams.entrySet()) {
                if (entry.getValue() == null) continue;
                if (!formBody.isEmpty()) {
                    formBody.append('&');
                }
                formBody.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return OBJECT_MAPPER.readTree(response.body());
        } catch (Exception e) {
            log.error("Lỗi kết nối tới cổng ZaloPay ({}): {}", url, e.getMessage(), e);
            throw new BadRequestException("Không thể kết nối tới cổng thanh toán ZaloPay: " + e.getMessage());
        }
    }

    private PaymentMethodResponse toMethodResponse(PaymentMethodEntity entity) {
        return PaymentMethodResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .type(entity.getType())
                .bankName(entity.getBankName())
                .accountNumber(entity.getAccountNumber())
                .status(entity.getStatus())
                .build();
    }

    private PaymentResponse toPaymentResponse(Payment payment, String orderCode) {
        PaymentMethodEntity pm = payment.getPaymentMethod();
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .orderCode(orderCode)
                .paymentMethodId(payment.getPaymentMethodId())
                .paymentMethodCode(pm != null ? pm.getCode() : null)
                .paymentMethodName(pm != null ? pm.getName() : null)
                .amount(payment.getAmount())
                .transactionCode(payment.getTransactionCode())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .createdDate(payment.getCreatedDate())
                .lastModifiedDate(payment.getLastModifiedDate())
                .build();
    }
}
