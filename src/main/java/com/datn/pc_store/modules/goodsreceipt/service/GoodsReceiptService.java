package com.datn.pc_store.modules.goodsreceipt.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.DuplicateResourceException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.auth.security.CustomUserDetails;
import com.datn.pc_store.modules.goodsreceipt.dto.*;
import com.datn.pc_store.modules.goodsreceipt.entity.GoodsReceipt;
import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import com.datn.pc_store.modules.goodsreceipt.entity.ReceiptItem;
import com.datn.pc_store.modules.goodsreceipt.enums.GoodsReceiptStatus;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import com.datn.pc_store.modules.goodsreceipt.mapper.GoodsReceiptMapper;
import com.datn.pc_store.modules.goodsreceipt.repository.GoodsReceiptRepository;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.purchaseorder.entity.PurchaseOrder;
import com.datn.pc_store.modules.purchaseorder.entity.PurchaseOrderItem;
import com.datn.pc_store.modules.purchaseorder.enums.PurchaseOrderStatus;
import com.datn.pc_store.modules.purchaseorder.repository.PurchaseOrderRepository;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsReceiptService {
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "code", "name", "purchaseOrderId", "receivedAt", "status",
            "createdDate", "createdBy", "lastModifiedDate", "lastModifiedBy"
    );

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final ProductSerialRepository productSerialRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final GoodsReceiptMapper goodsReceiptMapper;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public PageResponse<GoodsReceiptResponse> getAllReceipts(String keyword, GoodsReceiptStatus status,
                                                            Long purchaseOrderId, Instant fromDate, Instant toDate,
                                                            Pageable pageable) {
        validateDates(fromDate, toDate);
        for (var sort : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(sort.getProperty())) {
                throw new BadRequestException("Trường sắp xếp không hợp lệ: " + sort.getProperty()
                        + ". Sử dụng sort=createdDate,desc hoặc bỏ tham số sort.");
            }
        }

        boolean isAdmin = hasRole("ADMIN");
        boolean isManager = hasRole("MANAGER");
        boolean isWarehouse = hasRole("STAFF_WAREHOUSE");

        Long currentUserId = getCurrentUserId();
        int filterMode = 0; // 0: ADMIN (all)
        Collection<GoodsReceiptStatus> managerStatuses = List.of(
                GoodsReceiptStatus.PENDING, GoodsReceiptStatus.CONFIRMED, GoodsReceiptStatus.REJECTED
        );
        GoodsReceiptStatus draftStatus = GoodsReceiptStatus.DRAFT;
        GoodsReceiptStatus cancelledStatus = GoodsReceiptStatus.CANCELLED;

        if (isAdmin) {
            filterMode = 0;
        } else if (isManager) {
            // Manager: Thấy toàn bộ phiếu ở trạng thái PENDING, CONFIRMED, REJECTED
            // hoặc các phiếu ở trạng thái DRAFT / CANCELLED do chính manager tự tạo.
            // Không thấy phiếu CANCELLED hoặc DRAFT do người khác tạo.
            if (currentUserId == null && (status == GoodsReceiptStatus.CANCELLED || status == GoodsReceiptStatus.DRAFT)) {
                return PageResponse.of(Page.<GoodsReceipt>empty(pageable).map(goodsReceiptMapper::toResponse));
            }
            filterMode = 2;
        } else if (isWarehouse) {
            // Role STAFF_WAREHOUSE: chỉ hiển thị các bản ghi do chính người dùng tạo.
            if (currentUserId == null) {
                return PageResponse.of(Page.<GoodsReceipt>empty(pageable).map(goodsReceiptMapper::toResponse));
            }
            filterMode = 1;
        }

        Page<GoodsReceipt> page = goodsReceiptRepository.searchGoodsReceipts(
                keyword, status, filterMode, currentUserId, managerStatuses, draftStatus, cancelledStatus,
                purchaseOrderId, fromDate, toDate, pageable
        );
        return PageResponse.of(page.map(goodsReceiptMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public GoodsReceiptResponse getReceiptById(Long id) {
        GoodsReceipt receipt = goodsReceiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu nhập hàng", "id", id));
        validateReadPermission(receipt);
        return goodsReceiptMapper.toResponse(receipt);
    }

    @Transactional(readOnly = true)
    public GoodsReceiptResponse getReceiptByCode(String code) {
        GoodsReceipt receipt = goodsReceiptRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu nhập hàng", "code", code));
        validateReadPermission(receipt);
        return goodsReceiptMapper.toResponse(receipt);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderItemReceiptInfo> getOrderItemsForReceipt(Long purchaseOrderId) {
        PurchaseOrder po = validateAndGetPurchaseOrder(purchaseOrderId);
        List<Long> productIds = po.getItems().stream()
                .map(PurchaseOrderItem::getProductId)
                .distinct()
                .toList();

        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        return po.getItems().stream().map(poi -> {
            Product product = productMap.get(poi.getProductId());
            int remaining = Math.max(0, poi.getQuantityOrder() - poi.getQuantityReceived());
            return PurchaseOrderItemReceiptInfo.builder()
                    .id(poi.getId())
                    .productId(poi.getProductId())
                    .productCode(product != null ? product.getCode() : null)
                    .productName(product != null ? product.getName() : null)
                    .quantityOrder(poi.getQuantityOrder())
                    .quantityReceived(poi.getQuantityReceived())
                    .remainingQuantity(remaining)
                    .unitCost(poi.getUnitCost())
                    .build();
        }).toList();
    }

    @Transactional
    public GoodsReceiptResponse createReceipt(GoodsReceiptCreateRequest req) {
        String code = req.getCode().trim();
        validateCode(code, null);

        PurchaseOrder po = validateAndGetPurchaseOrder(req.getPurchaseOrderId());

        GoodsReceiptStatus targetStatus = req.getStatus() == null ? GoodsReceiptStatus.PENDING : req.getStatus();
        if (targetStatus != GoodsReceiptStatus.PENDING && targetStatus != GoodsReceiptStatus.DRAFT) {
            throw new BadRequestException("Khi khởi tạo phiếu nhập hàng, trạng thái chỉ có thể là PENDING hoặc DRAFT.");
        }

        Long currentUserId = getCurrentUserId();

        GoodsReceipt receipt = GoodsReceipt.builder()
                .code(code)
                .name(req.getName())
                .purchaseOrderId(po.getId())
                .receivedAt(req.getReceivedAt())
                .status(targetStatus)
                .build();

        if (currentUserId != null) {
            receipt.setCreatedBy(currentUserId);
        }

        List<ReceiptItem> items = prepareItems(receipt, req.getItems(), po, null);
        if (targetStatus == GoodsReceiptStatus.PENDING) {
            validateItemsForPending(items);
        }

        attachItems(receipt, items);
        GoodsReceipt saved = goodsReceiptRepository.saveAndFlush(receipt);
        log.info("Đã tạo phiếu nhập hàng id: {} với trạng thái: {}", saved.getId(), saved.getStatus());
        return goodsReceiptMapper.toResponse(saved);
    }

    @Transactional
    public GoodsReceiptResponse updateReceipt(Long id, GoodsReceiptUpdateRequest req) {
        GoodsReceipt receipt = lockReceipt(id);
        requireEditable(receipt);

        Long currentUserId = getCurrentUserId();
        if (receipt.getCreatedBy() != null && currentUserId != null
                && !currentUserId.equals(receipt.getCreatedBy()) && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ người tạo mới có thể chỉnh sửa phiếu nhập nháp.");
        }

        String code = req.getCode().trim();
        validateCode(code, id);

        PurchaseOrder po = validateAndGetPurchaseOrder(receipt.getPurchaseOrderId());

        Set<String> existingReceiptSerials = receipt.getItems().stream()
                .flatMap(i -> i.getProductSerials().stream())
                .map(ProductSerial::getSerialNumber)
                .collect(Collectors.toSet());

        receipt.getItems().clear();
        entityManager.flush();

        List<ReceiptItem> items = prepareItems(receipt, req.getItems(), po, existingReceiptSerials);

        receipt.setCode(code);
        receipt.setName(req.getName());
        receipt.setReceivedAt(req.getReceivedAt());
        attachItems(receipt, items);

        GoodsReceipt saved = goodsReceiptRepository.saveAndFlush(receipt);
        log.info("Đã cập nhật phiếu nhập hàng nháp id: {}", id);
        return goodsReceiptMapper.toResponse(saved);
    }

    @Transactional
    public GoodsReceiptResponse updateStatus(Long id, GoodsReceiptStatus target) {
        GoodsReceipt receipt = lockReceipt(id);
        GoodsReceiptStatus current = receipt.getStatus();
        if (current == target) {
            return goodsReceiptMapper.toResponse(receipt);
        }

        boolean isAdmin = hasRole("ADMIN");
        boolean isManager = hasRole("MANAGER");
        Long currentUserId = getCurrentUserId();
        Long createdBy = receipt.getCreatedBy();

        // Edge case: Không cho phép chuyển trạng thái phiếu nhập kho đã bị hủy
        if (current == GoodsReceiptStatus.CANCELLED) {
            throw conflict("phiếu nhập kho đã được hủy, không thực hiện được thao tác này");
        }

        // Không cho phép chuyển từ PENDING sang DRAFT
        if (current == GoodsReceiptStatus.PENDING && target == GoodsReceiptStatus.DRAFT) {
            throw conflict("Không cho phép chuyển phiếu nhập hàng từ PENDING sang DRAFT.");
        }

        boolean allowed = switch (current) {
            case DRAFT -> target == GoodsReceiptStatus.PENDING;
            case PENDING -> target == GoodsReceiptStatus.CONFIRMED
                    || target == GoodsReceiptStatus.REJECTED
                    || target == GoodsReceiptStatus.CANCELLED;
            case CONFIRMED, REJECTED, CANCELLED -> false;
        };

        if (!allowed) {
            throw conflict("Không thể chuyển phiếu nhập hàng từ " + current + " sang " + target + ".");
        }

        switch (target) {
            case PENDING -> {
                // Người tạo (hoặc Admin) chuyển từ DRAFT sang PENDING khi thực hiện gửi
                if (createdBy != null && currentUserId != null && !currentUserId.equals(createdBy) && !isAdmin) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ người tạo mới có thể gửi duyệt phiếu nhập hàng (PENDING).");
                }
                validateItemsForPending(receipt.getItems());
            }
            case CANCELLED -> {
                // "chỉ cho phép chuyển từ pending sang cancelled (người tạo tự chuyển)"
                if (createdBy != null && currentUserId != null && !currentUserId.equals(createdBy) && !isAdmin) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ người tạo mới có quyền hủy phiếu nhập hàng (CANCELLED).");
                }
                terminateAllSerials(receipt);
            }
            case CONFIRMED, REJECTED -> {
                // "Từ PENDING sang CONFIRMED hoặc REJECTED:
                // Chỉ role manager hoặc admin được chuyển, không bắt buộc chính mình không được tự chuyển (vì admin và manager có thể tự tạo và tự duyệt)"
                if (!isAdmin && !isManager) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Quản lý (MANAGER) hoặc Quản trị viên (ADMIN) mới có quyền xác nhận hoặc từ chối phiếu nhập hàng.");
                }

                if (target == GoodsReceiptStatus.CONFIRMED) {
                    validateReadyForConfirm(receipt);
                    // Serial GOOD_CONDITION -> IN_STOCK, DEFECTIVE -> PENDING_RETURN
                    for (ReceiptItem item : receipt.getItems()) {
                        for (ProductSerial serial : item.getProductSerials()) {
                            if (serial.getStatus() == ProductSerialStatus.GOOD_CONDITION) {
                                serial.setStatus(ProductSerialStatus.IN_STOCK);
                            } else if (serial.getStatus() == ProductSerialStatus.DEFECTIVE) {
                                serial.setStatus(ProductSerialStatus.PENDING_RETURN);
                            }
                        }
                    }
                    if (receipt.getReceivedAt() == null) {
                        receipt.setReceivedAt(Instant.now());
                    }
                    receipt.setReceivedBy(currentUserId);
                    updatePurchaseOrderReceivedQuantities(receipt);
                } else {
                    // REJECTED
                    terminateAllSerials(receipt);
                }
            }
            default -> throw conflict("Trạng thái chuyển tiếp không hợp lệ.");
        }

        receipt.setStatus(target);
        GoodsReceipt saved = goodsReceiptRepository.saveAndFlush(receipt);
        log.info("Chuyển trạng thái phiếu nhập hàng id: {} từ {} sang {}", id, current, target);
        return goodsReceiptMapper.toResponse(saved);
    }

    @Transactional
    public void deleteReceipt(Long id) {
        GoodsReceipt receipt = lockReceipt(id);

        // "Chỉ cho phép xóa ở trạng thái draft, các trạng thái như pending, confirmed, rejected, cancelled không được xóa."
        if (receipt.getStatus() != GoodsReceiptStatus.DRAFT) {
            throw conflict("Chỉ cho phép xóa phiếu nhập ở trạng thái lưu nháp (DRAFT). Các trạng thái pending, confirmed, rejected, cancelled không được xóa.");
        }

        Long currentUserId = getCurrentUserId();
        if (receipt.getCreatedBy() != null && currentUserId != null
                && !currentUserId.equals(receipt.getCreatedBy()) && !hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ người tạo mới có quyền xóa phiếu nhập nháp.");
        }

        // "Khi xóa ở trạng thái draft sẽ là xóa cứng (xóa hẳn khỏi hệ thống từ goodRecepit, receiptItem, product_serial)."
        // Nhờ CascadeType.ALL và orphanRemoval = true trên cả goodsReceipt -> items và item -> productSerials,
        // toàn bộ receipt, receipt_items và product_serials sẽ được xóa sạch khỏi cơ sở dữ liệu.
        goodsReceiptRepository.delete(receipt);
        goodsReceiptRepository.flush();
        log.info("Đã xóa cứng phiếu nhập hàng nháp id: {}", id);
    }

    private void terminateAllSerials(GoodsReceipt receipt) {
        for (ReceiptItem item : receipt.getItems()) {
            for (ProductSerial serial : item.getProductSerials()) {
                serial.setTerminated(true);
            }
        }
    }

    private void validateReadPermission(GoodsReceipt receipt) {
        boolean isAdmin = hasRole("ADMIN");
        boolean isManager = hasRole("MANAGER");
        boolean isWarehouse = hasRole("STAFF_WAREHOUSE");

        if (isAdmin) {
            return;
        }

        Long currentUserId = getCurrentUserId();

        if (isManager) {
            // Manager thấy tất cả PENDING, CONFIRMED, REJECTED.
            // Đối với CANCELLED hoặc DRAFT, chỉ thấy nếu do chính mình tạo.
            if (receipt.getStatus() == GoodsReceiptStatus.CANCELLED || receipt.getStatus() == GoodsReceiptStatus.DRAFT) {
                if (receipt.getCreatedBy() != null && currentUserId != null
                        && !receipt.getCreatedBy().equals(currentUserId)) {
                    throw new ResourceNotFoundException("Phiếu nhập hàng", "id", receipt.getId());
                }
            }
            return;
        }

        if (isWarehouse) {
            if (receipt.getCreatedBy() != null && currentUserId != null
                    && !receipt.getCreatedBy().equals(currentUserId)) {
                throw new ResourceNotFoundException("Phiếu nhập hàng", "id", receipt.getId());
            }
        }
    }

    private GoodsReceipt lockReceipt(Long id) {
        return goodsReceiptRepository.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu nhập hàng", "id", id));
    }

    private void requireEditable(GoodsReceipt receipt) {
        if (receipt.getStatus() != GoodsReceiptStatus.DRAFT) {
            throw conflict("Chỉ có thể chỉnh sửa phiếu nhập hàng khi ở trạng thái lưu nháp (DRAFT).");
        }
    }

    private PurchaseOrder validateAndGetPurchaseOrder(Long purchaseOrderId) {
        PurchaseOrder po = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn đặt hàng (Purchase Order)", "id", purchaseOrderId));
        if (po.isDeleted()) {
            throw new BadRequestException("Đơn đặt hàng đã bị xóa.");
        }
        if (po.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw conflict("Chỉ được tạo phiếu nhập cho đơn đặt hàng đã duyệt (APPROVED). Trạng thái hiện tại: " + po.getStatus());
        }
        return po;
    }

    private List<ReceiptItem> prepareItems(GoodsReceipt receipt, List<ReceiptItemRequest> itemRequests,
                                           PurchaseOrder po, Set<String> allowReuseSerials) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, PurchaseOrderItem> poItemMap = po.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getId, i -> i));

        // 1. Kiểm tra tính hợp lệ của receiptItem theo đơn đặt hàng
        Set<Long> seenPoiIds = new HashSet<>();
        List<Long> duplicatePoiIds = new ArrayList<>();
        for (ReceiptItemRequest req : itemRequests) {
            if (req.getPurchaseOrderItemId() != null && !seenPoiIds.add(req.getPurchaseOrderItemId())) {
                duplicatePoiIds.add(req.getPurchaseOrderItemId());
            }
        }
        if (!duplicatePoiIds.isEmpty()) {
            throw new BadRequestException("Mỗi dòng đơn đặt hàng chỉ được thêm một lần trong cùng phiếu nhập. Các ID bị lặp: " + duplicatePoiIds);
        }

        // Nếu lỗi receiptItem không nằm trong danh sách sẽ gửi luôn lỗi không tồn tại những cái nào
        List<Long> invalidPoiIds = new ArrayList<>();
        for (ReceiptItemRequest req : itemRequests) {
            if (req.getPurchaseOrderItemId() == null || !poItemMap.containsKey(req.getPurchaseOrderItemId())) {
                invalidPoiIds.add(req.getPurchaseOrderItemId());
            }
        }
        if (!invalidPoiIds.isEmpty()) {
            throw new BadRequestException("Các dòng mặt hàng thực nhận sau không tồn tại trong đơn đặt hàng mã " + po.getCode() + ": " + invalidPoiIds);
        }

        // 2. Cuối cùng mới check serial_number:
        // - Nếu bỏ trống serial_number sẽ phản hồi hàng loạt mục bỏ trống
        // - Nếu trùng serial_number sẽ gửi hàng loạt các serial_number bị trùng, không gửi đơn lẻ: Lỗi ở đâu, trả lỗi ở đó
        List<String> blankSerialErrors = new ArrayList<>();
        List<String> invalidStatusErrors = new ArrayList<>();
        Map<String, List<String>> serialPositions = new LinkedHashMap<>();

        for (int itemIdx = 0; itemIdx < itemRequests.size(); itemIdx++) {
            ReceiptItemRequest itemReq = itemRequests.get(itemIdx);
            PurchaseOrderItem poi = poItemMap.get(itemReq.getPurchaseOrderItemId());
            List<ProductSerialRequest> serialReqs = itemReq.getProductSerials();
            if (serialReqs == null || serialReqs.isEmpty()) {
                continue;
            }

            for (int sIdx = 0; sIdx < serialReqs.size(); sIdx++) {
                ProductSerialRequest sReq = serialReqs.get(sIdx);
                String posDesc = "Dòng đặt hàng #" + poi.getId() + " (vị trí dòng " + (itemIdx + 1) + "), serial thứ " + (sIdx + 1);

                if (sReq.getSerialNumber() == null || sReq.getSerialNumber().trim().isEmpty()) {
                    blankSerialErrors.add(posDesc + ": Số serial bị bỏ trống");
                } else {
                    String trimmed = sReq.getSerialNumber().trim();
                    String upper = trimmed.toUpperCase();
                    serialPositions.computeIfAbsent(upper, k -> new ArrayList<>()).add(posDesc + " ('" + trimmed + "')");
                }

                if (sReq.getStatus() == null || (sReq.getStatus() != ProductSerialStatus.GOOD_CONDITION && sReq.getStatus() != ProductSerialStatus.DEFECTIVE)) {
                    invalidStatusErrors.add(posDesc + ": Trạng thái serial không hợp lệ (" + sReq.getStatus() + "). Chỉ chấp nhận GOOD_CONDITION hoặc DEFECTIVE");
                }
            }
        }

        // Gom các serial bị trùng lặp trong chính payload gửi lên
        List<String> payloadDuplicateErrors = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : serialPositions.entrySet()) {
            if (entry.getValue().size() > 1) {
                payloadDuplicateErrors.add(entry.getKey() + " (xuất hiện " + entry.getValue().size() + " lần)");
            }
        }

        // Gom các serial bị trùng với serial đang hoạt động trong hệ thống (is_terminated = false)
        List<String> dbDuplicateErrors = new ArrayList<>();
        if (!serialPositions.isEmpty()) {
            Set<String> existingActive = productSerialRepository.findActiveExistingSerialNumbers(serialPositions.keySet());
            if (allowReuseSerials != null) {
                Set<String> upperAllowed = allowReuseSerials.stream().map(String::toUpperCase).collect(Collectors.toSet());
                existingActive.removeIf(upperAllowed::contains);
            }
            if (!existingActive.isEmpty()) {
                dbDuplicateErrors.addAll(existingActive);
            }
        }

        // Nếu có bất kỳ lỗi nào về serial (trống, trùng lặp trong payload, sai trạng thái):
        if (!blankSerialErrors.isEmpty() || !payloadDuplicateErrors.isEmpty() || !invalidStatusErrors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Phát hiện lỗi trong danh sách serial sản phẩm:");
            if (!blankSerialErrors.isEmpty()) {
                sb.append("\n- Các serial bị bỏ trống (").append(blankSerialErrors.size()).append(" mục):\n  + ")
                        .append(String.join("\n  + ", blankSerialErrors));
            }
            if (!payloadDuplicateErrors.isEmpty()) {
                sb.append("\n- Các số serial bị trùng lặp trong dữ liệu gửi lên (").append(payloadDuplicateErrors.size()).append(" mục):\n  + ")
                        .append(String.join("\n  + ", payloadDuplicateErrors));
            }
            if (!dbDuplicateErrors.isEmpty()) {
                sb.append("\n- Các số serial đã tồn tại và đang hoạt động trong hệ thống (is_terminated = false) (").append(dbDuplicateErrors.size()).append(" mục):\n  + ")
                        .append(String.join(", ", dbDuplicateErrors));
            }
            if (!invalidStatusErrors.isEmpty()) {
                sb.append("\n- Trạng thái serial không hợp lệ:\n  + ")
                        .append(String.join("\n  + ", invalidStatusErrors));
            }
            throw new BadRequestException(sb.toString());
        }

        // Nếu chỉ trùng với serial đang hoạt động trong DB -> trả về DuplicateResourceException (HTTP 409 Conflict) với toàn bộ danh sách
        if (!dbDuplicateErrors.isEmpty()) {
            throw new DuplicateResourceException("Số serial đang hoạt động đã tồn tại trong hệ thống (is_terminated = false): " + String.join(", ", dbDuplicateErrors));
        }

        List<ReceiptItem> items = new ArrayList<>();
        for (ReceiptItemRequest req : itemRequests) {
            PurchaseOrderItem poi = poItemMap.get(req.getPurchaseOrderItemId());

            ReceiptItem item = ReceiptItem.builder()
                    .goodsReceipt(receipt)
                    .purchaseOrderItemId(poi.getId())
                    .actualUnitCost(req.getActualUnitCost())
                    .build();

            List<ProductSerial> serialEntities = new ArrayList<>();
            if (req.getProductSerials() != null) {
                for (ProductSerialRequest sReq : req.getProductSerials()) {
                    ProductSerial ps = ProductSerial.builder()
                            .productId(poi.getProductId())
                            .receiptItem(item)
                            .serialNumber(sReq.getSerialNumber().trim())
                            .status(sReq.getStatus())
                            .isTerminated(false)
                            .build();
                    serialEntities.add(ps);
                }
            }

            int qtyReceived = serialEntities.size();
            int qtyAccepted = (int) serialEntities.stream().filter(s -> s.getStatus() == ProductSerialStatus.GOOD_CONDITION).count();
            int qtyDefective = (int) serialEntities.stream().filter(s -> s.getStatus() == ProductSerialStatus.DEFECTIVE).count();

            item.setQuantityReceived(qtyReceived);
            item.setQuantityAccepted(qtyAccepted);
            item.setQuantityDefective(qtyDefective);
            item.getProductSerials().addAll(serialEntities);

            items.add(item);
        }

        return items;
    }

    private void validateItemsForPending(List<ReceiptItem> items) {
        if (items.isEmpty()) {
            throw new BadRequestException("Chưa thêm danh sách mặt hàng thực nhận (receiptItem) và serial sản phẩm (productSerial), không thể lưu phiếu nhập hàng.");
        }
        for (ReceiptItem item : items) {
            if (item.getQuantityReceived() <= 0) {
                throw new BadRequestException("Chưa thêm serial sản phẩm (productSerial) cho tất cả các mặt hàng thực nhận, không thể lưu phiếu nhập hàng.");
            }
        }
    }

    private void validateReadyForConfirm(GoodsReceipt receipt) {
        validateItemsForPending(receipt.getItems());
        PurchaseOrder po = validateAndGetPurchaseOrder(receipt.getPurchaseOrderId());

        Map<Long, PurchaseOrderItem> poItemMap = po.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getId, i -> i));

        for (ReceiptItem item : receipt.getItems()) {
            PurchaseOrderItem poi = poItemMap.get(item.getPurchaseOrderItemId());
            if (poi == null) {
                throw new BadRequestException("Mặt hàng không còn tồn tại trong đơn đặt hàng.");
            }
            if (poi.getQuantityReceived() + item.getQuantityReceived() > poi.getQuantityOrder()) {
                throw new BadRequestException("Số lượng nhận của dòng hàng (ID " + poi.getId()
                        + ") vượt quá số lượng đặt mua (" + poi.getQuantityOrder() + "). Hiện đã nhận: "
                        + poi.getQuantityReceived() + ", phiếu này nhận: " + item.getQuantityReceived());
            }
        }
    }

    private void updatePurchaseOrderReceivedQuantities(GoodsReceipt receipt) {
        PurchaseOrder po = purchaseOrderRepository.findById(receipt.getPurchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Đơn đặt hàng", "id", receipt.getPurchaseOrderId()));

        Map<Long, PurchaseOrderItem> poItemMap = po.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getId, i -> i));

        for (ReceiptItem ri : receipt.getItems()) {
            PurchaseOrderItem poi = poItemMap.get(ri.getPurchaseOrderItemId());
            if (poi != null) {
                poi.setQuantityReceived(poi.getQuantityReceived() + ri.getQuantityReceived());
            }
        }
        purchaseOrderRepository.saveAndFlush(po);
    }

    private void attachItems(GoodsReceipt receipt, List<ReceiptItem> items) {
        for (ReceiptItem item : items) {
            item.setGoodsReceipt(receipt);
            receipt.getItems().add(item);
        }
    }

    private void validateCode(String code, Long id) {
        if (goodsReceiptRepository.countByCode(code, id) > 0) {
            throw new DuplicateResourceException("Phiếu nhập hàng", "code", code);
        }
    }

    private void validateDates(Instant start, Instant end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BadRequestException("Thời gian kết thúc không được trước thời gian bắt đầu.");
        }
    }

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private Long getCurrentUserId() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        if (auth.getPrincipal() instanceof CustomUserDetails principal) {
            return principal.getId();
        }
        if (auth.getPrincipal() instanceof UserDetails ud) {
            return userRepository.findByUsername(ud.getUsername()).map(User::getId).orElse(null);
        }
        return null;
    }

    private boolean hasRole(String role) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        String target = "ROLE_" + role;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(target));
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
