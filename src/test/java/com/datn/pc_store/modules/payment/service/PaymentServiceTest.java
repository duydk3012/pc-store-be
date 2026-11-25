package com.datn.pc_store.modules.payment.service;

import com.datn.pc_store.modules.order.entity.Order;
import com.datn.pc_store.modules.order.enums.OrderStatus;
import com.datn.pc_store.modules.order.repository.OrderRepository;
import com.datn.pc_store.modules.payment.config.ZaloPayConfig;
import com.datn.pc_store.modules.payment.dto.CodPaymentRequest;
import com.datn.pc_store.modules.payment.dto.PaymentResponse;
import com.datn.pc_store.modules.payment.dto.ZaloPayCallbackRequest;
import com.datn.pc_store.modules.payment.entity.Payment;
import com.datn.pc_store.modules.payment.entity.PaymentMethodEntity;
import com.datn.pc_store.modules.payment.enums.PaymentMethodStatus;
import com.datn.pc_store.modules.payment.enums.PaymentMethodType;
import com.datn.pc_store.modules.payment.enums.PaymentStatus;
import com.datn.pc_store.modules.payment.repository.PaymentMethodRepository;
import com.datn.pc_store.modules.payment.repository.PaymentRepository;
import com.datn.pc_store.modules.payment.util.ZaloPayUtil;
import com.datn.pc_store.modules.systemlog.event.SystemLogPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ZaloPayConfig zaloPayConfig;

    @Mock
    private SystemLogPublisher systemLogPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private static final String KEY2 = "uUfsWgfLkRLzq6W2uNXTCxrfxs51auny";

    private PaymentMethodEntity codMethod;
    private PaymentMethodEntity zaloPayMethod;

    @BeforeEach
    void setUp() {
        codMethod = PaymentMethodEntity.builder()
                .id(10001L)
                .code("COD")
                .name("Thanh toán khi nhận hàng (COD)")
                .type(PaymentMethodType.CASH)
                .status(PaymentMethodStatus.ACTIVE)
                .build();

        zaloPayMethod = PaymentMethodEntity.builder()
                .id(10002L)
                .code("ZALOPAY")
                .name("Thanh toán trực tuyến ZaloPay")
                .type(PaymentMethodType.ONLINE)
                .bankName("ZaloPay Sandbox")
                .accountNumber("554")
                .status(PaymentMethodStatus.ACTIVE)
                .build();

        lenient().when(zaloPayConfig.getAppId()).thenReturn(554);
        lenient().when(zaloPayConfig.getKey2()).thenReturn(KEY2);
    }

    @Test
    @DisplayName("createCodPayment tạo bản ghi thanh toán COD ở trạng thái PENDING")
    void testCreateCodPayment() {
        Order order = Order.builder()
                .id(1001L)
                .code("DH-20261123-001")
                .userId(10L)
                .finalAmount(2500000L)
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatus(1001L, PaymentStatus.SUCCESS)).thenReturn(false);
        when(paymentMethodRepository.findByCodeIgnoreCase("COD")).thenReturn(Optional.of(codMethod));
        when(paymentRepository.findAllByOrderIdOrderByIdDesc(1001L)).thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(5001L);
            return p;
        });

        PaymentResponse res = paymentService.createCodPayment(new CodPaymentRequest(1001L), 10L, false);

        assertNotNull(res);
        assertEquals(5001L, res.getId());
        assertEquals("COD", res.getPaymentMethodCode());
        assertEquals(2500000L, res.getAmount());
        assertEquals(PaymentStatus.PENDING, res.getStatus());
        assertEquals("COD-DH-20261123-001", res.getTransactionCode());
    }

    @Test
    @DisplayName("handleZaloPayCallback cập nhật trạng thái SUCCESS khi chữ ký Key2 hợp lệ")
    void testHandleZaloPayCallback_Success() {
        String appTransId = "261123_1001_654321";
        String dataJson = "{\"app_id\":554,\"app_trans_id\":\"" + appTransId + "\",\"amount\":2500000,\"server_time\":1700000000000}";
        String mac = ZaloPayUtil.hmacSHA256(KEY2, dataJson);

        Payment pendingPayment = Payment.builder()
                .id(5002L)
                .orderId(1001L)
                .paymentMethodId(zaloPayMethod.getId())
                .paymentMethod(zaloPayMethod)
                .amount(2500000L)
                .transactionCode(appTransId)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByTransactionCode(appTransId)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.findAllByOrderIdOrderByIdDesc(1001L)).thenReturn(List.of(pendingPayment));

        ZaloPayCallbackRequest callbackReq = ZaloPayCallbackRequest.builder()
                .data(dataJson)
                .mac(mac)
                .type(1)
                .build();

        Map<String, Object> result = paymentService.handleZaloPayCallback(callbackReq);

        assertEquals(1, result.get("return_code"));
        assertEquals("success", result.get("return_message"));
        assertEquals(PaymentStatus.SUCCESS, pendingPayment.getStatus());
        assertNotNull(pendingPayment.getPaidAt());
        verify(paymentRepository).save(pendingPayment);
    }

    @Test
    @DisplayName("handleZaloPayCallback trả về -1 khi sai chữ ký Key2")
    void testHandleZaloPayCallback_InvalidMac() {
        ZaloPayCallbackRequest callbackReq = ZaloPayCallbackRequest.builder()
                .data("{\"app_id\":554,\"app_trans_id\":\"261123_1001_654321\"}")
                .mac("wrong_mac_signature")
                .type(1)
                .build();

        Map<String, Object> result = paymentService.handleZaloPayCallback(callbackReq);

        assertEquals(-1, result.get("return_code"));
        assertEquals("mac not equal", result.get("return_message"));
    }
}
