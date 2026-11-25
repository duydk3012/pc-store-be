package com.datn.pc_store.modules.order.service;

import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.modules.delivery.service.DeliveryService;
import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.order.dto.OrderResponse;
import com.datn.pc_store.modules.order.entity.Order;
import com.datn.pc_store.modules.order.entity.OrderItem;
import com.datn.pc_store.modules.order.entity.OrderItemSerial;
import com.datn.pc_store.modules.order.enums.OrderStatus;
import com.datn.pc_store.modules.order.mapper.OrderMapper;
import com.datn.pc_store.modules.order.repository.OrderItemSerialRepository;
import com.datn.pc_store.modules.order.repository.OrderRepository;
import com.datn.pc_store.modules.payment.repository.PaymentRepository;
import com.datn.pc_store.modules.payment.service.PaymentService;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.product.service.ProductStockService;
import com.datn.pc_store.modules.systemlog.event.SystemLogPublisher;
import com.datn.pc_store.modules.user.repository.UserRepository;
import com.datn.pc_store.modules.warranty.service.WarrantyActivationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private DeliveryService deliveryService;
    @Mock private OrderItemSerialRepository orderItemSerialRepository;
    @Mock private ProductSerialRepository productSerialRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderSerialService orderSerialService;
    @Mock private WarrantyActivationService warrantyActivationService;
    @Mock private InvoiceService invoiceService;
    @Mock private PaymentService paymentService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private SystemLogPublisher systemLogPublisher;
    @Mock private ProductStockService productStockService;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("updateStatus chặn chuyển trạng thái sai quy trình (CANCELLED -> CONFIRMED)")
    void updateStatus_invalidTransition_throwsBadRequest() {
        Order order = Order.builder()
                .id(10L)
                .code("DH-10")
                .userId(100L)
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderRepository.findForUpdate(10L)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.updateStatus(10L, OrderStatus.CONFIRMED, 1L, true));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("handoverToCarrier ủy quyền chuyển trạng thái serial qua OrderSerialService")
    void handoverToCarrier_delegatesToOrderSerialService() {
        Order order = Order.builder()
                .id(11L)
                .code("DH-11")
                .userId(100L)
                .status(OrderStatus.DELIVERING)
                .build();

        when(orderRepository.findById(11L)).thenReturn(Optional.of(order));
        when(orderItemSerialRepository.findAllByOrderId(11L)).thenReturn(List.of());
        when(paymentRepository.findFirstByOrderIdOrderByIdDesc(11L)).thenReturn(Optional.empty());
        when(orderMapper.toResponse(eq(order), anyMap(), isNull())).thenReturn(OrderResponse.builder().id(11L).build());

        OrderResponse res = orderService.handoverToCarrier(11L);

        assertEquals(11L, res.getId());
        verify(orderSerialService).markDelivering(11L);
    }

    @Test
    @DisplayName("TC-09a: assignSerialsToOrder chặn quét serial nếu đơn hàng đã CANCELLED hoặc COMPLETED")
    void assignSerialsToOrder_invalidOrderStatus_throwsBadRequest() {
        Order order = Order.builder()
                .id(20L)
                .code("DH-20")
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderRepository.findForUpdate(20L)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.assignSerialsToOrder(20L, Map.of(1L, List.of("SN-01"))));
    }

    @Test
    @DisplayName("TC-09b: assignSerialsToOrder chặn khi số lượng serial quét không khớp quantity đặt mua")
    void assignSerialsToOrder_mismatchedQuantity_throwsBadRequest() {
        Order order = Order.builder()
                .id(21L)
                .code("DH-21")
                .status(OrderStatus.CONFIRMED)
                .build();
        OrderItem item = OrderItem.builder().id(101L).order(order).productId(10003L).quantity(2).build();
        order.setItems(List.of(item));

        when(orderRepository.findForUpdate(21L)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.assignSerialsToOrder(21L, Map.of(101L, List.of("SN-01"))));
    }

    @Test
    @DisplayName("TC-10a: Đơn nhiều dòng hàng mới quét 1 dòng thì giữ nguyên CONFIRMED")
    void assignSerialsToOrder_partialScanInMultiItemOrder_keepsConfirmed() {
        Order order = Order.builder()
                .id(22L)
                .code("DH-22")
                .status(OrderStatus.CONFIRMED)
                .build();
        OrderItem item1 = OrderItem.builder().id(201L).order(order).productId(10003L).quantity(1).build();
        OrderItem item2 = OrderItem.builder().id(202L).order(order).productId(10004L).quantity(1).build();
        order.setItems(List.of(item1, item2));

        ProductSerial ps1 = ProductSerial.builder()
                .id(301L).productId(10003L).serialNumber("SN-201").status(ProductSerialStatus.IN_STOCK).isTerminated(false).build();

        when(orderRepository.findForUpdate(22L)).thenReturn(Optional.of(order));
        when(orderItemSerialRepository.findAllByOrderItemId(201L)).thenReturn(List.of());
        when(productSerialRepository.lockActiveBySerialNumbers(anyCollection())).thenReturn(List.of(ps1));
        when(orderItemSerialRepository.findAllByOrderId(22L)).thenReturn(List.of(
                OrderItemSerial.builder().orderItemId(201L).productSerialId(301L).productSerial(ps1).build()
        ));
        when(paymentRepository.findFirstByOrderIdOrderByIdDesc(22L)).thenReturn(Optional.empty());
        when(orderMapper.toResponse(eq(order), anyMap(), isNull()))
                .thenReturn(OrderResponse.builder().id(22L).status(OrderStatus.CONFIRMED).build());

        OrderResponse res = orderService.assignSerialsToOrder(22L, Map.of(201L, List.of("SN-201")));

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(ProductSerialStatus.PACKING, ps1.getStatus());
        assertEquals(OrderStatus.CONFIRMED, res.getStatus());
        verify(productStockService).syncProductStockStatus(anyCollection());
    }

    @Test
    @DisplayName("TC-10b: Quét đủ serial toàn đơn -> Serial PACKING, Order DELIVERING, đồng bộ stock")
    void assignSerialsToOrder_fullScan_transitionsToDelivering() {
        Order order = Order.builder()
                .id(23L)
                .code("DH-23")
                .status(OrderStatus.CONFIRMED)
                .build();
        OrderItem item1 = OrderItem.builder().id(203L).order(order).productId(10003L).quantity(2).build();
        order.setItems(List.of(item1));

        ProductSerial ps1 = ProductSerial.builder()
                .id(303L).productId(10003L).serialNumber("SN-303").status(ProductSerialStatus.IN_STOCK).isTerminated(false).build();
        ProductSerial ps2 = ProductSerial.builder()
                .id(304L).productId(10003L).serialNumber("SN-304").status(ProductSerialStatus.IN_STOCK).isTerminated(false).build();

        when(orderRepository.findForUpdate(23L)).thenReturn(Optional.of(order));
        when(orderItemSerialRepository.findAllByOrderItemId(203L)).thenReturn(List.of());
        when(productSerialRepository.lockActiveBySerialNumbers(anyCollection())).thenReturn(List.of(ps1, ps2));
        when(orderItemSerialRepository.findAllByOrderId(23L)).thenReturn(List.of(
                OrderItemSerial.builder().orderItemId(203L).productSerialId(303L).productSerial(ps1).build(),
                OrderItemSerial.builder().orderItemId(203L).productSerialId(304L).productSerial(ps2).build()
        ));
        when(paymentRepository.findFirstByOrderIdOrderByIdDesc(23L)).thenReturn(Optional.empty());
        when(orderMapper.toResponse(eq(order), anyMap(), isNull()))
                .thenReturn(OrderResponse.builder().id(23L).status(OrderStatus.DELIVERING).build());

        OrderResponse res = orderService.assignSerialsToOrder(23L, Map.of(203L, List.of("SN-303", "SN-304")));

        assertEquals(OrderStatus.DELIVERING, order.getStatus());
        assertEquals(ProductSerialStatus.PACKING, ps1.getStatus());
        assertEquals(ProductSerialStatus.PACKING, ps2.getStatus());
        assertEquals(OrderStatus.DELIVERING, res.getStatus());
        verify(orderRepository).saveAndFlush(order);
        verify(productStockService).syncProductStockStatus(anyCollection());
    }
}
