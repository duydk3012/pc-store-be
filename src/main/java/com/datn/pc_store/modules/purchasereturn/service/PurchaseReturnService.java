package com.datn.pc_store.modules.purchasereturn.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.ConflictException;
import com.datn.pc_store.common.exception.DuplicateResourceException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.common.util.PaginationUtils;
import com.datn.pc_store.common.util.SecurityUtils;
import com.datn.pc_store.modules.delivery.enums.DeliveryResourceType;
import com.datn.pc_store.modules.delivery.service.DeliveryService;
import com.datn.pc_store.modules.goodsreceipt.entity.GoodsReceipt;
import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import com.datn.pc_store.modules.goodsreceipt.entity.ReceiptItem;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.purchaseorder.entity.PurchaseOrder;
import com.datn.pc_store.modules.purchaseorder.repository.PurchaseOrderRepository;
import com.datn.pc_store.modules.purchasereturn.dto.*;
import com.datn.pc_store.modules.purchasereturn.entity.PurchaseReturn;
import com.datn.pc_store.modules.purchasereturn.entity.PurchaseReturnItem;
import com.datn.pc_store.modules.purchasereturn.enums.PurchaseReturnStatus;
import com.datn.pc_store.modules.purchasereturn.mapper.PurchaseReturnMapper;
import com.datn.pc_store.modules.purchasereturn.repository.PurchaseReturnItemRepository;
import com.datn.pc_store.modules.purchasereturn.repository.PurchaseReturnRepository;
import com.datn.pc_store.modules.suppliers.entity.Supplier;
import com.datn.pc_store.modules.suppliers.repository.SupplierRepository;
import com.datn.pc_store.modules.systemlog.dto.SystemLogCreateCommand;
import com.datn.pc_store.modules.systemlog.enums.SystemLogAction;
import com.datn.pc_store.modules.systemlog.enums.SystemLogModule;
import com.datn.pc_store.modules.systemlog.event.SystemLogPublisher;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
public class PurchaseReturnService {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "code", "name", "supplierId", "returnDate", "status", "totalAmount",
            "createdDate", "createdBy", "lastModifiedDate", "lastModifiedBy"
    );

    private static final Set<ProductSerialStatus> ELIGIBLE_SERIAL_STATUSES = Set.of(
            ProductSerialStatus.DEFECTIVE,
            ProductSerialStatus.PENDING_RETURN,
            ProductSerialStatus.IN_STOCK
    );

    private final PurchaseReturnRepository purchaseReturnRepository;
    private final PurchaseReturnItemRepository purchaseReturnItemRepository;
    private final ProductSerialRepository productSerialRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final PurchaseReturnMapper purchaseReturnMapper;
    private final EntityManager entityManager;
    private final DeliveryService deliveryService;
    private final SystemLogPublisher systemLogPublisher;

    @Transactional(readOnly = true)
    public PageResponse<PurchaseReturnResponse> getAllReturns(
            String keyword,
            PurchaseReturnStatus status,
            Long supplierId,
            Instant fromDate,
            Instant toDate,
            Pageable pageable
    ) {
        PaginationUtils.validateDateRange(fromDate, toDate);
        PaginationUtils.validateSort(pageable, SORTABLE_FIELDS);

        boolean isAdmin = SecurityUtils.hasRole("ADMIN");
        boolean isManager = SecurityUtils.hasRole("MANAGER");
        boolean isWarehouse = SecurityUtils.hasRole("STAFF_WAREHOUSE");

        Long currentUserId = SecurityUtils.getCurrentUserId();
        int filterMode = 0; // 0: ADMIN (all)
        Collection<PurchaseReturnStatus> managerStatuses = List.of(
                PurchaseReturnStatus.PENDING,
                PurchaseReturnStatus.DELIVERING,
                PurchaseReturnStatus.COMPLETED
        );
        PurchaseReturnStatus returnedStatus = PurchaseReturnStatus.RETURNED;
        PurchaseReturnStatus cancelledStatus = PurchaseReturnStatus.CANCELLED;

        if (isAdmin) {
            filterMode = 0;
        } else if (isManager) {
            if (currentUserId == null && (status == PurchaseReturnStatus.CANCELLED || status == PurchaseReturnStatus.RETURNED)) {
                return PageResponse.of(Page.<PurchaseReturn>empty(pageable).map(purchaseReturnMapper::toResponse));
            }
            filterMode = 2;
        } else if (isWarehouse) {
            if (currentUserId == null) {
                return PageResponse.of(Page.<PurchaseReturn>empty(pageable).map(purchaseReturnMapper::toResponse));
            }
            filterMode = 1;
        }

        Page<PurchaseReturn> page = purchaseReturnRepository.searchReturns(
                keyword, status, supplierId, filterMode, currentUserId,
                managerStatuses, returnedStatus, cancelledStatus, fromDate, toDate, pageable
        );

        return PageResponse.of(page.map(this::toEnrichedResponse));
    }

    @Transactional(readOnly = true)
    public PurchaseReturnResponse getReturnById(Long id) {
        PurchaseReturn pr = purchaseReturnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu hoàn trả", "id", id));
        validateReadPermission(pr);
        return toEnrichedResponse(pr);
    }

    @Transactional(readOnly = true)
    public PurchaseReturnResponse getReturnByCode(String code) {
        PurchaseReturn pr = purchaseReturnRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu hoàn trả", "code", code));
        validateReadPermission(pr);
        return toEnrichedResponse(pr);
    }

    @Transactional
    public PurchaseReturnResponse createReturn(PurchaseReturnCreateRequest req) {
        String code = req.getCode() != null ? req.getCode().trim() : "";
        validateCode(code, null);

        Supplier supplier = validateAndGetSupplier(req.getSupplierId());

        PurchaseReturnStatus targetStatus = req.getStatus() == null ? PurchaseReturnStatus.PENDING : req.getStatus();
        if (targetStatus != PurchaseReturnStatus.PENDING) {
            throw new BadRequestException("Khi khởi tạo phiếu hoàn trả, trạng thái chỉ có thể là PENDING.");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();

        PurchaseReturn pr = PurchaseReturn.builder()
                .code(code)
                .name(req.getName())
                .supplierId(supplier.getId())
                .reason(req.getReason())
                .description(req.getDescription())
                .returnDate(req.getReturnDate())
                .status(targetStatus)
                .build();

        if (currentUserId != null) {
            pr.setCreatedBy(currentUserId);
        }

        List<ProductSerial> serials = resolveAndValidateSerials(
                req.getSerialNumbers(),
                req.getProductSerialIds(),
                supplier.getId(),
                null
        );

        if (serials.isEmpty()) {
            throw new BadRequestException("Phiếu hoàn trả phải có ít nhất một serial sản phẩm.");
        }

        List<PurchaseReturnItem> items = new ArrayList<>();
        long totalAmount = 0L;

        for (ProductSerial serial : serials) {
            long unitCost = getSerialUnitCost(serial);
            try {
                totalAmount = Math.addExact(totalAmount, unitCost);
            } catch (ArithmeticException ex) {
                throw new BadRequestException("Tổng tiền hoàn trả vượt giới hạn cho phép.");
            }

            PurchaseReturnItem item = PurchaseReturnItem.builder()
                    .purchaseReturn(pr)
                    .productSerial(serial)
                    .unitCost(unitCost)
                    .build();
            if (currentUserId != null) {
                item.setCreatedBy(currentUserId);
            }
            items.add(item);

            if (targetStatus == PurchaseReturnStatus.PENDING) {
                serial.setStatus(ProductSerialStatus.PENDING_RETURN);
            }
        }

        pr.setTotalAmount(totalAmount);
        pr.setItems(items);

        PurchaseReturn saved = purchaseReturnRepository.saveAndFlush(pr);
        log.info("Đã tạo phiếu hoàn trả NCC id: {} với trạng thái: {}", saved.getId(), saved.getStatus());
        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .module(SystemLogModule.PURCHASE_RETURN)
                    .action(SystemLogAction.CREATE)
                    .resourceType("PURCHASE_RETURN")
                    .resourceId(saved.getId())
                    .resourceCode(saved.getCode())
                    .description("Tạo phiếu hoàn trả nhà cung cấp " + saved.getCode())
                    .detailMap(Map.of("supplierId", saved.getSupplierId(), "totalAmount", saved.getTotalAmount()))
                    .build());
        }
        return toEnrichedResponse(saved);
    }

    @Transactional
    public PurchaseReturnResponse updateReturn(Long id, PurchaseReturnUpdateRequest req) {
        PurchaseReturn pr = lockReturn(id);
        requireEditable(pr);

        Long currentUserId = SecurityUtils.getCurrentUserId();

        String code = req.getCode() != null ? req.getCode().trim() : "";
        validateCode(code, id);

        List<ProductSerial> serials = resolveAndValidateSerials(
                req.getSerialNumbers(),
                req.getProductSerialIds(),
                pr.getSupplierId(),
                id
        );
        if (serials.isEmpty()) {
            throw new BadRequestException("Phiếu hoàn trả phải có ít nhất một serial sản phẩm.");
        }

        // Giải phóng các serial cũ đang giữ PENDING_RETURN trước khi thay thế danh sách mới
        for (PurchaseReturnItem oldItem : pr.getItems()) {
            ProductSerial oldPs = oldItem.getProductSerial();
            if (oldPs != null && oldPs.getStatus() == ProductSerialStatus.PENDING_RETURN) {
                oldPs.setStatus(ProductSerialStatus.DEFECTIVE);
            }
        }

        pr.setCode(code);
        pr.setName(req.getName());
        pr.setReason(req.getReason());
        pr.setDescription(req.getDescription());
        pr.setReturnDate(req.getReturnDate());

        pr.getItems().clear();
        entityManager.flush();

        long totalAmount = 0L;
        List<PurchaseReturnItem> items = new ArrayList<>();
        for (ProductSerial serial : serials) {
            long unitCost = getSerialUnitCost(serial);
            try {
                totalAmount = Math.addExact(totalAmount, unitCost);
            } catch (ArithmeticException ex) {
                throw new BadRequestException("Tổng tiền hoàn trả vượt giới hạn cho phép.");
            }

            serial.setStatus(ProductSerialStatus.PENDING_RETURN);
            PurchaseReturnItem item = PurchaseReturnItem.builder()
                    .purchaseReturn(pr)
                    .productSerial(serial)
                    .unitCost(unitCost)
                    .build();
            if (currentUserId != null) {
                item.setCreatedBy(currentUserId);
            }
            items.add(item);
        }

        pr.setTotalAmount(totalAmount);
        pr.getItems().addAll(items);

        PurchaseReturn saved = purchaseReturnRepository.saveAndFlush(pr);
        log.info("Đã cập nhật phiếu hoàn trả id: {}", id);
        return toEnrichedResponse(saved);
    }

    @Transactional
    public PurchaseReturnResponse updateStatus(Long id, PurchaseReturnStatusUpdateRequest req) {
        PurchaseReturn pr = lockReturn(id);
        PurchaseReturnStatus current = pr.getStatus();
        PurchaseReturnStatus target = req.getStatus();

        if (current == target) {
            return toEnrichedResponse(pr);
        }

        boolean isAdmin = SecurityUtils.hasRole("ADMIN");
        boolean isManager = SecurityUtils.hasRole("MANAGER");
        boolean isWarehouse = SecurityUtils.hasRole("STAFF_WAREHOUSE");
        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (current == PurchaseReturnStatus.CANCELLED) {
            throw conflict("Phiếu hoàn trả đã bị hủy, không thể thay đổi trạng thái.");
        }
        if (current == PurchaseReturnStatus.COMPLETED) {
            throw conflict("Phiếu hoàn trả đã hoàn tất (COMPLETED), không thể thay đổi trạng thái.");
        }
        if (current == PurchaseReturnStatus.RETURNED) {
            throw conflict("Phiếu hoàn trả đã trả về kho (RETURNED), không thể thay đổi trạng thái.");
        }
        if (current == PurchaseReturnStatus.DELIVERING
                && (target == PurchaseReturnStatus.COMPLETED || target == PurchaseReturnStatus.CANCELLED || target == PurchaseReturnStatus.RETURNED)) {
            if (deliveryService.existsForResource(DeliveryResourceType.PURCHASE_RETURN, id)) {
                throw conflict("Phiếu đang giao; hãy cập nhật trạng thái vận đơn để ghi nhận kết quả giao hàng.");
            }
        }

        boolean allowed = switch (current) {
            case PENDING -> target == PurchaseReturnStatus.DELIVERING || target == PurchaseReturnStatus.CANCELLED;
            case DELIVERING -> target == PurchaseReturnStatus.COMPLETED || target == PurchaseReturnStatus.CANCELLED || target == PurchaseReturnStatus.RETURNED;
            case COMPLETED, CANCELLED, RETURNED -> false;
        };

        if (!allowed) {
            throw conflict("Không thể chuyển phiếu hoàn trả từ trạng thái " + current + " sang " + target + ".");
        }

        switch (target) {
            case PENDING -> {
                if (pr.getItems().isEmpty()) {
                    throw new BadRequestException("Phiếu hoàn trả phải có ít nhất một serial sản phẩm khi chuyển sang PENDING.");
                }
                for (PurchaseReturnItem item : pr.getItems()) {
                    ProductSerial ps = item.getProductSerial();
                    if (ps != null) {
                        ps.setStatus(ProductSerialStatus.PENDING_RETURN);
                    }
                }
            }
            case DELIVERING -> {
                if (!isAdmin && !isManager && !isWarehouse) {
                    throw new AccessDeniedException("Không có quyền chuyển trạng thái giao hàng.");
                }
                pr.startDelivery();
            }
            case COMPLETED -> {
                if (!isAdmin && !isManager) {
                    throw new AccessDeniedException("Chỉ Quản lý (MANAGER) hoặc Quản trị viên (ADMIN) mới có quyền xác nhận NCC đã nhận hàng hoàn.");
                }
                for (PurchaseReturnItem item : pr.getItems()) {
                    ProductSerial ps = item.getProductSerial();
                    if (ps != null) {
                        ps.setStatus(ProductSerialStatus.RETURNED);
                        ps.setTerminated(true);
                    }
                }
            }
            case CANCELLED -> {
                Long createdBy = pr.getCreatedBy();
                if (!isAdmin && !isManager && (createdBy == null || !createdBy.equals(currentUserId))) {
                    throw new AccessDeniedException("Chỉ Quản lý hoặc người tạo mới có quyền hủy phiếu hoàn trả.");
                }
                for (PurchaseReturnItem item : pr.getItems()) {
                    ProductSerial ps = item.getProductSerial();
                    if (ps != null) {
                        ps.setStatus(ProductSerialStatus.DEFECTIVE);
                        ps.setTerminated(false);
                    }
                }
            }
            case RETURNED -> {
                Long createdBy = pr.getCreatedBy();
                if (!isAdmin && !isManager && (createdBy == null || !createdBy.equals(currentUserId))) {
                    throw new AccessDeniedException("Chỉ Quản lý hoặc người tạo mới có quyền chuyển phiếu sang trạng thái đã hoàn về kho.");
                }
                for (PurchaseReturnItem item : pr.getItems()) {
                    ProductSerial ps = item.getProductSerial();
                    if (ps != null) {
                        ps.setStatus(ProductSerialStatus.DEFECTIVE);
                        ps.setTerminated(false);
                    }
                }
            }
        }

        pr.setStatus(target);
        if (req.getNote() != null && !req.getNote().isBlank()) {
            String currentDesc = pr.getDescription() == null ? "" : pr.getDescription() + " | ";
            pr.setDescription(currentDesc + "Ghi chú chuyển trạng thái: " + req.getNote().trim());
        }

        PurchaseReturn saved = purchaseReturnRepository.saveAndFlush(pr);
        log.info("Phiếu hoàn trả id: {} đã chuyển từ trạng thái {} sang {}", id, current, target);
        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .module(SystemLogModule.PURCHASE_RETURN)
                    .action(SystemLogAction.UPDATE_STATUS)
                    .resourceType("PURCHASE_RETURN")
                    .resourceId(saved.getId())
                    .resourceCode(saved.getCode())
                    .description("Chuyển trạng thái phiếu hoàn trả NCC " + saved.getCode() + " từ " + current + " sang " + target)
                    .detailMap(Map.of("oldStatus", current.name(), "newStatus", target.name()))
                    .build());
        }
        return toEnrichedResponse(saved);
    }

    @Transactional
    public void deleteReturn(Long id) {
        PurchaseReturn pr = lockReturn(id);
        if (pr.getStatus() != PurchaseReturnStatus.PENDING) {
            throw conflict("Chỉ có thể xóa phiếu hoàn trả khi ở trạng thái chờ xử lý (PENDING).");
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (pr.getCreatedBy() != null && currentUserId != null
                && !currentUserId.equals(pr.getCreatedBy()) && !SecurityUtils.hasRole("ADMIN")) {
            throw new AccessDeniedException("Chỉ người tạo hoặc Quản trị viên mới có quyền xóa phiếu hoàn trả.");
        }
        // Hoàn nguyên trạng thái serial về DEFECTIVE trước khi xóa phiếu
        for (PurchaseReturnItem item : pr.getItems()) {
            ProductSerial ps = item.getProductSerial();
            if (ps != null && ps.getStatus() == ProductSerialStatus.PENDING_RETURN) {
                ps.setStatus(ProductSerialStatus.DEFECTIVE);
            }
        }
        String returnCode = pr.getCode();
        purchaseReturnRepository.delete(pr);
        log.info("Đã xóa phiếu hoàn trả id: {}", id);
        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .module(SystemLogModule.PURCHASE_RETURN)
                    .action(SystemLogAction.DELETE)
                    .resourceType("PURCHASE_RETURN")
                    .resourceId(id)
                    .resourceCode(returnCode)
                    .description("Xóa phiếu hoàn trả nhà cung cấp " + returnCode)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public List<AvailableSerialResponse> getAvailableSerialsForReturn(Long supplierId, Long productId, String keyword) {
        validateAndGetSupplier(supplierId);
        List<ProductSerial> serials = purchaseReturnRepository.findAvailableSerialsForReturn(
                supplierId,
                ELIGIBLE_SERIAL_STATUSES,
                List.of(PurchaseReturnStatus.CANCELLED, PurchaseReturnStatus.RETURNED),
                null,
                productId,
                keyword
        );

        return enrichAvailableSerials(serials);
    }

    @Transactional(readOnly = true)
    public AvailableSerialResponse validateSerialForReturn(Long supplierId, String serialNumber) {
        if (serialNumber == null || serialNumber.trim().isBlank()) {
            throw new BadRequestException("Mã serial không được để trống.");
        }
        String cleanSn = serialNumber.trim();
        validateAndGetSupplier(supplierId);

        List<ProductSerial> matched = purchaseReturnRepository.findActiveSerialsWithReceiptBySerialNumber(cleanSn);
        if (matched.isEmpty()) {
            throw new ResourceNotFoundException("Serial sản phẩm", "serialNumber", cleanSn);
        }

        ProductSerial ps = matched.get(0);
        if (ps.isTerminated()) {
            throw new BadRequestException("Serial '" + cleanSn + "' đã bị kết thúc/hủy, không thể xuất hoàn trả.");
        }

        ReceiptItem ri = ps.getReceiptItem();
        if (ri == null || ri.getGoodsReceipt() == null) {
            throw new BadRequestException("Serial '" + cleanSn + "' không có liên kết phiếu nhập kho hợp lệ.");
        }

        GoodsReceipt gr = ri.getGoodsReceipt();
        PurchaseOrder po = purchaseOrderRepository.findById(gr.getPurchaseOrderId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn nhập hàng gốc của serial '" + cleanSn + "'."));

        if (!po.getSupplierId().equals(supplierId)) {
            Supplier actualSup = supplierRepository.findById(po.getSupplierId()).orElse(null);
            String actualSupName = actualSup != null ? actualSup.getName() : "ID " + po.getSupplierId();
            throw new BadRequestException("Serial '" + cleanSn + "' thuộc Nhà cung cấp '" + actualSupName
                    + "', không khớp với Nhà cung cấp đang chọn.");
        }

        if (!ELIGIBLE_SERIAL_STATUSES.contains(ps.getStatus())) {
            throw new BadRequestException("Serial '" + cleanSn + "' đang có trạng thái " + ps.getStatus()
                    + ", không đủ điều kiện xuất hoàn trả.");
        }

        Set<Long> inUse = purchaseReturnItemRepository.findSerialIdsInActiveReturns(
                List.of(ps.getId()),
                List.of(PurchaseReturnStatus.CANCELLED, PurchaseReturnStatus.RETURNED),
                null
        );
        if (!inUse.isEmpty()) {
            throw new DuplicateResourceException("Serial '" + cleanSn + "' hiện đang nằm trong một phiếu hoàn trả khác đang xử lý.");
        }

        return enrichAvailableSerials(List.of(ps)).get(0);
    }

    private List<ProductSerial> resolveAndValidateSerials(
            List<String> serialNumbers,
            List<Long> productSerialIds,
            Long supplierId,
            Long excludeReturnId
    ) {
        Set<String> cleanSns = serialNumbers == null ? Collections.emptySet() :
                serialNumbers.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> cleanIds = productSerialIds == null ? Collections.emptySet() :
                productSerialIds.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        if (cleanSns.isEmpty() && cleanIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ProductSerial> serialMap = new LinkedHashMap<>();

        if (!cleanIds.isEmpty()) {
            List<ProductSerial> byIds = productSerialRepository.findAllById(cleanIds);
            for (ProductSerial ps : byIds) {
                serialMap.put(ps.getId(), ps);
            }
            if (byIds.size() < cleanIds.size()) {
                Set<Long> foundIds = byIds.stream().map(ProductSerial::getId).collect(Collectors.toSet());
                List<Long> missing = cleanIds.stream().filter(id -> !foundIds.contains(id)).toList();
                throw new ResourceNotFoundException("Serial sản phẩm", "ids", missing);
            }
        }

        if (!cleanSns.isEmpty()) {
            for (String sn : cleanSns) {
                List<ProductSerial> list = purchaseReturnRepository.findActiveSerialsWithReceiptBySerialNumber(sn);
                if (list.isEmpty()) {
                    throw new ResourceNotFoundException("Serial sản phẩm", "serialNumber", sn);
                }
                ProductSerial ps = list.get(0);
                serialMap.put(ps.getId(), ps);
            }
        }

        List<ProductSerial> serialList = new ArrayList<>(serialMap.values());
        List<Long> allSerialIds = serialList.stream().map(ProductSerial::getId).toList();

        Set<Long> alreadyInActive = purchaseReturnItemRepository.findSerialIdsInActiveReturns(
                allSerialIds,
                List.of(PurchaseReturnStatus.CANCELLED, PurchaseReturnStatus.RETURNED),
                excludeReturnId
        );

        Set<Long> poIds = serialList.stream()
                .map(ProductSerial::getReceiptItem)
                .filter(Objects::nonNull)
                .map(ReceiptItem::getGoodsReceipt)
                .filter(Objects::nonNull)
                .map(GoodsReceipt::getPurchaseOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, PurchaseOrder> poMap = poIds.isEmpty() ? Map.of()
                : purchaseOrderRepository.findAllById(poIds).stream()
                        .collect(Collectors.toMap(PurchaseOrder::getId, p -> p));

        Set<Long> mismatchSupIds = poMap.values().stream()
                .map(PurchaseOrder::getSupplierId)
                .filter(sid -> sid != null && !sid.equals(supplierId))
                .collect(Collectors.toSet());
        Map<Long, Supplier> supMap = mismatchSupIds.isEmpty() ? Map.of()
                : supplierRepository.findAllById(mismatchSupIds).stream()
                        .collect(Collectors.toMap(Supplier::getId, s -> s));

        for (ProductSerial ps : serialList) {
            if (ps.isTerminated()) {
                throw new BadRequestException("Serial '" + ps.getSerialNumber() + "' đã bị kết thúc, không thể hoàn trả.");
            }

            if (!ELIGIBLE_SERIAL_STATUSES.contains(ps.getStatus())) {
                throw new BadRequestException("Serial '" + ps.getSerialNumber() + "' có trạng thái " + ps.getStatus()
                        + " không đủ điều kiện xuất hoàn trả.");
            }

            if (alreadyInActive.contains(ps.getId())) {
                throw new DuplicateResourceException("Serial '" + ps.getSerialNumber() + "' hiện đang nằm trong một phiếu hoàn trả khác đang xử lý.");
            }

            ReceiptItem ri = ps.getReceiptItem();
            if (ri == null || ri.getGoodsReceipt() == null) {
                throw new BadRequestException("Serial '" + ps.getSerialNumber() + "' không có phiếu nhập gốc.");
            }

            GoodsReceipt gr = ri.getGoodsReceipt();
            PurchaseOrder po = poMap.get(gr.getPurchaseOrderId());
            if (po == null) {
                throw new BadRequestException("Không tìm thấy đơn nhập hàng gốc của serial '" + ps.getSerialNumber() + "'.");
            }

            if (!po.getSupplierId().equals(supplierId)) {
                Supplier actualSup = supMap.get(po.getSupplierId());
                String actualName = actualSup != null ? actualSup.getName() : "ID " + po.getSupplierId();
                throw new BadRequestException("Serial '" + ps.getSerialNumber() + "' thuộc Nhà cung cấp '" + actualName
                        + "', không khớp với Nhà cung cấp của phiếu hoàn.");
            }
        }

        return serialList;
    }

    private List<AvailableSerialResponse> enrichAvailableSerials(List<ProductSerial> serials) {
        if (serials == null || serials.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds = serials.stream().map(ProductSerial::getProductId).distinct().toList();
        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        return serials.stream().map(ps -> {
            Product prod = productMap.get(ps.getProductId());
            ReceiptItem ri = ps.getReceiptItem();
            GoodsReceipt gr = ri != null ? ri.getGoodsReceipt() : null;

            return AvailableSerialResponse.builder()
                    .productSerialId(ps.getId())
                    .serialNumber(ps.getSerialNumber())
                    .productId(ps.getProductId())
                    .productCode(prod != null ? prod.getCode() : null)
                    .productName(prod != null ? prod.getName() : null)
                    .status(ps.getStatus())
                    .unitCost(ri != null ? ri.getActualUnitCost() : 0L)
                    .receiptCode(gr != null ? gr.getCode() : null)
                    .receiptDate(gr != null ? gr.getReceivedAt() : null)
                    .build();
        }).toList();
    }

    private PurchaseReturnResponse toEnrichedResponse(PurchaseReturn pr) {
        PurchaseReturnResponse resp = purchaseReturnMapper.toResponse(pr);
        if (resp == null) return null;

        supplierRepository.findById(pr.getSupplierId()).ifPresent(sup -> {
            resp.setSupplierCode(sup.getCode());
            resp.setSupplierName(sup.getName());
        });

        if (pr.getItems() != null && !pr.getItems().isEmpty()) {
            List<Long> productIds = pr.getItems().stream()
                    .map(PurchaseReturnItem::getProductSerial)
                    .filter(Objects::nonNull)
                    .map(ProductSerial::getProductId)
                    .distinct()
                    .toList();

            Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));

            List<PurchaseReturnItemResponse> itemResponses = pr.getItems().stream().map(item -> {
                PurchaseReturnItemResponse itemResp = purchaseReturnMapper.toItemResponse(item);
                ProductSerial ps = item.getProductSerial();
                if (ps != null) {
                    Product prod = productMap.get(ps.getProductId());
                    if (prod != null) {
                        itemResp.setProductCode(prod.getCode());
                        itemResp.setProductName(prod.getName());
                    }
                    ReceiptItem ri = ps.getReceiptItem();
                    if (ri != null && ri.getGoodsReceipt() != null) {
                        itemResp.setReceiptCode(ri.getGoodsReceipt().getCode());
                        itemResp.setReceiptDate(ri.getGoodsReceipt().getReceivedAt());
                    }
                }
                return itemResp;
            }).toList();

            resp.setItems(itemResponses);
        }

        return resp;
    }

    private long getSerialUnitCost(ProductSerial serial) {
        if (serial.getReceiptItem() != null) {
            return serial.getReceiptItem().getActualUnitCost();
        }
        return 0L;
    }

    private Supplier validateAndGetSupplier(Long supplierId) {
        if (supplierId == null) {
            throw new BadRequestException("ID nhà cung cấp không được để trống.");
        }
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhà cung cấp", "id", supplierId));
    }

    private void validateCode(String code, Long excludeId) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Mã phiếu hoàn trả không được để trống.");
        }
        if (purchaseReturnRepository.countByCode(code, excludeId) > 0) {
            throw new DuplicateResourceException("Mã phiếu hoàn trả đã tồn tại: " + code);
        }
    }

    private PurchaseReturn lockReturn(Long id) {
        return purchaseReturnRepository.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu hoàn trả", "id", id));
    }

    private void requireEditable(PurchaseReturn pr) {
        if (pr.getStatus() != PurchaseReturnStatus.PENDING) {
            throw conflict("Chỉ có thể chỉnh sửa phiếu hoàn trả ở trạng thái chờ xử lý (PENDING). Trạng thái hiện tại: " + pr.getStatus());
        }
    }

    private void validateReadPermission(PurchaseReturn pr) {
        boolean isAdmin = SecurityUtils.hasRole("ADMIN");
        boolean isManager = SecurityUtils.hasRole("MANAGER");
        boolean isWarehouse = SecurityUtils.hasRole("STAFF_WAREHOUSE");
        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (isAdmin) return;

        if (isWarehouse) {
            if (pr.getCreatedBy() == null || currentUserId == null || !currentUserId.equals(pr.getCreatedBy())) {
                throw new AccessDeniedException("Bạn chỉ có thể xem phiếu hoàn trả do chính mình tạo.");
            }
            return;
        }

        if (isManager) {
            if (pr.getStatus() == PurchaseReturnStatus.RETURNED || pr.getStatus() == PurchaseReturnStatus.CANCELLED) {
                if (pr.getCreatedBy() == null || currentUserId == null || !currentUserId.equals(pr.getCreatedBy())) {
                    throw new AccessDeniedException("Quản lý chỉ xem được phiếu đã trả về kho hoặc phiếu đã hủy do chính mình tạo.");
                }
            }
        }
    }

    private ConflictException conflict(String message) {
        return new ConflictException(message);
    }
}
