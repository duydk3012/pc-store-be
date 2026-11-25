package com.datn.pc_store.modules.cart.service;

import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.ConflictException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.cart.dto.*;
import com.datn.pc_store.modules.cart.entity.Cart;
import com.datn.pc_store.modules.cart.entity.CartItem;
import com.datn.pc_store.modules.cart.enums.CartItemAvailability;
import com.datn.pc_store.modules.cart.repository.CartRepository;
import com.datn.pc_store.modules.order.dto.OrderCreateRequest;
import com.datn.pc_store.modules.order.dto.OrderItemRequest;
import com.datn.pc_store.modules.order.dto.OrderResponse;
import com.datn.pc_store.modules.order.enums.OrderType;
import com.datn.pc_store.modules.order.service.OrderService;
import com.datn.pc_store.modules.payment.enums.PaymentMethodCode;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.enums.ProductStatus;
import com.datn.pc_store.modules.product.enums.ProductType;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.product.service.ProductStockService;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.enums.UserStatus;
import com.datn.pc_store.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductStockService productStockService;
    private final UserRepository userRepository;
    private final OrderService orderService;

    @Transactional(readOnly = true)
    public CartResponse getCart(Long customerId) {
        if (customerId == null) {
            throw new BadRequestException("Yêu cầu đăng nhập để xem giỏ hàng.");
        }
        return cartRepository.findByCustomerId(customerId)
                .map(this::toCartResponse)
                .orElseGet(() -> emptyCartResponse(null, customerId));
    }

    @Transactional
    public CartResponse addItem(Long customerId, CartAddItemRequest req) {
        if (customerId == null) {
            throw new BadRequestException("Yêu cầu đăng nhập để thêm sản phẩm vào giỏ hàng.");
        }
        Product product = productRepository.findById(req.getProductId())
                .filter(p -> !p.isDeleted() && p.getStatus() != ProductStatus.INACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", req.getProductId()));

        if (product.getPrice() == null || product.getPrice() <= 0) {
            throw new BadRequestException("Sản phẩm '" + product.getName() + "' chưa có giá bán niêm yết.");
        }

        int availableStock = productStockService.getAvailableStock(product);
        if (product.getStatus() == ProductStatus.OUT_OF_STOCK || availableStock <= 0) {
            throw new BadRequestException("Sản phẩm '" + product.getName() + "' hiện đã hết hàng.");
        }

        Cart cart = getOrCreateCartForUpdate(customerId);
        Optional<CartItem> existingOpt = cart.getItems().stream()
                .filter(it -> Objects.equals(it.getProductId(), product.getId()))
                .findFirst();

        if (existingOpt.isPresent()) {
            CartItem existingItem = existingOpt.get();
            int targetQty = existingItem.getQuantity() + req.getQuantity();
            if (targetQty > availableStock) {
                throw new BadRequestException("Bạn đã có " + existingItem.getQuantity() + " sản phẩm '"
                        + product.getName() + "' trong giỏ hàng. Kho hiện chỉ còn tối đa "
                        + availableStock + " sản phẩm khả dụng.");
            }
            existingItem.setQuantity(targetQty);
            existingItem.setUnitPrice(product.getPrice());
        } else {
            if (req.getQuantity() > availableStock) {
                throw new BadRequestException("Sản phẩm '" + product.getName() + "' chỉ còn "
                        + availableStock + " sản phẩm khả dụng.");
            }
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productId(product.getId())
                    .quantity(req.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            cart.getItems().add(newItem);
        }

        recalculateCartQuantity(cart);
        Cart saved = cartRepository.saveAndFlush(cart);
        return toCartResponse(saved);
    }

    @Transactional
    public CartResponse updateItemQuantity(Long customerId, Long productId, CartUpdateItemRequest req) {
        if (customerId == null) {
            throw new BadRequestException("Yêu cầu đăng nhập để cập nhật giỏ hàng.");
        }
        Cart cart = cartRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng", "customerId", customerId));

        CartItem item = cart.getItems().stream()
                .filter(it -> Objects.equals(it.getProductId(), productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm trong giỏ hàng", "productId", productId));

        if (req.getQuantity() == 0) {
            cart.getItems().remove(item);
            recalculateCartQuantity(cart);
            Cart saved = cartRepository.saveAndFlush(cart);
            return toCartResponse(saved);
        }

        Product product = productRepository.findById(productId)
                .filter(p -> !p.isDeleted() && p.getStatus() != ProductStatus.INACTIVE
                        && p.getPrice() != null && p.getPrice() > 0)
                .orElseThrow(() -> new BadRequestException(
                        "Sản phẩm hiện đã ngừng kinh doanh, không thể cập nhật số lượng. Vui lòng xóa khỏi giỏ hàng."));

        int availableStock = productStockService.getAvailableStock(product);
        if (product.getStatus() == ProductStatus.OUT_OF_STOCK || availableStock <= 0) {
            throw new BadRequestException("Sản phẩm '" + product.getName()
                    + "' hiện đã hết hàng, không thể cập nhật số lượng.");
        }
        if (req.getQuantity() > availableStock) {
            throw new BadRequestException("Sản phẩm '" + product.getName()
                    + "' hiện chỉ còn tối đa " + availableStock + " sản phẩm khả dụng.");
        }

        item.setQuantity(req.getQuantity());
        item.setUnitPrice(product.getPrice());
        recalculateCartQuantity(cart);
        Cart saved = cartRepository.saveAndFlush(cart);
        return toCartResponse(saved);
    }

    @Transactional
    public CartResponse removeItem(Long customerId, Long productId) {
        if (customerId == null) {
            throw new BadRequestException("Yêu cầu đăng nhập để cập nhật giỏ hàng.");
        }
        Optional<Cart> cartOpt = cartRepository.findByCustomerIdForUpdate(customerId);
        if (cartOpt.isEmpty()) {
            return emptyCartResponse(null, customerId);
        }
        Cart cart = cartOpt.get();
        cart.getItems().removeIf(it -> Objects.equals(it.getProductId(), productId));
        recalculateCartQuantity(cart);
        Cart saved = cartRepository.saveAndFlush(cart);
        return toCartResponse(saved);
    }

    @Transactional
    public CartResponse removeUnavailableItems(Long customerId) {
        if (customerId == null) {
            throw new BadRequestException("Yêu cầu đăng nhập để cập nhật giỏ hàng.");
        }
        Optional<Cart> cartOpt = cartRepository.findByCustomerIdForUpdate(customerId);
        if (cartOpt.isEmpty()) {
            return emptyCartResponse(null, customerId);
        }
        Cart cart = cartOpt.get();
        if (cart.getItems().isEmpty()) {
            return emptyCartResponse(cart.getId(), customerId);
        }

        List<Long> productIds = cart.getItems().stream()
                .map(CartItem::getProductId)
                .distinct()
                .toList();
        Map<Long, Product> activeMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, Integer> stockMap = productStockService.getAvailableStockMap(activeMap.values());

        cart.getItems().removeIf(item -> {
            Product product = activeMap.get(item.getProductId());
            if (product == null || product.isDeleted()
                    || product.getStatus() != ProductStatus.ACTIVE
                    || product.getPrice() == null || product.getPrice() <= 0) {
                return true;
            }
            return stockMap.getOrDefault(product.getId(), 0) <= 0;
        });

        recalculateCartQuantity(cart);
        Cart saved = cartRepository.saveAndFlush(cart);
        return toCartResponse(saved);
    }

    @Transactional
    public void clearCart(Long customerId) {
        if (customerId == null) {
            throw new BadRequestException("Yêu cầu đăng nhập để xóa giỏ hàng.");
        }
        cartRepository.findByCustomerIdForUpdate(customerId).ifPresent(cart -> {
            cart.getItems().clear();
            cart.setQuantity(0);
            cartRepository.saveAndFlush(cart);
        });
    }

    @Transactional
    public OrderResponse checkout(Long customerId, CartCheckoutRequest req) {
        if (customerId == null) {
            throw new BadRequestException("Yêu cầu đăng nhập để thanh toán giỏ hàng.");
        }
        Cart cart = cartRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new BadRequestException("Giỏ hàng đang trống, không thể thanh toán."));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng đang trống, không thể thanh toán.");
        }

        List<CartItem> selectedItems = resolveSelectedItems(cart, req);
        List<Long> sortedProductIds = selectedItems.stream()
                .map(CartItem::getProductId)
                .distinct()
                .sorted()
                .toList();

        List<Product> lockedProducts = productRepository.findAllByIdForUpdate(sortedProductIds);
        Map<Long, Product> productMap = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, Integer> stockMap = productStockService.getAvailableStockMap(lockedProducts);

        List<OrderItemRequest> orderItems = new ArrayList<>();
        for (CartItem ci : selectedItems) {
            Product product = productMap.get(ci.getProductId());
            if (product == null || product.isDeleted()
                    || product.getStatus() == ProductStatus.INACTIVE
                    || product.getPrice() == null || product.getPrice() <= 0) {
                throw new ConflictException("Sản phẩm (ID: " + ci.getProductId()
                        + ") đã ngừng kinh doanh hoặc không còn khả dụng.");
            }
            int available = stockMap.getOrDefault(product.getId(), 0);
            if (product.getStatus() == ProductStatus.OUT_OF_STOCK || available <= 0) {
                throw new ConflictException("Sản phẩm '" + product.getName()
                        + "' vừa hết hàng (Sold Out), vui lòng bỏ chọn sản phẩm này để tiếp tục.");
            }
            if (ci.getQuantity() > available) {
                throw new ConflictException("Sản phẩm '" + product.getName() + "' chỉ còn "
                        + available + " sản phẩm khả dụng (trong giỏ đang chọn " + ci.getQuantity() + ").");
            }
            orderItems.add(OrderItemRequest.builder()
                    .productId(product.getId())
                    .quantity(ci.getQuantity())
                    .unitPrice(product.getPrice())
                    .build());
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng", "id", customerId));

        String recipientName = firstNonBlank(req != null ? req.getRecipientName() : null, customer.getFullName());
        String recipientPhone = firstNonBlank(req != null ? req.getRecipientPhone() : null, customer.getPhoneNumber());
        String address = firstNonBlank(req != null ? req.getAddress() : null, customer.getAddress());

        if (isBlank(recipientName) || isBlank(recipientPhone) || isBlank(address)) {
            throw new BadRequestException("Vui lòng cung cấp đầy đủ tên người nhận, số điện thoại và địa chỉ nhận hàng.");
        }

        Long discountAmount = (req != null && req.getDiscountAmount() != null)
                ? req.getDiscountAmount()
                : 0L;

        OrderCreateRequest orderReq = OrderCreateRequest.builder()
                .type(OrderType.ONLINE)
                .paymentMethod(req != null && req.getPaymentMethod() != null ? req.getPaymentMethod() : PaymentMethodCode.COD)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .address(address)
                .note(req != null ? req.getNote() : null)
                .discountAmount(discountAmount)
                .items(orderItems)
                .build();

        OrderResponse orderResponse = orderService.createOrder(orderReq, customerId, false);

        cart.getItems().clear();
        cart.setQuantity(0);
        cartRepository.saveAndFlush(cart);

        log.info("Checkout giỏ hàng thành công: customerId={}, orderCode={}, itemCount={}",
                customerId, orderResponse.getCode(), orderItems.size());
        return orderResponse;
    }

    private List<CartItem> resolveSelectedItems(Cart cart, CartCheckoutRequest req) {
        if (req == null || req.getSelectedProductIds() == null || req.getSelectedProductIds().isEmpty()) {
            return new ArrayList<>(cart.getItems());
        }
        Set<Long> selectedSet = new LinkedHashSet<>();
        for (Long pid : req.getSelectedProductIds()) {
            if (pid == null) {
                throw new BadRequestException("Danh sách sản phẩm được chọn chứa giá trị không hợp lệ.");
            }
            selectedSet.add(pid);
        }
        Map<Long, CartItem> byProductId = cart.getItems().stream()
                .collect(Collectors.toMap(CartItem::getProductId, it -> it, (a, b) -> a));

        List<CartItem> selected = new ArrayList<>();
        for (Long pid : selectedSet) {
            CartItem item = byProductId.get(pid);
            if (item == null) {
                throw new BadRequestException("Sản phẩm ID " + pid + " không có trong giỏ hàng của bạn.");
            }
            selected.add(item);
        }
        return selected;
    }

    private Cart getOrCreateCartForUpdate(Long customerId) {
        return cartRepository.findByCustomerIdForUpdate(customerId)
                .orElseGet(() -> {
                    User customer = userRepository.findById(customerId)
                            .orElseThrow(() -> new ResourceNotFoundException("Khách hàng", "id", customerId));
                    if (customer.isDeleted() || customer.getStatus() == UserStatus.BANNED) {
                        throw new BadRequestException("Tài khoản khách hàng này đang bị khóa hoặc không hợp lệ.");
                    }
                    Cart created = Cart.builder()
                            .customerId(customerId)
                            .quantity(0)
                            .build();
                    return cartRepository.saveAndFlush(created);
                });
    }

    private void recalculateCartQuantity(Cart cart) {
        int totalQty = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
        cart.setQuantity(totalQty);
    }

    private CartResponse toCartResponse(Cart cart) {
        List<CartItem> items = cart.getItems() == null ? List.of() : cart.getItems();
        if (items.isEmpty()) {
            return emptyCartResponse(cart.getId(), cart.getCustomerId());
        }

        List<Long> productIds = items.stream()
                .map(CartItem::getProductId)
                .distinct()
                .toList();

        Map<Long, Product> activeMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, Integer> stockMap = productStockService.getAvailableStockMap(activeMap.values());

        List<Long> missingIds = productIds.stream()
                .filter(id -> !activeMap.containsKey(id))
                .toList();
        Map<Long, RawProductInfo> rawMap = loadRawProductMap(missingIds);

        List<CartItemResponse> itemResponses = new ArrayList<>();
        int totalQuantity = 0;
        int eligibleDistinct = 0;
        int eligibleQuantity = 0;
        long eligibleTotalAmount = 0L;
        boolean hasInvalidItems = false;

        for (CartItem item : items) {
            Long pid = item.getProductId();
            Product product = activeMap.get(pid);
            RawProductInfo raw = product == null ? rawMap.get(pid) : null;

            String code = product != null ? product.getCode() : (raw != null ? raw.code() : null);
            String name = product != null ? product.getName() : (raw != null ? raw.name() : null);
            String slug = product != null ? product.getSlug() : (raw != null ? raw.slug() : null);
            String imageUrl = product != null ? product.getImageUrl() : (raw != null ? raw.imageUrl() : null);
            ProductType productType = product != null ? product.getProductType() : (raw != null ? raw.productType() : null);
            ProductStatus productStatus = product != null ? product.getStatus() : (raw != null ? raw.status() : null);

            CartItemAvailability availability;
            int availableStock;
            boolean checkoutEligible;
            String availabilityMessage;
            long currentUnitPrice;
            boolean priceChanged;

            if (product == null || product.isDeleted()
                    || product.getStatus() == ProductStatus.INACTIVE
                    || product.getPrice() == null || product.getPrice() <= 0) {
                availability = CartItemAvailability.UNAVAILABLE;
                availableStock = 0;
                checkoutEligible = false;
                availabilityMessage = "Sản phẩm hiện ngừng kinh doanh";
                Long fallbackPrice = product != null && product.getPrice() != null
                        ? product.getPrice()
                        : (raw != null && raw.price() != null ? raw.price() : item.getUnitPrice());
                currentUnitPrice = fallbackPrice;
                priceChanged = product != null && product.getPrice() != null
                        && !Objects.equals(item.getUnitPrice(), product.getPrice());
            } else {
                currentUnitPrice = product.getPrice();
                priceChanged = !Objects.equals(item.getUnitPrice(), currentUnitPrice);
                int stock = stockMap.getOrDefault(pid, 0);

                if (product.getStatus() == ProductStatus.OUT_OF_STOCK || stock <= 0) {
                    availability = CartItemAvailability.SOLD_OUT;
                    availableStock = 0;
                    checkoutEligible = false;
                    availabilityMessage = "Sản phẩm hiện đã hết hàng";
                } else if (stock < item.getQuantity()) {
                    availability = CartItemAvailability.INSUFFICIENT_STOCK;
                    availableStock = stock;
                    checkoutEligible = false;
                    availabilityMessage = "Kho chỉ còn " + stock + " sản phẩm khả dụng, vui lòng giảm số lượng";
                } else {
                    availability = CartItemAvailability.AVAILABLE;
                    availableStock = stock;
                    checkoutEligible = true;
                    availabilityMessage = null;
                }
            }

            long subtotal = currentUnitPrice * item.getQuantity();
            totalQuantity += item.getQuantity();

            if (checkoutEligible) {
                eligibleDistinct++;
                eligibleQuantity += item.getQuantity();
                eligibleTotalAmount += subtotal;
            } else {
                hasInvalidItems = true;
            }

            itemResponses.add(CartItemResponse.builder()
                    .id(item.getId())
                    .productId(pid)
                    .productCode(code)
                    .productName(name)
                    .productSlug(slug)
                    .productImageUrl(imageUrl)
                    .productType(productType)
                    .productStatus(productStatus)
                    .quantity(item.getQuantity())
                    .savedUnitPrice(item.getUnitPrice())
                    .currentUnitPrice(currentUnitPrice)
                    .priceChanged(priceChanged)
                    .subtotal(subtotal)
                    .availableStock(availableStock)
                    .availabilityStatus(availability)
                    .checkoutEligible(checkoutEligible)
                    .availabilityMessage(availabilityMessage)
                    .createdDate(item.getCreatedDate())
                    .lastModifiedDate(item.getLastModifiedDate())
                    .build());
        }

        return CartResponse.builder()
                .id(cart.getId())
                .customerId(cart.getCustomerId())
                .totalDistinctItems(itemResponses.size())
                .totalQuantity(totalQuantity)
                .eligibleDistinctItems(eligibleDistinct)
                .eligibleQuantity(eligibleQuantity)
                .eligibleTotalAmount(eligibleTotalAmount)
                .hasInvalidItems(hasInvalidItems)
                .items(itemResponses)
                .build();
    }

    private Map<Long, RawProductInfo> loadRawProductMap(List<Long> missingIds) {
        if (missingIds == null || missingIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = productRepository.findRawProductsIncludingDeleted(missingIds);
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        Map<Long, RawProductInfo> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 9 || row[0] == null) {
                continue;
            }
            long id = ((Number) row[0]).longValue();
            String code = row[1] != null ? row[1].toString() : null;
            String name = row[2] != null ? row[2].toString() : null;
            String slug = row[3] != null ? row[3].toString() : null;
            String imageUrl = row[4] != null ? row[4].toString() : null;
            ProductType type = row[5] instanceof Number n ? ProductType.fromValue(n.intValue()) : null;
            Long price = row[6] instanceof Number n ? n.longValue() : null;
            ProductStatus status = row[7] instanceof Number n ? ProductStatus.fromValue(n.intValue()) : null;
            boolean deleted = row[8] instanceof Boolean b ? b : (row[8] instanceof Number n && n.intValue() != 0);
            map.put(id, new RawProductInfo(id, code, name, slug, imageUrl, type, price, status, deleted));
        }
        return map;
    }

    private CartResponse emptyCartResponse(Long cartId, Long customerId) {
        return CartResponse.builder()
                .id(cartId)
                .customerId(customerId)
                .totalDistinctItems(0)
                .totalQuantity(0)
                .eligibleDistinctItems(0)
                .eligibleQuantity(0)
                .eligibleTotalAmount(0L)
                .hasInvalidItems(false)
                .items(List.of())
                .build();
    }

    private String firstNonBlank(String primary, String fallback) {
        if (!isBlank(primary)) {
            return primary.trim();
        }
        return !isBlank(fallback) ? fallback.trim() : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record RawProductInfo(
            Long id,
            String code,
            String name,
            String slug,
            String imageUrl,
            ProductType productType,
            Long price,
            ProductStatus status,
            boolean deleted
    ) {
    }
}
