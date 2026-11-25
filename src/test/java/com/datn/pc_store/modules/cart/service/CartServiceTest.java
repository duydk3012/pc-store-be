package com.datn.pc_store.modules.cart.service;

import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.ConflictException;
import com.datn.pc_store.modules.cart.dto.CartAddItemRequest;
import com.datn.pc_store.modules.cart.dto.CartCheckoutRequest;
import com.datn.pc_store.modules.cart.dto.CartResponse;
import com.datn.pc_store.modules.cart.dto.CartUpdateItemRequest;
import com.datn.pc_store.modules.cart.entity.Cart;
import com.datn.pc_store.modules.cart.entity.CartItem;
import com.datn.pc_store.modules.cart.enums.CartItemAvailability;
import com.datn.pc_store.modules.cart.repository.CartRepository;
import com.datn.pc_store.modules.order.dto.OrderCreateRequest;
import com.datn.pc_store.modules.order.dto.OrderResponse;
import com.datn.pc_store.modules.order.enums.OrderStatus;
import com.datn.pc_store.modules.order.enums.OrderType;
import com.datn.pc_store.modules.order.service.OrderService;
import com.datn.pc_store.modules.payment.enums.PaymentMethodCode;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.enums.ProductStatus;
import com.datn.pc_store.modules.product.enums.ProductType;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.product.service.ProductStockService;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.enums.RoleType;
import com.datn.pc_store.modules.user.enums.UserStatus;
import com.datn.pc_store.modules.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductStockService productStockService;
    @Mock private UserRepository userRepository;
    @Mock private OrderService orderService;

    @InjectMocks
    private CartService cartService;

    private Product activeProduct(Long id, String name, long price, ProductStatus status) {
        return Product.builder()
                .id(id)
                .code("SP-" + id)
                .name(name)
                .slug("sp-" + id)
                .price(price)
                .productType(ProductType.OTHER)
                .status(status)
                .isDeleted(false)
                .build();
    }

    private User activeCustomer(Long id) {
        return User.builder()
                .id(id)
                .username("customer" + id)
                .passwordHash("hash")
                .fullName("Nguyễn Minh An")
                .email("c" + id + "@example.com")
                .phoneNumber("0901000011")
                .address("18 Hoàng Quốc Việt, Hà Nội")
                .role(RoleType.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("TC-02a: Thêm sản phẩm vào giỏ mới tự khởi tạo Cart và trả về AVAILABLE")
    void addItem_newCart_createsCartAndAddsItem() {
        Long customerId = 10011L;
        Product ram = activeProduct(10003L, "RAM Kingston 16GB", 720_000L, ProductStatus.ACTIVE);
        User user = activeCustomer(customerId);

        when(productRepository.findById(10003L)).thenReturn(Optional.of(ram));
        when(productStockService.getAvailableStock(ram)).thenReturn(5);
        when(cartRepository.findByCustomerIdForUpdate(customerId)).thenReturn(Optional.empty());
        when(userRepository.findById(customerId)).thenReturn(Optional.of(user));
        when(cartRepository.saveAndFlush(any(Cart.class))).thenAnswer(inv -> {
            Cart c = inv.getArgument(0);
            if (c.getId() == null) c.setId(1L);
            return c;
        });
        when(productRepository.findAllById(List.of(10003L))).thenReturn(List.of(ram));
        when(productStockService.getAvailableStockMap(anyCollection())).thenReturn(Map.of(10003L, 5));

        CartResponse res = cartService.addItem(customerId, CartAddItemRequest.builder()
                .productId(10003L).quantity(2).build());

        assertEquals(1, res.getTotalDistinctItems());
        assertEquals(2, res.getTotalQuantity());
        assertEquals(1_440_000L, res.getEligibleTotalAmount());
        assertFalse(res.isHasInvalidItems());
        assertEquals(CartItemAvailability.AVAILABLE, res.getItems().get(0).getAvailabilityStatus());
    }

    @Test
    @DisplayName("TC-02b: Thêm sản phẩm đã có trong giỏ sẽ cộng dồn số lượng và cập nhật unitPrice")
    void addItem_existingItem_incrementsQuantityAndUpdatesPrice() {
        Long customerId = 10011L;
        Product ram = activeProduct(10003L, "RAM Kingston 16GB", 750_000L, ProductStatus.ACTIVE);
        Cart cart = Cart.builder().id(1L).customerId(customerId).quantity(1).items(new ArrayList<>()).build();
        cart.getItems().add(CartItem.builder().id(10L).cart(cart).productId(10003L).quantity(1).unitPrice(720_000L).build());

        when(productRepository.findById(10003L)).thenReturn(Optional.of(ram));
        when(productStockService.getAvailableStock(ram)).thenReturn(5);
        when(cartRepository.findByCustomerIdForUpdate(customerId)).thenReturn(Optional.of(cart));
        when(cartRepository.saveAndFlush(cart)).thenReturn(cart);
        when(productRepository.findAllById(List.of(10003L))).thenReturn(List.of(ram));
        when(productStockService.getAvailableStockMap(anyCollection())).thenReturn(Map.of(10003L, 5));

        CartResponse res = cartService.addItem(customerId, CartAddItemRequest.builder()
                .productId(10003L).quantity(2).build());

        assertEquals(3, res.getTotalQuantity());
        assertEquals(750_000L, res.getItems().get(0).getCurrentUnitPrice());
        assertEquals(2_250_000L, res.getEligibleTotalAmount());
    }

    @Test
    @DisplayName("TC-03: Chặn thêm vào giỏ khi tổng số lượng vượt quá availableStock")
    void addItem_exceedsAvailableStock_throwsBadRequest() {
        Long customerId = 10011L;
        Product ram = activeProduct(10003L, "RAM Kingston 16GB", 720_000L, ProductStatus.ACTIVE);
        Cart cart = Cart.builder().id(1L).customerId(customerId).quantity(1).items(new ArrayList<>()).build();
        cart.getItems().add(CartItem.builder().id(10L).cart(cart).productId(10003L).quantity(1).unitPrice(720_000L).build());

        when(productRepository.findById(10003L)).thenReturn(Optional.of(ram));
        when(productStockService.getAvailableStock(ram)).thenReturn(1);
        when(cartRepository.findByCustomerIdForUpdate(customerId)).thenReturn(Optional.of(cart));

        assertThrows(BadRequestException.class,
                () -> cartService.addItem(customerId, CartAddItemRequest.builder().productId(10003L).quantity(1).build()));
        verify(cartRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC-04: getCart phát hiện SOLD_OUT, INSUFFICIENT_STOCK, UNAVAILABLE và priceChanged")
    void getCart_detectsSoldOutInsufficientUnavailableAndPriceChanged() {
        Long customerId = 10011L;
        Product pAvailable = activeProduct(10003L, "RAM 16GB", 800_000L, ProductStatus.ACTIVE);
        Product pInsufficient = activeProduct(10001L, "CPU i5", 4_800_000L, ProductStatus.ACTIVE);
        Product pSoldOut = activeProduct(10005L, "GPU RTX 4060", 9_600_000L, ProductStatus.OUT_OF_STOCK);

        Cart cart = Cart.builder().id(1L).customerId(customerId).quantity(7).items(new ArrayList<>()).build();
        cart.getItems().add(CartItem.builder().id(1L).cart(cart).productId(10003L).quantity(2).unitPrice(720_000L).build());
        cart.getItems().add(CartItem.builder().id(2L).cart(cart).productId(10001L).quantity(3).unitPrice(4_800_000L).build());
        cart.getItems().add(CartItem.builder().id(3L).cart(cart).productId(10005L).quantity(1).unitPrice(9_600_000L).build());
        cart.getItems().add(CartItem.builder().id(4L).cart(cart).productId(10099L).quantity(1).unitPrice(500_000L).build());

        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(productRepository.findAllById(List.of(10003L, 10001L, 10005L, 10099L)))
                .thenReturn(List.of(pAvailable, pInsufficient, pSoldOut));
        when(productStockService.getAvailableStockMap(anyCollection()))
                .thenReturn(Map.of(10003L, 5, 10001L, 1, 10005L, 0));
        when(productRepository.findRawProductsIncludingDeleted(List.of(10099L)))
                .thenReturn(List.<Object[]>of(new Object[]{
                        10099L, "SP-DEL", "Deleted Product", "deleted-product", null, 0, 500_000L, 0, 1
                }));

        CartResponse res = cartService.getCart(customerId);

        assertEquals(4, res.getTotalDistinctItems());
        assertEquals(7, res.getTotalQuantity());
        assertEquals(1, res.getEligibleDistinctItems());
        assertEquals(2, res.getEligibleQuantity());
        assertEquals(1_600_000L, res.getEligibleTotalAmount());
        assertTrue(res.isHasInvalidItems());

        assertEquals(CartItemAvailability.AVAILABLE, res.getItems().get(0).getAvailabilityStatus());
        assertTrue(res.getItems().get(0).isPriceChanged());
        assertEquals(CartItemAvailability.INSUFFICIENT_STOCK, res.getItems().get(1).getAvailabilityStatus());
        assertFalse(res.getItems().get(1).isCheckoutEligible());
        assertEquals(CartItemAvailability.SOLD_OUT, res.getItems().get(2).getAvailabilityStatus());
        assertEquals(CartItemAvailability.UNAVAILABLE, res.getItems().get(3).getAvailabilityStatus());
        assertEquals("Deleted Product", res.getItems().get(3).getProductName());
    }

    @Test
    @DisplayName("TC-05 & TC-06: Cập nhật số lượng về mức hợp lệ hoặc 0 để xóa khỏi giỏ")
    void updateItemQuantity_validAndZeroQuantity() {
        Long customerId = 10011L;
        Product cpu = activeProduct(10001L, "CPU i5", 4_800_000L, ProductStatus.ACTIVE);
        Cart cart = Cart.builder().id(1L).customerId(customerId).quantity(3).items(new ArrayList<>()).build();
        cart.getItems().add(CartItem.builder().id(2L).cart(cart).productId(10001L).quantity(3).unitPrice(4_800_000L).build());

        when(cartRepository.findByCustomerIdForUpdate(customerId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10001L)).thenReturn(Optional.of(cpu));
        when(productStockService.getAvailableStock(cpu)).thenReturn(1);
        when(cartRepository.saveAndFlush(cart)).thenReturn(cart);
        when(productRepository.findAllById(List.of(10001L))).thenReturn(List.of(cpu));
        when(productStockService.getAvailableStockMap(anyCollection())).thenReturn(Map.of(10001L, 1));

        CartResponse updated = cartService.updateItemQuantity(customerId, 10001L, new CartUpdateItemRequest(1));
        assertEquals(1, updated.getTotalQuantity());
        assertEquals(CartItemAvailability.AVAILABLE, updated.getItems().get(0).getAvailabilityStatus());

        CartResponse removed = cartService.updateItemQuantity(customerId, 10001L, new CartUpdateItemRequest(0));
        assertEquals(0, removed.getTotalDistinctItems());
        assertEquals(0, removed.getTotalQuantity());
    }

    @Test
    @DisplayName("TC-07: Checkout từ chối 409 Conflict khi sản phẩm được chọn vừa Sold Out")
    void checkout_soldOutItem_throwsConflict() {
        Long customerId = 10011L;
        Product gpu = activeProduct(10005L, "GPU RTX 4060", 9_600_000L, ProductStatus.OUT_OF_STOCK);
        Cart cart = Cart.builder().id(1L).customerId(customerId).quantity(1).items(new ArrayList<>()).build();
        cart.getItems().add(CartItem.builder().id(3L).cart(cart).productId(10005L).quantity(1).unitPrice(9_600_000L).build());

        when(cartRepository.findByCustomerIdForUpdate(customerId)).thenReturn(Optional.of(cart));
        when(productRepository.findAllByIdForUpdate(List.of(10005L))).thenReturn(List.of(gpu));
        when(productStockService.getAvailableStockMap(List.of(gpu))).thenReturn(Map.of(10005L, 0));

        assertThrows(ConflictException.class, () -> cartService.checkout(customerId, new CartCheckoutRequest()));
        verify(orderService, never()).createOrder(any(), anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("TC-08: Checkout một phần giỏ hàng tạo Order cho SP chọn và clear toàn bộ giỏ hàng của khách")
    void checkout_partialSelection_createsOrderAndClearsCart() {
        Long customerId = 10011L;
        User user = activeCustomer(customerId);
        Product ram = activeProduct(10003L, "RAM 16GB", 720_000L, ProductStatus.ACTIVE);

        Cart cart = Cart.builder().id(1L).customerId(customerId).quantity(3).items(new ArrayList<>()).build();
        cart.getItems().add(CartItem.builder().id(1L).cart(cart).productId(10003L).quantity(2).unitPrice(720_000L).build());
        cart.getItems().add(CartItem.builder().id(2L).cart(cart).productId(10005L).quantity(1).unitPrice(9_600_000L).build());

        when(cartRepository.findByCustomerIdForUpdate(customerId)).thenReturn(Optional.of(cart));
        when(productRepository.findAllByIdForUpdate(List.of(10003L))).thenReturn(List.of(ram));
        when(productStockService.getAvailableStockMap(List.of(ram))).thenReturn(Map.of(10003L, 5));
        when(userRepository.findById(customerId)).thenReturn(Optional.of(user));
        when(orderService.createOrder(any(OrderCreateRequest.class), eq(customerId), eq(false)))
                .thenReturn(OrderResponse.builder().id(501L).code("DH-501").status(OrderStatus.PENDING).build());

        CartCheckoutRequest req = CartCheckoutRequest.builder()
                .selectedProductIds(List.of(10003L))
                .paymentMethod(PaymentMethodCode.COD)
                .build();

        OrderResponse order = cartService.checkout(customerId, req);

        assertEquals(501L, order.getId());
        assertTrue(cart.getItems().isEmpty());
        assertEquals(0, cart.getQuantity());
        verify(cartRepository).saveAndFlush(cart);

        ArgumentCaptor<OrderCreateRequest> orderCaptor = ArgumentCaptor.forClass(OrderCreateRequest.class);
        verify(orderService).createOrder(orderCaptor.capture(), eq(customerId), eq(false));
        OrderCreateRequest captured = orderCaptor.getValue();
        assertEquals(OrderType.ONLINE, captured.getType());
        assertEquals(1, captured.getItems().size());
        assertEquals(10003L, captured.getItems().get(0).getProductId());
        assertEquals(2, captured.getItems().get(0).getQuantity());
        assertEquals("Nguyễn Minh An", captured.getRecipientName());
    }

    @Test
    @DisplayName("Checkout toàn bộ giỏ hàng làm trống cart và đặt cart.quantity về 0")
    void checkout_fullCart_clearsCart() {
        Long customerId = 10011L;
        User user = activeCustomer(customerId);
        Product ram = activeProduct(10003L, "RAM 16GB", 720_000L, ProductStatus.ACTIVE);

        Cart cart = Cart.builder().id(1L).customerId(customerId).quantity(2).items(new ArrayList<>()).build();
        cart.getItems().add(CartItem.builder().id(1L).cart(cart).productId(10003L).quantity(2).unitPrice(720_000L).build());

        when(cartRepository.findByCustomerIdForUpdate(customerId)).thenReturn(Optional.of(cart));
        when(productRepository.findAllByIdForUpdate(List.of(10003L))).thenReturn(List.of(ram));
        when(productStockService.getAvailableStockMap(List.of(ram))).thenReturn(Map.of(10003L, 5));
        when(userRepository.findById(customerId)).thenReturn(Optional.of(user));
        when(orderService.createOrder(any(OrderCreateRequest.class), eq(customerId), eq(false)))
                .thenReturn(OrderResponse.builder().id(502L).code("DH-502").status(OrderStatus.PENDING).build());

        OrderResponse order = cartService.checkout(customerId, new CartCheckoutRequest());

        assertEquals(502L, order.getId());
        assertTrue(cart.getItems().isEmpty());
        assertEquals(0, cart.getQuantity());
        verify(cartRepository).saveAndFlush(cart);
    }

    @Test
    @DisplayName("removeUnavailableItems chỉ xóa sản phẩm SOLD_OUT và UNAVAILABLE, giữ lại INSUFFICIENT_STOCK")
    void removeUnavailableItems_removesSoldOutAndUnavailableOnly() {
        Long customerId = 10011L;
        Product pInsuff = activeProduct(10001L, "CPU i5", 4_800_000L, ProductStatus.ACTIVE);
        Product pSoldOut = activeProduct(10005L, "GPU RTX 4060", 9_600_000L, ProductStatus.OUT_OF_STOCK);

        Cart cart = Cart.builder().id(1L).customerId(customerId).quantity(4).items(new ArrayList<>()).build();
        cart.getItems().add(CartItem.builder().id(1L).cart(cart).productId(10001L).quantity(2).unitPrice(4_800_000L).build());
        cart.getItems().add(CartItem.builder().id(2L).cart(cart).productId(10005L).quantity(1).unitPrice(9_600_000L).build());
        cart.getItems().add(CartItem.builder().id(3L).cart(cart).productId(10099L).quantity(1).unitPrice(100_000L).build());

        when(cartRepository.findByCustomerIdForUpdate(customerId)).thenReturn(Optional.of(cart));
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(pInsuff, pSoldOut));
        when(productStockService.getAvailableStockMap(anyCollection())).thenReturn(Map.of(10001L, 1, 10005L, 0));
        when(cartRepository.saveAndFlush(cart)).thenReturn(cart);

        CartResponse res = cartService.removeUnavailableItems(customerId);

        assertEquals(1, res.getTotalDistinctItems());
        assertEquals(2, res.getTotalQuantity());
        assertEquals(10001L, res.getItems().get(0).getProductId());
    }
}
