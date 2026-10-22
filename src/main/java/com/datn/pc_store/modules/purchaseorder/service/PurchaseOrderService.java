package com.datn.pc_store.modules.purchaseorder.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.*;
import com.datn.pc_store.modules.product.enums.ProductStatus;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.purchaseorder.dto.*;
import com.datn.pc_store.modules.purchaseorder.entity.*;
import com.datn.pc_store.modules.purchaseorder.enums.PurchaseOrderStatus;
import com.datn.pc_store.modules.purchaseorder.mapper.PurchaseOrderMapper;
import com.datn.pc_store.modules.purchaseorder.repository.PurchaseOrderRepository;
import com.datn.pc_store.modules.suppliers.enums.SupplierStatus;
import com.datn.pc_store.modules.suppliers.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "code", "name", "supplierId", "orderDate", "expectedDate", "status", "totalAmount",
            "createdDate", "createdBy", "lastModifiedDate", "lastModifiedBy"
    );

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final PurchaseOrderMapper purchaseOrderMapper;

    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderResponse> getAllOrders(String keyword, PurchaseOrderStatus status, Long supplierId,
                                                           Instant fromDate, Instant toDate, Pageable pageable) {
        validateDates(fromDate, toDate);
        for (var sort : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(sort.getProperty())) {
                throw new BadRequestException("Trường sắp xếp không hợp lệ: " + sort.getProperty()
                        + ". Sử dụng sort=createdDate,desc hoặc bỏ tham số sort.");
            }
        }
        return PageResponse.of(purchaseOrderRepository.searchOrders(keyword, status, supplierId, fromDate, toDate, pageable)
                .map(purchaseOrderMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getOrderById(Long id) {
        return purchaseOrderMapper.toResponse(purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn nhập hàng", "id", id)));
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getOrderByCode(String code) {
        return purchaseOrderMapper.toResponse(purchaseOrderRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn nhập hàng", "code", code)));
    }

    @Transactional
    public PurchaseOrderResponse createOrder(PurchaseOrderCreateRequest req) {
        String code = req.getCode().trim();
        validateCode(code, null);
        Instant orderDate = req.getOrderDate() == null ? Instant.now() : req.getOrderDate();
        validateDates(orderDate, req.getExpectedDate());
        validateSupplier(req.getSupplierId());
        List<PurchaseOrderItem> items = prepareItems(req.getItems());
        long total = calculateTotal(items);

        PurchaseOrder order = PurchaseOrder.builder().code(code).name(req.getName()).supplierId(req.getSupplierId())
                .orderDate(orderDate).expectedDate(req.getExpectedDate()).description(req.getDescription())
                .totalAmount(total).build();
        attachItems(order, items);
        PurchaseOrder saved = purchaseOrderRepository.saveAndFlush(order);
        log.info("Đã tạo đơn nhập hàng id: {}", saved.getId());
        return purchaseOrderMapper.toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse updateOrder(Long id, PurchaseOrderUpdateRequest req) {
        PurchaseOrder order = lockOrder(id);
        requireEditable(order);
        String code = req.getCode().trim();
        validateCode(code, id);
        Instant orderDate = req.getOrderDate() == null ? order.getOrderDate() : req.getOrderDate();
        validateDates(orderDate, req.getExpectedDate());
        validateSupplier(req.getSupplierId());
        List<PurchaseOrderItem> items = prepareItems(req.getItems());
        long total = calculateTotal(items);

        order.setCode(code);
        order.setName(req.getName());
        order.setSupplierId(req.getSupplierId());
        order.setOrderDate(orderDate);
        order.setExpectedDate(req.getExpectedDate());
        order.setDescription(req.getDescription());
        order.setTotalAmount(total);
        order.getItems().clear();
        attachItems(order, items);
        log.info("Đã cập nhật đơn nhập hàng id: {}", id);
        return purchaseOrderMapper.toResponse(purchaseOrderRepository.saveAndFlush(order));
    }

    @Transactional
    public PurchaseOrderResponse updateStatus(Long id, PurchaseOrderStatus target) {
        PurchaseOrder order = lockOrder(id);
        PurchaseOrderStatus current = order.getStatus();
        if (current == target) return purchaseOrderMapper.toResponse(order);

        boolean allowed = switch (current) {
            case DRAFT -> target == PurchaseOrderStatus.PENDING || target == PurchaseOrderStatus.CANCELLED;
            case PENDING -> target == PurchaseOrderStatus.APPROVED || target == PurchaseOrderStatus.CANCELLED;
            case APPROVED -> target == PurchaseOrderStatus.RECEIVED || target == PurchaseOrderStatus.CANCELLED;
            case RECEIVED, CANCELLED -> false;
        };
        if (!allowed) throw conflict("Không thể chuyển đơn nhập từ " + current + " sang " + target + ".");

        switch (target) {
            case PENDING, APPROVED -> validateReady(order);
            case CANCELLED -> {
                if (hasReceivedItems(order) || purchaseOrderRepository.countOpenOrConfirmedReceipts(id) > 0) {
                    throw conflict("Không thể hủy đơn đã nhận hàng hoặc có phiếu nhập đang chờ/xác nhận.");
                }
            }
            case RECEIVED -> validateFullyReceived(order);
            default -> throw conflict("Không thể quay lại trạng thái nháp.");
        }
        order.setStatus(target);
        log.info("Chuyển trạng thái đơn nhập hàng id: {} từ {} sang {}", id, current, target);
        return purchaseOrderMapper.toResponse(purchaseOrderRepository.saveAndFlush(order));
    }

    @Transactional
    public void deleteOrder(Long id) {
        PurchaseOrder order = lockOrder(id);
        requireEditable(order);
        // Preserve item history: repository.delete would cascade orphan removal to the items.
        order.setDeleted(true);
        purchaseOrderRepository.saveAndFlush(order);
        log.info("Đã xóa mềm đơn nhập hàng nháp id: {}", id);
    }

    private PurchaseOrder lockOrder(Long id) {
        return purchaseOrderRepository.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn nhập hàng", "id", id));
    }

    private void requireEditable(PurchaseOrder order) {
        if (order.getStatus() != PurchaseOrderStatus.DRAFT || hasReceivedItems(order)
                || purchaseOrderRepository.countReceipts(order.getId()) > 0) {
            throw conflict("Chỉ được sửa hoặc xóa đơn nháp chưa phát sinh phiếu nhập và chưa nhận hàng.");
        }
    }

    private boolean hasReceivedItems(PurchaseOrder order) {
        return order.getItems().stream().anyMatch(item -> item.getQuantityReceived() != 0);
    }

    private void validateReady(PurchaseOrder order) {
        if (order.getItems().isEmpty()) throw new BadRequestException("Đơn nhập phải có ít nhất một mặt hàng trước khi gửi/duyệt.");
        validateSupplier(order.getSupplierId());
        validateDates(order.getOrderDate(), order.getExpectedDate());
        validateProducts(order.getItems().stream().map(PurchaseOrderItem::getProductId).collect(Collectors.toSet()));
        Set<Long> productIds = new HashSet<>();
        for (var item : order.getItems()) {
            if (!productIds.add(item.getProductId()) || item.getQuantityOrder() <= 0 || item.getUnitCost() < 0) {
                throw new BadRequestException("Mặt hàng đặt mua không hợp lệ hoặc bị trùng sản phẩm.");
            }
            try {
                if (item.getSubtotal() != Math.multiplyExact((long) item.getQuantityOrder(), item.getUnitCost())) {
                    throw new BadRequestException("Thành tiền mặt hàng không khớp số lượng và đơn giá.");
                }
            } catch (ArithmeticException ex) {
                throw new BadRequestException("Thành tiền mặt hàng vượt giới hạn cho phép.");
            }
        }
        if (order.getTotalAmount() != calculateTotal(order.getItems())) {
            throw new BadRequestException("Tổng tiền đơn nhập không khớp với chi tiết mặt hàng.");
        }
        if (hasReceivedItems(order) || purchaseOrderRepository.countReceipts(order.getId()) > 0) {
            throw conflict("Đơn nhập đã phát sinh dữ liệu nhận hàng, không thể gửi hoặc duyệt lại.");
        }
    }

    private void validateFullyReceived(PurchaseOrder order) {
        if (order.getItems().isEmpty() || purchaseOrderRepository.countPendingReceipts(order.getId()) > 0) {
            throw conflict("Đơn nhập chưa đủ mặt hàng hoặc còn phiếu nhập chưa xác nhận.");
        }
        Map<Long, Long> received = new HashMap<>();
        for (Object[] row : purchaseOrderRepository.findConfirmedReceivedQuantities(order.getId())) {
            received.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        if (received.size() != order.getItems().size() || order.getItems().stream().anyMatch(item ->
                item.getQuantityReceived() != item.getQuantityOrder()
                        || received.getOrDefault(item.getId(), 0L) != item.getQuantityOrder())) {
            throw conflict("Chỉ đánh dấu đã nhận khi mọi mặt hàng đã nhận đủ và khớp với các phiếu nhập được xác nhận.");
        }
    }

    private void validateCode(String code, Long id) {
        if (purchaseOrderRepository.countByCodeIncludingDeleted(code, id) > 0) {
            throw new DuplicateResourceException("Đơn nhập hàng", "code", code);
        }
    }

    private void validateSupplier(Long id) {
        var supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhà cung cấp", "id", id));
        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            throw new BadRequestException("Nhà cung cấp không còn hoạt động.");
        }
    }

    private List<PurchaseOrderItem> prepareItems(List<PurchaseOrderItemRequest> requests) {
        if (requests == null) return List.of();
        Set<Long> ids = new HashSet<>();
        List<PurchaseOrderItem> items = new ArrayList<>();
        for (var req : requests) {
            if (!ids.add(req.getProductId())) throw new BadRequestException("Sản phẩm không được lặp trong cùng đơn nhập.");
            long subtotal;
            try {
                subtotal = Math.multiplyExact((long) req.getQuantityOrder(), req.getUnitCost());
            } catch (ArithmeticException ex) {
                throw new BadRequestException("Thành tiền mặt hàng vượt giới hạn cho phép.");
            }
            items.add(PurchaseOrderItem.builder().productId(req.getProductId()).quantityOrder(req.getQuantityOrder())
                    .unitCost(req.getUnitCost()).subtotal(subtotal).build());
        }
        validateProducts(ids);
        return items;
    }

    private void validateProducts(Set<Long> ids) {
        var products = productRepository.findAllById(ids).stream().collect(Collectors.toMap(p -> p.getId(), p -> p));
        for (Long id : ids) {
            var product = products.get(id);
            if (product == null) throw new ResourceNotFoundException("Sản phẩm", "id", id);
            if (product.getStatus() == ProductStatus.INACTIVE) {
                throw new BadRequestException("Sản phẩm đã ngừng kinh doanh: " + product.getCode());
            }
        }
    }

    private long calculateTotal(List<PurchaseOrderItem> items) {
        long total = 0;
        try {
            for (var item : items) total = Math.addExact(total, item.getSubtotal());
        } catch (ArithmeticException ex) {
            throw new BadRequestException("Tổng tiền đơn nhập vượt giới hạn cho phép.");
        }
        return total;
    }

    private void attachItems(PurchaseOrder order, List<PurchaseOrderItem> items) {
        for (var item : items) {
            item.setPurchaseOrder(order);
            order.getItems().add(item);
        }
    }

    private void validateDates(Instant start, Instant end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BadRequestException("Ngày kết thúc/giao dự kiến không được trước ngày bắt đầu/đặt hàng.");
        }
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
