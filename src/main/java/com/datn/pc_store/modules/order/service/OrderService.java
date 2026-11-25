package com.datn.pc_store.modules.order.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.ConflictException;
import com.datn.pc_store.common.exception.DuplicateResourceException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.common.util.PaginationUtils;
import com.datn.pc_store.modules.delivery.enums.DeliveryResourceType;
import com.datn.pc_store.modules.delivery.service.DeliveryService;
import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.order.dto.*;
import com.datn.pc_store.modules.order.entity.Order;
import com.datn.pc_store.modules.order.entity.OrderItem;
import com.datn.pc_store.modules.order.entity.OrderItemSerial;
import com.datn.pc_store.modules.order.enums.OrderStatus;
import com.datn.pc_store.modules.order.enums.OrderType;
import com.datn.pc_store.modules.order.mapper.OrderMapper;
import com.datn.pc_store.modules.order.repository.OrderItemSerialRepository;
import com.datn.pc_store.modules.order.repository.OrderRepository;
import com.datn.pc_store.modules.order.util.OrderAddressHelper;
import com.datn.pc_store.modules.payment.entity.Payment;
import com.datn.pc_store.modules.payment.repository.PaymentRepository;
import com.datn.pc_store.modules.payment.service.PaymentService;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.enums.ProductStatus;
import com.datn.pc_store.modules.product.enums.ProductType;
import com.datn.pc_store.modules.product.repository.BuildingPcRepository;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.product.service.ProductStockService;
import com.datn.pc_store.modules.systemlog.dto.SystemLogCreateCommand;
import com.datn.pc_store.modules.systemlog.enums.SystemLogAction;
import com.datn.pc_store.modules.systemlog.enums.SystemLogModule;
import com.datn.pc_store.modules.systemlog.event.SystemLogPublisher;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.enums.UserStatus;
import com.datn.pc_store.modules.user.repository.UserRepository;
import com.datn.pc_store.modules.warranty.service.WarrantyActivationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "code", "userId", "type", "totalAmount", "discountAmount", "finalAmount",
            "status", "address", "createdDate", "createdBy", "lastModifiedDate", "lastModifiedBy"
    );

    private final OrderRepository orderRepository;
    private final DeliveryService deliveryService;
    private final OrderItemSerialRepository orderItemSerialRepository;
    private final ProductSerialRepository productSerialRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final BuildingPcRepository buildingPcRepository;
    private final OrderMapper orderMapper;
    private final OrderSerialService orderSerialService;
    private final WarrantyActivationService warrantyActivationService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final SystemLogPublisher systemLogPublisher;
    private final ProductStockService productStockService;

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(
            String keyword,
            OrderStatus status,
            OrderType type,
            Long userId,
            Instant fromDate,
            Instant toDate,
            Pageable pageable
    ) {
        PaginationUtils.validateDateRange(fromDate, toDate);
        PaginationUtils.validateSort(pageable, SORTABLE_FIELDS);

        Page<Order> page = orderRepository.searchOrders(
                keyword != null && !keyword.isBlank() ? keyword.trim() : null,
                status,
                type,
                userId,
                fromDate,
                toDate,
                pageable
        );

        List<Order> orders = page.getContent();
        Map<Long, Map<Long, List<String>>> orderSerialsMap = new HashMap<>();
        if (!orders.isEmpty()) {
            List<Long> orderIds = orders.stream().map(Order::getId).toList();
            for (Object[] row : orderItemSerialRepository.findAllWithOrderIdByOrderIdIn(orderIds)) {
                Long orderId = (Long) row[0];
                OrderItemSerial ois = (OrderItemSerial) row[1];
                ProductSerial ps = ois.getProductSerial();
                if (ps != null && ps.getSerialNumber() != null) {
                    orderSerialsMap
                            .computeIfAbsent(orderId, k -> new HashMap<>())
                            .computeIfAbsent(ois.getOrderItemId(), k -> new ArrayList<>())
                            .add(ps.getSerialNumber());
                }
            }
        }

        List<OrderResponse> content = orders.stream()
                .map(o -> orderMapper.toResponse(o, orderSerialsMap.getOrDefault(o.getId(), Collections.emptyMap())))
                .toList();

        return PageResponse.of(new PageImpl<>(
                content, pageable, page.getTotalElements()
        ));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id, Long currentUserId, boolean isStaffOrAdmin) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", id));

        if (!isStaffOrAdmin && !order.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không có quyền xem thông tin đơn hàng này.");
        }

        return mapOrderWithSerials(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByCode(String code, Long currentUserId, boolean isStaffOrAdmin) {
        Order order = orderRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "code", code));

        if (!isStaffOrAdmin && !order.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không có quyền xem thông tin đơn hàng này.");
        }

        return mapOrderWithSerials(order);
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest req, Long currentUserId, boolean isStaffOrAdmin) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BadRequestException("Đơn hàng phải có ít nhất 1 sản phẩm.");
        }

        // Xác định chủ sở hữu đơn hàng (userId)
        Long targetUserId;
        if (req.getUserId() != null && isStaffOrAdmin) {
            targetUserId = req.getUserId();
        } else if (currentUserId != null) {
            targetUserId = currentUserId;
        } else if (req.getUserId() != null) {
            targetUserId = req.getUserId();
        } else {
            throw new BadRequestException("Vui lòng cung cấp ID khách hàng hoặc đăng nhập để tiếp tục.");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng", "id", targetUserId));
        if (user.isDeleted() || user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Tài khoản khách hàng này đang bị khóa hoặc không hoạt động.");
        }

        boolean hasAnySerial = req.getItems().stream()
                .anyMatch(it -> it.getSerialNumbers() != null && !it.getSerialNumbers().isEmpty());
        if (!isStaffOrAdmin && hasAnySerial) {
            throw new BadRequestException("Khách hàng không được tự chỉ định mã serial khi đặt hàng.");
        }

        // Mã đơn hàng
        String code;
        if (req.getCode() != null && !req.getCode().isBlank()) {
            code = req.getCode().trim();
            if (orderRepository.existsByCode(code)) {
                throw new DuplicateResourceException("Đơn hàng", "code", code);
            }
        } else {
            code = generateUniqueOrderCode();
        }

        // Xử lý các dòng sản phẩm
        List<OrderItem> orderItems = new ArrayList<>();
        Map<Long, Product> productMap = new LinkedHashMap<>();
        Map<Long, Integer> requestedWithoutSerialMap = new LinkedHashMap<>();
        Set<Long> orderedProductIds = new LinkedHashSet<>();
        long runningTotal = 0L;
        long totalItemDiscounts = 0L;

        Map<Long, Integer> requiredQtyByProduct = new HashMap<>();
        for (OrderItemRequest itemReq : req.getItems()) {
            Product product = productMap.get(itemReq.getProductId());
            if (product == null) {
                throw new ResourceNotFoundException("Sản phẩm", "id", itemReq.getProductId());
            }
            if (product.isDeleted() || product.getStatus() == ProductStatus.INACTIVE) {
                throw new BadRequestException("Sản phẩm '" + product.getName() + "' (ID: "
                        + product.getId() + ") đang tạm ngưng kinh doanh.");
            }
            if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
                throw new ConflictException("Sản phẩm '" + product.getName() + "' hiện đã hết hàng.");
            }

        for (OrderItemRequest itemReq : req.getItems()) {
            Product product = productMap.get(itemReq.getProductId());
            Long catalogPrice = product.getPrice();
            long unitPrice;
            if (isStaffOrAdmin && itemReq.getUnitPrice() != null && itemReq.getUnitPrice() > 0) {
                unitPrice = itemReq.getUnitPrice();
            } else {
                if (catalogPrice == null || catalogPrice <= 0) {
                    throw new BadRequestException("Sản phẩm '" + product.getName() + "' chưa được thiết lập giá bán hợp lệ.");
                }
                unitPrice = catalogPrice;
            }

            int quantity = itemReq.getQuantity() != null && itemReq.getQuantity() > 0 ? itemReq.getQuantity() : 1;
            long lineGross;
            try {
                lineGross = Math.multiplyExact(unitPrice, (long) quantity);
            } catch (ArithmeticException ex) {
                throw new BadRequestException("Thành tiền dòng sản phẩm vượt giới hạn cho phép.");
            }

            long itemDiscount = 0L;
            if (isStaffOrAdmin && itemReq.getDiscountAmount() != null && itemReq.getDiscountAmount() > 0) {
                itemDiscount = itemReq.getDiscountAmount();
                if (itemDiscount > lineGross) {
                    throw new BadRequestException("Giảm giá dòng sản phẩm không được lớn hơn thành tiền.");
                }
            }

            long itemSubtotal = lineGross - itemDiscount;

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.getId())
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .discountAmount(itemDiscount)
                    .subtotal(itemSubtotal)
                    .build();

            orderItems.add(orderItem);
            productMap.put(product.getId(), product);
            orderedProductIds.add(product.getId());
            if (itemReq.getSerialNumbers() == null || itemReq.getSerialNumbers().isEmpty()) {
                requestedWithoutSerialMap.merge(product.getId(), quantity, Integer::sum);
            }
            runningTotal += (unitPrice * quantity);
            totalItemDiscounts += itemDiscount;
        }

        if (productStockService != null && !requestedWithoutSerialMap.isEmpty()) {
            List<Product> productsWithoutSerial = requestedWithoutSerialMap.keySet().stream()
                    .map(productMap::get)
                    .toList();
            Map<Long, Integer> availableMap = productStockService.getAvailableStockMap(productsWithoutSerial);
            for (Map.Entry<Long, Integer> entry : requestedWithoutSerialMap.entrySet()) {
                Product p = productMap.get(entry.getKey());
                int available = availableMap.getOrDefault(entry.getKey(), 0);
                if (available <= 0) {
                    throw new ConflictException("Sản phẩm '" + p.getName() + "' hiện đã hết hàng.");
                }
                if (entry.getValue() > available) {
                    throw new ConflictException("Sản phẩm '" + p.getName() + "' chỉ còn "
                            + available + " sản phẩm khả dụng.");
                }
            }
        }

        long orderLevelDiscount = (req.getDiscountAmount() != null && req.getDiscountAmount() > 0)
                ? req.getDiscountAmount()
                : 0L;
        long finalDiscountAmount = totalItemDiscounts + orderLevelDiscount;
        long finalAmount = runningTotal - finalDiscountAmount;

        // Lưu thông tin người nhận & ghi chú qua OrderAddressHelper
        String encodedAddress = OrderAddressHelper.encodeAddress(
                req.getAddress(),
                req.getRecipientName(),
                req.getRecipientPhone(),
                req.getNote()
        );

        OrderType orderType = req.getType() != null ? req.getType() : OrderType.ONLINE;

        Order order = Order.builder()
                .code(code)
                .userId(targetUserId)
                .user(user)
                .type(orderType)
                .totalAmount(runningTotal)
                .discountAmount(finalDiscountAmount)
                .finalAmount(finalAmount)
                .status(OrderStatus.PENDING)
                .address(encodedAddress)
                .build();

        for (OrderItem oi : orderItems) {
            oi.setOrder(order);
            order.getItems().add(oi);
        }

        Order saved = orderRepository.saveAndFlush(order);
        if (req.getItems() != null && req.getItems().stream().anyMatch(it -> it.getSerialNumbers() != null && !it.getSerialNumbers().isEmpty())) {
            orderSerialService.reserve(saved, req.getItems());
            orderRepository.flush();
        }
        if (productStockService != null && !orderedProductIds.isEmpty()) {
            productStockService.syncProductStockStatus(orderedProductIds);
        }
        paymentService.initPaymentForNewOrder(saved, req.getPaymentMethod());
        log.info("Đã tạo đơn hàng mới id={}, code={}, userId={}, finalAmount={}",
                saved.getId(), saved.getCode(), saved.getUserId(), saved.getFinalAmount());
        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .module(SystemLogModule.ORDER)
                    .action(SystemLogAction.CREATE)
                    .resourceType("ORDER")
                    .resourceId(saved.getId())
                    .resourceCode(saved.getCode())
                    .description("Tạo đơn bán hàng mới " + saved.getCode() + " với tổng thanh toán " + saved.getFinalAmount())
                    .detailMap(Map.of(
                            "orderCode", saved.getCode(),
                            "finalAmount", saved.getFinalAmount(),
                            "type", saved.getType().name()
                    ))
                    .build());
        }

        return mapOrderWithSerials(saved);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus, Long currentUserId, boolean isStaffOrAdmin) {
        Order order = orderRepository.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", id));

        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == newStatus) {
            return mapOrderWithSerials(order);
        }

        // Quyền khách hàng: chỉ được hủy khi đơn còn PENDING hoặc xác nhận khi đơn đang giao
        if (!isStaffOrAdmin) {
            if (!order.getUserId().equals(currentUserId)) {
                throw new AccessDeniedException("Bạn không có quyền cập nhật trạng thái đơn hàng này.");
            }
            if (newStatus == OrderStatus.CANCELLED) {
                if (currentStatus != OrderStatus.PENDING) {
                    throw new BadRequestException("Đơn hàng ở trạng thái '" + currentStatus + "' không thể tự hủy.");
                }
            } else if (newStatus == OrderStatus.COMPLETED) {
                if (currentStatus != OrderStatus.DELIVERING) {
                    throw new BadRequestException("Chỉ có thể xác nhận nhận hàng khi đơn đang trong quá trình vận chuyển.");
                }
            } else {
                throw new BadRequestException("Khách hàng chỉ được phép hủy đơn chờ duyệt hoặc xác nhận nhận hàng.");
            }
        }

        if (currentStatus == OrderStatus.DELIVERING
                && (newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.RETURNED)
                && deliveryService.existsForResource(DeliveryResourceType.ORDER, id)) {
            throw new BadRequestException("Đơn đang có vận đơn; hãy cập nhật trạng thái vận đơn để hoàn tất hoặc hủy giao.");
        }

        // Kiểm tra máy trạng thái hợp lệ (throw BadRequestException nếu sai luồng)
        validateStatusTransition(currentStatus, newStatus, order.getType(), isStaffOrAdmin);

        if (newStatus == OrderStatus.COMPLETED) {
            orderSerialService.markSold(id);
            order.setStatus(OrderStatus.COMPLETED);
            warrantyActivationService.activateForCompletedOrder(order);
            Order savedOrder = orderRepository.saveAndFlush(order);
            if (productStockService != null) {
                productStockService.syncProductStockStatus(extractProductIds(savedOrder));
            }
            invoiceService.createInvoiceForCompletedOrder(savedOrder);
            paymentService.completePaymentForOrder(savedOrder);
            if (systemLogPublisher != null) {
                systemLogPublisher.publish(SystemLogCreateCommand.builder()
                        .module(SystemLogModule.ORDER)
                        .action(SystemLogAction.UPDATE_STATUS)
                        .resourceType("ORDER")
                        .resourceId(savedOrder.getId())
                        .resourceCode(savedOrder.getCode())
                        .description("Cập nhật trạng thái đơn hàng " + savedOrder.getCode() + " từ " + currentStatus + " sang " + newStatus)
                        .detailMap(Map.of("oldStatus", currentStatus.name(), "newStatus", newStatus.name()))
                        .build());
            }
            return mapOrderWithSerials(savedOrder);
        }

        if (currentStatus == OrderStatus.DELIVERING
                && deliveryService.existsForResource(DeliveryResourceType.ORDER, id)
                && (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.RETURNED)) {
            throw new BadRequestException("Đơn đang giao hàng; hãy cập nhật trạng thái vận đơn để hoàn tất hoặc hủy giao.");
        }

        // Kiểm tra logic luồng chuyển đổi trạng thái hợp lệ
        validateStatusTransition(currentStatus, newStatus);

        if (newStatus == OrderStatus.DELIVERING) {
            orderSerialService.markDelivering(id);
        } else if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.REJECTED || newStatus == OrderStatus.RETURNED) {
            orderSerialService.release(id);
            invoiceService.cancelInvoiceForOrder(id);
            paymentService.cancelPendingPaymentsForOrder(id);
        }

        order.setStatus(newStatus);
        Order updated = orderRepository.saveAndFlush(order);
        if (productStockService != null
                && (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.REJECTED || newStatus == OrderStatus.RETURNED)) {
            productStockService.syncProductStockStatus(extractProductIds(updated));
        }
        log.info("Cập nhật trạng thái đơn hàng id={} từ {} sang {}", id, currentStatus, newStatus);
        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .module(SystemLogModule.ORDER)
                    .action(SystemLogAction.UPDATE_STATUS)
                    .resourceType("ORDER")
                    .resourceId(updated.getId())
                    .resourceCode(updated.getCode())
                    .description("Cập nhật trạng thái đơn hàng " + updated.getCode() + " từ " + currentStatus + " sang " + newStatus)
                    .detailMap(Map.of("oldStatus", currentStatus.name(), "newStatus", newStatus.name()))
                    .build());
        }

        return mapOrderWithSerials(updated);
    }

    /**
     * Giai đoạn 3: Soạn hàng, đóng gói và gán mã định danh (Serial) vào đơn hàng
     * Trạng thái serial chuyển sang PACKING (Đang đóng gói)
     */
    @Transactional
    public OrderResponse assignSerialsToOrder(Long orderId, Map<Long, List<String>> itemSerials) {
        Order order = orderRepository.findForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", orderId));

        if (order.getStatus() != OrderStatus.PENDING
                && order.getStatus() != OrderStatus.CONFIRMED
                && order.getStatus() != OrderStatus.DELIVERING) {
            throw new BadRequestException("Không thể gán hoặc thay đổi serial cho đơn hàng ở trạng thái "
                    + order.getStatus() + ".");
        }

        if (itemSerials == null || itemSerials.isEmpty()) {
            throw new BadRequestException("Danh sách serial quét gán không được để trống.");
        }

        Map<Long, OrderItem> itemMap = order.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, item -> item));

        Map<Long, List<String>> normalizedItemSerials = new LinkedHashMap<>();
        Set<String> allRequestedSerials = new LinkedHashSet<>();

        for (Map.Entry<Long, List<String>> entry : itemSerials.entrySet()) {
            Long orderItemId = entry.getKey();
            OrderItem item = itemMap.get(orderItemId);
            if (item == null) {
                throw new BadRequestException("Mặt hàng id=" + orderItemId + " không thuộc đơn hàng " + order.getCode());
            }

            List<String> rawSerials = entry.getValue();
            int receivedSize = rawSerials == null ? 0 : rawSerials.size();
            if (receivedSize != item.getQuantity()) {
                throw new BadRequestException("Mặt hàng ID " + orderItemId + " yêu cầu đúng "
                        + item.getQuantity() + " mã serial nhưng nhận được " + receivedSize + " mã.");
            }
            if (serialNumbers.size() != item.getQuantity()) {
                throw new BadRequestException("Mặt hàng id=" + orderItemId + " yêu cầu đúng "
                        + item.getQuantity() + " serial, nhưng nhận được " + serialNumbers.size() + ".");
            }
            for (String rawSn : serialNumbers) {
                String cleanSn = rawSn != null ? rawSn.trim() : "";
                if (cleanSn.isEmpty()) {
                    throw new BadRequestException("Mã serial không được để trống.");
                }
                if (!allRequestedSerials.add(cleanSn)) {
                    throw new BadRequestException("Mã serial '" + cleanSn + "' bị trùng lặp trong cùng yêu cầu gán.");
                }
            }
        }

            List<String> trimmedList = new ArrayList<>(receivedSize);
            for (String raw : rawSerials) {
                if (raw == null || raw.isBlank()) {
                    throw new BadRequestException("Mã serial không được để trống.");
                }
                String trimmed = raw.trim();
                if (!allRequestedSerials.add(trimmed)) {
                    throw new BadRequestException("Mã serial bị trùng lặp trong danh sách quét: " + trimmed);
                }
                trimmedList.add(trimmed);
            }
            normalizedItemSerials.put(orderItemId, trimmedList);
        }

        Set<Long> affectedProductIds = new LinkedHashSet<>();
        for (Long orderItemId : normalizedItemSerials.keySet()) {
            OrderItem item = itemMap.get(orderItemId);
            affectedProductIds.add(item.getProductId());

            List<OrderItemSerial> existingOis = orderItemSerialRepository.findAllByOrderItemId(orderItemId);
            if (!existingOis.isEmpty()) {
                List<Long> oldSerialIds = existingOis.stream().map(OrderItemSerial::getProductSerialId).toList();
                List<ProductSerial> oldSerials = productSerialRepository.lockByIds(oldSerialIds);
                for (ProductSerial oldPs : oldSerials) {
                    if (oldPs.getStatus() == ProductSerialStatus.PACKING
                            || oldPs.getStatus() == ProductSerialStatus.DELIVERING) {
                        oldPs.setStatus(ProductSerialStatus.IN_STOCK);
                    }
                }
                productSerialRepository.saveAllAndFlush(oldSerials);
                orderItemSerialRepository.deleteAll(existingOis);
                orderItemSerialRepository.flush();
            }
        }

        List<ProductSerial> lockedSerials = productSerialRepository.lockActiveBySerialNumbers(allRequestedSerials);
        Map<String, ProductSerial> serialByNumber = new HashMap<>();
        for (ProductSerial ps : lockedSerials) {
            if (serialByNumber.put(ps.getSerialNumber(), ps) != null) {
                throw new BadRequestException("Có nhiều serial đang hoạt động cùng mã: " + ps.getSerialNumber());
            }
        }
        if (serialByNumber.size() != allRequestedSerials.size()) {
            List<String> missing = allRequestedSerials.stream()
                    .filter(s -> !serialByNumber.containsKey(s))
                    .toList();
            throw new BadRequestException("Các mã serial không tồn tại trong hệ thống: " + String.join(", ", missing));
        }

        List<ProductSerial> serialsToSave = new ArrayList<>();
        List<OrderItemSerial> linksToSave = new ArrayList<>();
        for (Map.Entry<Long, List<String>> entry : normalizedItemSerials.entrySet()) {
            Long orderItemId = entry.getKey();
            OrderItem item = itemMap.get(orderItemId);

            for (String serialNumber : entry.getValue()) {
                ProductSerial ps = serialByNumber.get(serialNumber);
                if (!ps.getProductId().equals(item.getProductId())) {
                    throw new BadRequestException("Serial '" + ps.getSerialNumber()
                            + "' không thuộc sản phẩm ID: " + item.getProductId());
                }
                if (ps.getStatus() != ProductSerialStatus.IN_STOCK) {
                    throw new BadRequestException("Serial '" + ps.getSerialNumber()
                            + "' không còn trong kho (trạng thái hiện tại: " + ps.getStatus() + ")");
                }

                linksToSave.add(OrderItemSerial.builder()
                        .orderItemId(orderItemId)
                        .productSerialId(ps.getId())
                        .productSerial(ps)
                        .build());
                ps.setStatus(ProductSerialStatus.PACKING);
                serialsToSave.add(ps);
            }
        }
        productSerialRepository.saveAllAndFlush(serialsToSave);
        orderItemSerialRepository.saveAllAndFlush(linksToSave);

        List<OrderItemSerial> allOrderLinks = orderItemSerialRepository.findAllByOrderId(order.getId());
        Map<Long, Long> assignedCounts = allOrderLinks.stream()
                .collect(Collectors.groupingBy(OrderItemSerial::getOrderItemId, Collectors.counting()));
        boolean allItemsFullyAssigned = !order.getItems().isEmpty() && order.getItems().stream()
                .allMatch(it -> assignedCounts.getOrDefault(it.getId(), 0L) == it.getQuantity());

        if (allItemsFullyAssigned
                && (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PENDING)) {
            order.setStatus(OrderStatus.DELIVERING);
            orderRepository.saveAndFlush(order);
        }

        if (productStockService != null && !affectedProductIds.isEmpty()) {
            productStockService.syncProductStockStatus(affectedProductIds);
        }

        log.info("Đã quét và gán serial thành công cho đơn hàng id={}, chuyển trạng thái serial sang PACKING", orderId);
        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .module(SystemLogModule.ORDER)
                    .action(SystemLogAction.ASSIGN_SERIAL)
                    .resourceType("ORDER")
                    .resourceId(order.getId())
                    .resourceCode(order.getCode())
                    .description("Quét gán serial đóng gói cho đơn hàng " + order.getCode())
                    .detailMap(Map.of("orderStatus", order.getStatus().name()))
                    .build());
        }
        return mapOrderWithSerials(order);
    }

    /**
     * Giai đoạn 3 Bước 5: Bàn giao cho ĐVVC
     * Cập nhật trạng thái đơn và các mã serial thành DELIVERING (7 - Đang giao)
     */
    @Transactional
    public OrderResponse handoverToCarrier(Long orderId) {
        Order order = orderRepository.findForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", orderId));

        orderSerialService.markDelivering(orderId);
        log.info("Đơn hàng id={} đã bàn giao ĐVVC, chuyển trạng thái serial sang DELIVERING", orderId);

        return mapOrderWithSerials(order);
    }

    @Transactional
    public void deleteOrder(Long id, boolean isStaffOrAdmin) {
        if (!isStaffOrAdmin) {
            throw new AccessDeniedException("Chỉ Quản trị viên mới có quyền xóa đơn hàng.");
        }

        Order order = orderRepository.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", id));

        if (deliveryService.existsForResource(DeliveryResourceType.ORDER, id)) {
            throw new BadRequestException("Không thể xóa đơn hàng đã có vận đơn.");
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("Không thể xóa đơn hàng đã hoàn tất giao dịch.");
        }

        if (orderRepository.countInvoices(id) > 0) {
            throw new BadRequestException("Không thể xóa đơn hàng đã phát sinh hóa đơn bán hàng.");
        }

        if (orderRepository.countPayments(id) > 0) {
            throw new BadRequestException("Không thể xóa đơn hàng đã có bản ghi thanh toán.");
        }

        String orderCode = order.getCode();
        List<Long> productIds = extractProductIds(order);
        // Hoàn trả serial nếu có
        orderSerialService.release(id);

        orderRepository.delete(order);
        orderRepository.flush();
        if (productStockService != null && !productIds.isEmpty()) {
            productStockService.syncProductStockStatus(productIds);
        }
        log.info("Đã xóa vĩnh viễn đơn hàng id={}, code={}", id, orderCode);
        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .module(SystemLogModule.ORDER)
                    .action(SystemLogAction.DELETE)
                    .resourceType("ORDER")
                    .resourceId(id)
                    .resourceCode(orderCode)
                    .description("Xóa đơn hàng " + orderCode)
                    .build());
        }
    }

    private List<Long> extractProductIds(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            return List.of();
        }
        return order.getItems().stream()
                .map(OrderItem::getProductId)
                .distinct()
                .toList();
    }

    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        boolean allowed = switch (from) {
            case PENDING -> to == OrderStatus.CONFIRMED || to == OrderStatus.REJECTED || to == OrderStatus.CANCELLED;
            case CONFIRMED -> to == OrderStatus.DELIVERING || to == OrderStatus.CANCELLED;
            case DELIVERING -> to == OrderStatus.COMPLETED || to == OrderStatus.CANCELLED || to == OrderStatus.RETURNED;
            case COMPLETED -> to == OrderStatus.RETURNED;
            case REJECTED, CANCELLED, RETURNED -> false;
        };

        if (!allowed) {
            throw new BadRequestException("Không thể chuyển trạng thái đơn hàng từ " + from + " sang " + to + ".");
        }
    }

    private OrderResponse mapOrderWithSerials(Order order) {
        List<OrderItemSerial> serials = orderItemSerialRepository.findAllByOrderId(order.getId());
        Map<Long, List<String>> itemSerialMap = new HashMap<>();

        for (OrderItemSerial ois : serials) {
            ProductSerial ps = ois.getProductSerial();
            if (ps != null && ps.getSerialNumber() != null) {
                itemSerialMap.computeIfAbsent(ois.getOrderItemId(), k -> new ArrayList<>())
                        .add(ps.getSerialNumber());
            }
        }

        Payment latestPayment = paymentRepository.findFirstByOrderIdOrderByIdDesc(order.getId()).orElse(null);
        return orderMapper.toResponse(order, itemSerialMap, latestPayment);
    }

    private String generateUniqueOrderCode() {
        for (int i = 0; i < 10; i++) {
            long ts = System.currentTimeMillis() % 100000000;
            int rand = (int) (Math.random() * 900 + 100);
            String candidate = "DH-" + ts + rand;
            if (!orderRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        return "DH-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }
}
