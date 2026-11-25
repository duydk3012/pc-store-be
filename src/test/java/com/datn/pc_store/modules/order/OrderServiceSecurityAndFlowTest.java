package com.datn.pc_store.modules.order;

import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.modules.delivery.enums.DeliveryResourceType;
import com.datn.pc_store.modules.delivery.service.DeliveryService;
import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.order.dto.OrderCreateRequest;
import com.datn.pc_store.modules.order.dto.OrderItemRequest;
import com.datn.pc_store.modules.order.dto.OrderResponse;
import com.datn.pc_store.modules.order.entity.Order;
import com.datn.pc_store.modules.order.entity.OrderItem;
import com.datn.pc_store.modules.order.enums.OrderStatus;
import com.datn.pc_store.modules.order.enums.OrderType;
import com.datn.pc_store.modules.order.mapper.OrderMapper;
import com.datn.pc_store.modules.order.repository.OrderItemSerialRepository;
import com.datn.pc_store.modules.order.repository.OrderRepository;
import com.datn.pc_store.modules.order.service.InvoiceService;
import com.datn.pc_store.modules.order.service.OrderSerialService;
import com.datn.pc_store.modules.order.service.OrderService;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.enums.ProductStatus;
import com.datn.pc_store.modules.product.enums.ProductType;
import com.datn.pc_store.modules.product.repository.BuildingPcRepository;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.enums.RoleType;
import com.datn.pc_store.modules.user.enums.UserStatus;
import com.datn.pc_store.modules.user.repository.UserRepository;
import com.datn.pc_store.modules.warranty.service.WarrantyActivationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceSecurityAndFlowTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private DeliveryService deliveryService;
    @Mock
    private OrderItemSerialRepository orderItemSerialRepository;
    @Mock
    private ProductSerialRepository productSerialRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private BuildingPcRepository buildingPcRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderSerialService orderSerialService;
    @Mock
    private WarrantyActivationService warrantyActivationService;
    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Khách hàng đặt đơn bị ép dùng giá niêm yết DB và không được tự truyền giảm giá/price=1đ")
    void customerCannotTamperPriceOrDiscount() {
        User customer = User.builder().id(10L).role(RoleType.CUSTOMER).status(UserStatus.ACTIVE).build();
        Product product = Product.builder()
                .id(5L)
                .name("RTX 4090")
                .price(45_000_000L)
                .productType(ProductType.OTHER)
                .status(ProductStatus.ACTIVE)
                .build();

        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(5L);
        itemReq.setQuantity(2);
        itemReq.setUnitPrice(1L); // Tampered!
        itemReq.setDiscountAmount(89_000_000L); // Tampered!

        OrderCreateRequest req = new OrderCreateRequest();
        req.setType(OrderType.ONLINE);
        req.setDiscountAmount(40_000_000L); // Tampered!
        req.setRecipientName("Khách A");
        req.setRecipientPhone("0901234567");
        req.setAddress("Hà Nội");
        req.setItems(List.of(itemReq));

        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(orderRepository.existsByCode(anyString())).thenReturn(false);
        when(productRepository.findAllById(any())).thenReturn(List.of(product));
        when(productSerialRepository.countByProductIdAndStatusAndIsTerminatedFalse(5L, ProductSerialStatus.IN_STOCK))
                .thenReturn(10L);
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(99L);
            return o;
        });
        when(orderItemSerialRepository.findAllByOrderId(99L)).thenReturn(List.of());
        when(orderMapper.toResponse(any(Order.class), any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return OrderResponse.builder()
                    .id(o.getId())
                    .totalAmount(o.getTotalAmount())
                    .discountAmount(o.getDiscountAmount())
                    .finalAmount(o.getFinalAmount())
                    .build();
        });

        OrderResponse response = orderService.createOrder(req, 10L, false);

        assertEquals(90_000_000L, response.getTotalAmount());
        assertEquals(0L, response.getDiscountAmount());
        assertEquals(90_000_000L, response.getFinalAmount());
    }

    @Test
    @DisplayName("Khách hàng bị chặn nếu tự chọn serialNumbers")
    void customerCannotPassSerials() {
        User customer = User.builder().id(10L).role(RoleType.CUSTOMER).status(UserStatus.ACTIVE).build();

        OrderItemRequest itemWithSerial = new OrderItemRequest();
        itemWithSerial.setProductId(7L);
        itemWithSerial.setQuantity(1);
        itemWithSerial.setSerialNumbers(List.of("SN-001"));

        OrderCreateRequest req = new OrderCreateRequest();
        req.setType(OrderType.ONLINE);
        req.setRecipientName("Khách A");
        req.setRecipientPhone("0901234567");
        req.setAddress("Hà Nội");
        req.setItems(List.of(itemWithSerial));

        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));

        assertThrows(BadRequestException.class, () -> orderService.createOrder(req, 10L, false));
    }

    @Test
    @DisplayName("Chặn đặt mua sản phẩm hết hàng (OUT_OF_STOCK) hoặc ngừng bán (INACTIVE)")
    void cannotOrderOutOfStockProduct() {
        User customer = User.builder().id(10L).role(RoleType.CUSTOMER).status(UserStatus.ACTIVE).build();
        Product outOfStock = Product.builder().id(6L).name("SSD").price(5_000_000L).productType(ProductType.OTHER).status(ProductStatus.OUT_OF_STOCK).build();

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(6L);
        item.setQuantity(1);

        OrderCreateRequest req = new OrderCreateRequest();
        req.setType(OrderType.ONLINE);
        req.setRecipientName("Khách A");
        req.setRecipientPhone("0901234567");
        req.setAddress("Hà Nội");
        req.setItems(List.of(item));

        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(orderRepository.existsByCode(anyString())).thenReturn(false);
        when(productRepository.findAllById(any())).thenReturn(List.of(outOfStock));

        assertThrows(BadRequestException.class, () -> orderService.createOrder(req, 10L, false));
    }

    @Test
    @DisplayName("updateStatus từ chối chuyển trạng thái trái phép (COMPLETED -> PENDING) và thu hồi bảo hành + serial khi RETURNED")
    void updateStatusEnforcesTransitionsAndVoidsOnReturn() {
        Order completed = Order.builder().id(50L).userId(10L).type(OrderType.ONLINE).status(OrderStatus.COMPLETED).build();
        when(orderRepository.findForUpdate(50L)).thenReturn(Optional.of(completed));

        assertThrows(BadRequestException.class,
                () -> orderService.updateStatus(50L, OrderStatus.PENDING, 1L, true));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemSerialRepository.findAllByOrderId(50L)).thenReturn(List.of());
        when(orderMapper.toResponse(any(Order.class), any())).thenReturn(OrderResponse.builder().id(50L).status(OrderStatus.RETURNED).build());

        OrderResponse returned = orderService.updateStatus(50L, OrderStatus.RETURNED, 1L, true);
        assertEquals(OrderStatus.RETURNED, returned.getStatus());
        verify(orderSerialService).releaseIncludingSold(50L);
    }

    @Test
    @DisplayName("assignSerialsToOrder khóa đơn, chặn đơn đã có vận đơn, kiểm tra đủ số serial")
    void assignSerialsValidatesOrderLockDeliveryAndCounts() {
        OrderItem item = OrderItem.builder().id(101L).productId(5L).quantity(2).build();
        Order confirmed = Order.builder().id(60L).status(OrderStatus.CONFIRMED).items(List.of(item)).build();
        item.setOrder(confirmed);

        when(orderRepository.findForUpdate(60L)).thenReturn(Optional.of(confirmed));
        when(deliveryService.existsForResource(DeliveryResourceType.ORDER, 60L)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> orderService.assignSerialsToOrder(60L, Map.of(101L, List.of("SN-1", "SN-2"))));

        when(deliveryService.existsForResource(DeliveryResourceType.ORDER, 60L)).thenReturn(false);
        assertThrows(BadRequestException.class,
                () -> orderService.assignSerialsToOrder(60L, Map.of(101L, List.of("SN-1"))));

        ProductSerial s1 = ProductSerial.builder().id(1L).productId(5L).serialNumber("SN-1").status(ProductSerialStatus.IN_STOCK).isTerminated(false).build();
        ProductSerial s2 = ProductSerial.builder().id(2L).productId(5L).serialNumber("SN-2").status(ProductSerialStatus.IN_STOCK).isTerminated(false).build();
        when(orderItemSerialRepository.findAllByOrderItemId(101L)).thenReturn(List.of());
        when(productSerialRepository.lockActiveBySerialNumbers(any())).thenReturn(List.of(s1, s2));
        when(orderItemSerialRepository.findAllByOrderId(60L)).thenReturn(List.of());
        when(orderMapper.toResponse(eq(confirmed), any())).thenReturn(OrderResponse.builder().id(60L).build());

        orderService.assignSerialsToOrder(60L, Map.of(101L, List.of("SN-1", "SN-2")));
        assertEquals(ProductSerialStatus.PACKING, s1.getStatus());
        assertEquals(ProductSerialStatus.PACKING, s2.getStatus());
        verify(orderItemSerialRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("handoverToCarrier kiểm tra serial đã gán và chuyển đơn sang DELIVERING")
    void handoverToCarrierChecksSerialsAndTransitions() {
        Order confirmed = Order.builder().id(70L).status(OrderStatus.CONFIRMED).build();
        when(orderRepository.findForUpdate(70L)).thenReturn(Optional.of(confirmed));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemSerialRepository.findAllByOrderId(70L)).thenReturn(List.of());
        when(orderMapper.toResponse(eq(confirmed), any())).thenReturn(OrderResponse.builder().id(70L).status(OrderStatus.DELIVERING).build());

        OrderResponse res = orderService.handoverToCarrier(70L);

        assertEquals(OrderStatus.DELIVERING, res.getStatus());
        verify(orderSerialService).verifyAllAssigned(70L);
        verify(orderSerialService).markDelivering(70L);
    }
}
