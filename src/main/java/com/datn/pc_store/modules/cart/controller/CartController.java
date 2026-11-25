package com.datn.pc_store.modules.cart.controller;

import com.datn.pc_store.common.util.SecurityUtils;
import com.datn.pc_store.modules.cart.dto.CartAddItemRequest;
import com.datn.pc_store.modules.cart.dto.CartCheckoutRequest;
import com.datn.pc_store.modules.cart.dto.CartResponse;
import com.datn.pc_store.modules.cart.dto.CartUpdateItemRequest;
import com.datn.pc_store.modules.cart.service.CartService;
import com.datn.pc_store.modules.order.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Quản lý giỏ hàng và đặt hàng trực tuyến từ giỏ hàng. Yêu cầu Bearer token")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Xem giỏ hàng hiện tại",
            description = "Trả về toàn bộ sản phẩm trong giỏ kèm trạng thái tồn kho khả dụng (AVAILABLE, INSUFFICIENT_STOCK, SOLD_OUT, UNAVAILABLE) và cảnh báo thay đổi giá theo thời gian thực.")
    public ResponseEntity<CartResponse> getCart() {
        Long customerId = SecurityUtils.requireCurrentUser().getId();
        return ResponseEntity.ok(cartService.getCart(customerId));
    }

    @PostMapping("/items")
    @Operation(summary = "Thêm sản phẩm vào giỏ hàng",
            description = "Thêm sản phẩm với số lượng tùy chỉnh (cộng dồn nếu sản phẩm đã có trong giỏ) và kiểm tra không vượt quá tồn kho khả dụng.")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody CartAddItemRequest req) {
        Long customerId = SecurityUtils.requireCurrentUser().getId();
        return ResponseEntity.ok(cartService.addItem(customerId, req));
    }

    @PatchMapping("/items/{productId}")
    @Operation(summary = "Thay đổi số lượng sản phẩm trong giỏ",
            description = "Cập nhật số lượng mới cho sản phẩm trong giỏ (truyền quantity = 0 để xóa khỏi giỏ).")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @Parameter(description = "ID sản phẩm") @PathVariable Long productId,
            @Valid @RequestBody CartUpdateItemRequest req
    ) {
        Long customerId = SecurityUtils.requireCurrentUser().getId();
        return ResponseEntity.ok(cartService.updateItemQuantity(customerId, productId, req));
    }

    @DeleteMapping("/items/unavailable")
    @Operation(summary = "Dọn nhanh sản phẩm hết hàng hoặc ngừng kinh doanh",
            description = "Xóa toàn bộ các sản phẩm đang ở trạng thái SOLD_OUT hoặc UNAVAILABLE khỏi giỏ hàng.")
    public ResponseEntity<CartResponse> removeUnavailableItems() {
        Long customerId = SecurityUtils.requireCurrentUser().getId();
        return ResponseEntity.ok(cartService.removeUnavailableItems(customerId));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Xóa một sản phẩm khỏi giỏ hàng")
    public ResponseEntity<CartResponse> removeItem(
            @Parameter(description = "ID sản phẩm cần xóa") @PathVariable Long productId
    ) {
        Long customerId = SecurityUtils.requireCurrentUser().getId();
        return ResponseEntity.ok(cartService.removeItem(customerId, productId));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa toàn bộ giỏ hàng")
    public void clearCart() {
        Long customerId = SecurityUtils.requireCurrentUser().getId();
        cartService.clearCart(customerId);
    }

    @PostMapping("/checkout")
    @Operation(summary = "Thanh toán / Đặt mua từ giỏ hàng",
            description = "Chuyển toàn bộ giỏ hàng (hoặc các sản phẩm trong selectedProductIds) thành Đơn hàng ONLINE (PENDING), khởi tạo bản ghi thanh toán (COD/ZALOPAY) và xóa sạch giỏ hàng của khách hàng.")
    public ResponseEntity<OrderResponse> checkout(
            @Valid @RequestBody(required = false) CartCheckoutRequest req
    ) {
        Long customerId = SecurityUtils.requireCurrentUser().getId();
        CartCheckoutRequest effectiveReq = req != null ? req : new CartCheckoutRequest();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.checkout(customerId, effectiveReq));
    }
}
