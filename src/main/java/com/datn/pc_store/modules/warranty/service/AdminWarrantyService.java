package com.datn.pc_store.modules.warranty.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.brand.repository.BrandRepository;
import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.order.entity.OrderItem;
import com.datn.pc_store.modules.order.entity.OrderItemSerial;
import com.datn.pc_store.modules.order.repository.OrderItemRepository;
import com.datn.pc_store.modules.order.repository.OrderItemSerialRepository;
import com.datn.pc_store.modules.order.repository.OrderRepository;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.systemlog.dto.SystemLogCreateCommand;
import com.datn.pc_store.modules.systemlog.enums.SystemLogAction;
import com.datn.pc_store.modules.systemlog.enums.SystemLogModule;
import com.datn.pc_store.modules.systemlog.event.SystemLogPublisher;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.repository.UserRepository;
import com.datn.pc_store.modules.warranty.dto.*;
import com.datn.pc_store.modules.warranty.entity.*;
import com.datn.pc_store.modules.warranty.enums.WarrantyPartStatus;
import com.datn.pc_store.modules.warranty.enums.WarrantyPartType;
import com.datn.pc_store.modules.warranty.enums.WarrantyRequestStatus;
import com.datn.pc_store.modules.warranty.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminWarrantyService {

    private final WarrantyRepository warranties;
    private final WarrantyPolicyRepository policies;
    private final WarrantyRequestRepository requests;
    private final WarrantyPartRepository parts;
    private final WarrantyPartSerialRepository partSerials;
    private final OrderItemSerialRepository orderItemSerials;
    private final OrderItemRepository orderItems;
    private final OrderRepository orders;
    private final ProductSerialRepository productSerials;
    private final ProductRepository products;
    private final BrandRepository brands;
    private final UserRepository users;
    private final WarrantyRequestService warrantyRequestService;
    private final SystemLogPublisher systemLogPublisher;

    // ==================== 1. PRODUCT WARRANTIES ====================

    public PageResponse<AdminWarrantyResponse> getAllWarranties(
            String keyword, String serialNumber, String orderCode, String status, Pageable pageable) {
        List<Warranty> all = warranties.findAll();
        Instant now = Instant.now();

        List<AdminWarrantyResponse> mapped = all.stream()
                .map(w -> buildWarrantyResponse(w, now))
                .filter(res -> {
                    if (serialNumber != null && !serialNumber.isBlank()) {
                        if (res.serialNumber() == null || !res.serialNumber().toLowerCase().contains(serialNumber.trim().toLowerCase())) {
                            return false;
                        }
                    }
                    if (orderCode != null && !orderCode.isBlank()) {
                        if (res.orderCode() == null || !res.orderCode().toLowerCase().contains(orderCode.trim().toLowerCase())) {
                            return false;
                        }
                    }
                    if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                        if (!status.equalsIgnoreCase(res.status())) {
                            return false;
                        }
                    }
                    if (keyword != null && !keyword.isBlank()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean match = (res.serialNumber() != null && res.serialNumber().toLowerCase().contains(kw))
                                || (res.productName() != null && res.productName().toLowerCase().contains(kw))
                                || (res.customerName() != null && res.customerName().toLowerCase().contains(kw))
                                || (res.customerPhone() != null && res.customerPhone().contains(kw))
                                || (res.orderCode() != null && res.orderCode().toLowerCase().contains(kw));
                        if (!match) return false;
                    }
                    return true;
                })
                .sorted((a, b) -> Long.compare(b.id(), a.id()))
                .collect(Collectors.toList());

        int totalElements = mapped.size();
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<AdminWarrantyResponse> content = mapped.subList(fromIndex, toIndex);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return PageResponse.<AdminWarrantyResponse>builder()
                .content(content)
                .pageNumber(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .empty(content.isEmpty())
                .build();
    }

    public AdminWarrantyResponse getWarrantyById(Long id) {
        Warranty w = warranties.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bảo hành sản phẩm", "id", id));
        return buildWarrantyResponse(w, Instant.now());
    }

    public AdminWarrantyResponse getWarrantyBySerial(String serialNumber) {
        List<Warranty> list = warranties.findBySerialNumber(serialNumber.trim());
        if (list.isEmpty()) {
            throw new ResourceNotFoundException("Bảo hành cho serial", "serialNumber", serialNumber);
        }
        return buildWarrantyResponse(list.get(0), Instant.now());
    }

    @Transactional
    public AdminWarrantyResponse extendWarranty(Long id, WarrantyExtendRequest req) {
        Warranty w = warranties.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bảo hành sản phẩm", "id", id));
        int days = req.additionalDays() != null ? req.additionalDays() : 0;
        w.setEndDate(w.getEndDate().plus(days, ChronoUnit.DAYS));
        if (req.note() != null && !req.note().isBlank()) {
            w.setNote(w.getNote() != null ? w.getNote() + "; " + req.note() : req.note());
        }
        if (w.getEndDate().isAfter(Instant.now())) {
            w.setStatus(true);
        }
        Warranty saved = warranties.save(w);
        AdminWarrantyResponse response = buildWarrantyResponse(saved, Instant.now());
        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .module(SystemLogModule.WARRANTY)
                    .action(SystemLogAction.UPDATE)
                    .resourceType("WARRANTY")
                    .resourceId(saved.getId())
                    .resourceCode(response.serialNumber())
                    .description("Gia hạn bảo hành thêm " + days + " ngày cho thiết bị " + response.serialNumber())
                    .detailMap(Map.of("additionalDays", days, "newEndDate", saved.getEndDate().toString()))
                    .build());
        }
        return response;
    }

    @Transactional
    public AdminWarrantyResponse voidWarranty(Long id) {
        Warranty w = warranties.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bảo hành sản phẩm", "id", id));
        w.setStatus(false);
        Warranty saved = warranties.save(w);
        AdminWarrantyResponse response = buildWarrantyResponse(saved, Instant.now());
        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .module(SystemLogModule.WARRANTY)
                    .action(SystemLogAction.UPDATE_STATUS)
                    .resourceType("WARRANTY")
                    .resourceId(saved.getId())
                    .resourceCode(response.serialNumber())
                    .description("Hủy hiệu lực bảo hành cho thiết bị " + response.serialNumber())
                    .build());
        }
        return response;
    }

    // ==================== 2. WARRANTY REQUESTS & TECHNICAL ====================

    public PageResponse<AdminWarrantyRequestResponse> getAllRequests(
            String keyword, WarrantyRequestStatus status, Long technicianId, Pageable pageable) {
        List<WarrantyRequest> all = requests.findAll();

        List<AdminWarrantyRequestResponse> mapped = all.stream()
                .map(this::buildRequestResponse)
                .filter(res -> {
                    if (status != null && res.status() != status) {
                        return false;
                    }
                    if (technicianId != null && !Objects.equals(res.technicianId(), technicianId)) {
                        return false;
                    }
                    if (keyword != null && !keyword.isBlank()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean match = (res.requestCode() != null && res.requestCode().toLowerCase().contains(kw))
                                || (res.serialNumber() != null && res.serialNumber().toLowerCase().contains(kw))
                                || (res.customerName() != null && res.customerName().toLowerCase().contains(kw))
                                || (res.customerPhone() != null && res.customerPhone().contains(kw))
                                || (res.productName() != null && res.productName().toLowerCase().contains(kw));
                        if (!match) return false;
                    }
                    return true;
                })
                .sorted((a, b) -> Long.compare(b.id(), a.id()))
                .collect(Collectors.toList());

        int totalElements = mapped.size();
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<AdminWarrantyRequestResponse> content = mapped.subList(fromIndex, toIndex);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return PageResponse.<AdminWarrantyRequestResponse>builder()
                .content(content)
                .pageNumber(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .empty(content.isEmpty())
                .build();
    }

    public AdminWarrantyRequestResponse getRequestById(Long id) {
        WarrantyRequest req = requests.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu yêu cầu bảo hành", "id", id));
        return buildRequestResponse(req);
    }

    @Transactional
    public AdminWarrantyRequestResponse createRequest(WarrantyRequestCreateRequest input) {
        WarrantyRequestResponse res = warrantyRequestService.create(input);
        WarrantyRequest created = requests.findById(res.id()).orElseThrow();
        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .module(SystemLogModule.WARRANTY)
                    .action(SystemLogAction.CREATE)
                    .resourceType("WARRANTY_REQUEST")
                    .resourceId(created.getId())
                    .resourceCode(created.getRequestCode())
                    .description("Tiếp nhận phiếu yêu cầu bảo hành " + created.getRequestCode())
                    .build());
        }
        return buildRequestResponse(created);
    }

    @Transactional
    public AdminWarrantyRequestResponse updateTechnicalHandling(Long requestId, TechnicalHandlingUpdateRequest input) {
        WarrantyRequest req = requests.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu bảo hành", "id", requestId));

        if (input.technicianId() != null) {
            req.setTechnicianId(input.technicianId());
        }
        if (input.inspectionNotes() != null) {
            req.setInspectionNotes(input.inspectionNotes());
        }
        if (input.solution() != null) {
            req.setSolution(input.solution());
        }
        if (input.status() != null) {
            req.setStatus(input.status());
            if (input.status() == WarrantyRequestStatus.COMPLETED) {
                req.setCompletedAt(Instant.now());
                List<WarrantyPart> reqParts = parts.findByWarrantyRequestId(requestId);

                // Kiểm tra các linh kiện loại WARRANTY hoặc UPGRADE phải được gán đủ serial
                for (WarrantyPart p : reqParts) {
                    if ((p.getType() == WarrantyPartType.WARRANTY || p.getType() == WarrantyPartType.UPGRADE)
                            && p.getStatus() == WarrantyPartStatus.PENDING) {
                        throw new BadRequestException("Linh kiện ID " + p.getId() + " (" + p.getType()
                                + ") chưa được thủ kho gán đủ serial để hoàn tất đóng ca.");
                    }
                }

                for (WarrantyPart p : reqParts) {
                    if (p.getStatus() == WarrantyPartStatus.RESERVED || p.getStatus() == WarrantyPartStatus.PENDING) {
                        p.setStatus(WarrantyPartStatus.INSTALLED);
                        parts.save(p);
                    }
                }

                // Cập nhật trạng thái các serial liên quan sang SOLD (theo quyết định Q1)
                List<Long> partIds = reqParts.stream().map(WarrantyPart::getId).collect(Collectors.toList());
                if (!partIds.isEmpty()) {
                    List<WarrantyPartSerial> links = partSerials.findByWarrantyPartIdIn(partIds);
                    for (WarrantyPartSerial link : links) {
                        productSerials.findById(link.getProductSerialId()).ifPresent(ps -> {
                            ps.setStatus(ProductSerialStatus.SOLD);
                            productSerials.save(ps);
                        });
                    }
                }

                // Cập nhật tổng chi phí khách phải trả
                long totalCost = reqParts.stream().mapToLong(p -> p.getUnitCost() * p.getQuantity()).sum();
                req.setTotalCost(totalCost);

            } else if (input.status() == WarrantyRequestStatus.REJECTED) {
                // Rollback toàn bộ serial đã giữ về IN_STOCK, hủy liên kết và chuyển part sang CANCELLED (theo quyết định Q3)
                List<WarrantyPart> reqParts = parts.findByWarrantyRequestId(requestId);
                List<Long> partIds = reqParts.stream().map(WarrantyPart::getId).collect(Collectors.toList());
                if (!partIds.isEmpty()) {
                    List<WarrantyPartSerial> links = partSerials.findByWarrantyPartIdIn(partIds);
                    for (WarrantyPartSerial link : links) {
                        productSerials.findById(link.getProductSerialId()).ifPresent(ps -> {
                            if (ps.getStatus() == ProductSerialStatus.USING_BY_TECHNICAL) {
                                ps.setStatus(ProductSerialStatus.IN_STOCK);
                                productSerials.save(ps);
                            }
                        });
                    }
                    partSerials.deleteByWarrantyPartIdIn(partIds);
                }

                for (WarrantyPart p : reqParts) {
                    p.setStatus(WarrantyPartStatus.CANCELLED);
                    parts.save(p);
                }
            }
        }

        WarrantyRequest saved = requests.save(req);
        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .module(SystemLogModule.WARRANTY)
                    .action(input.status() != null ? SystemLogAction.UPDATE_STATUS : SystemLogAction.UPDATE)
                    .resourceType("WARRANTY_REQUEST")
                    .resourceId(saved.getId())
                    .resourceCode(saved.getRequestCode())
                    .description("Cập nhật xử lý kỹ thuật phiếu bảo hành " + saved.getRequestCode())
                    .detailMap(Map.of("status", saved.getStatus().name()))
                    .build());
        }
        return buildRequestResponse(saved);
    }

    @Transactional
    public AdminWarrantyRequestResponse addPartToRequest(
            Long requestId,
            WarrantyPartType type,
            Long productId,
            Integer quantity,
            Integer warrantyCoveragePercent,
            Long unitCost
    ) {
        WarrantyRequest req = requests.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu bảo hành", "id", requestId));

        if (req.getStatus() == WarrantyRequestStatus.COMPLETED || req.getStatus() == WarrantyRequestStatus.REJECTED) {
            throw new BadRequestException("Không thể thêm linh kiện vào phiếu bảo hành đã hoàn tất hoặc bị từ chối.");
        }

        WarrantyPartType partType = type != null ? type : WarrantyPartType.WARRANTY;
        Product product = null;
        if (partType == WarrantyPartType.WARRANTY || partType == WarrantyPartType.UPGRADE) {
            if (productId == null) {
                throw new BadRequestException("Linh kiện loại " + partType + " bắt buộc phải chọn sản phẩm cụ thể.");
            }
            product = products.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm linh kiện", "id", productId));
        } else if (productId != null) {
            product = products.findById(productId).orElse(null);
        }

        long originalPrice = product != null && product.getPrice() != null ? product.getPrice() : 0L;
        int coverage = 0;
        long finalUnitCost;

        if (partType == WarrantyPartType.WARRANTY) {
            coverage = warrantyCoveragePercent != null ? warrantyCoveragePercent : 100;
            if (coverage < 0 || coverage > 100) {
                throw new BadRequestException("Phần trăm bảo hành phải nằm trong khoảng 0 - 100%.");
            }
            long autoCost = Math.round(originalPrice * (1.0 - (coverage / 100.0)));
            finalUnitCost = unitCost != null ? unitCost : autoCost;
        } else if (partType == WarrantyPartType.UPGRADE) {
            finalUnitCost = unitCost != null ? unitCost : originalPrice;
        } else { // REPAIR
            finalUnitCost = unitCost != null ? unitCost : 0L;
        }

        int qty = quantity != null && quantity > 0 ? quantity : 1;

        WarrantyPart part = new WarrantyPart();
        part.setWarrantyRequestId(requestId);
        part.setType(partType);
        part.setProductId(productId);
        part.setQuantity(qty);
        part.setOriginalPrice(originalPrice);
        part.setWarrantyCoveragePercent(coverage);
        part.setUnitCost(finalUnitCost);
        part.setStatus(WarrantyPartStatus.PENDING);

        parts.save(part);

        // Cập nhật lại totalCost của phiếu
        List<WarrantyPart> allParts = parts.findByWarrantyRequestId(requestId);
        long grandTotal = allParts.stream().mapToLong(p -> p.getUnitCost() * p.getQuantity()).sum();
        req.setTotalCost(grandTotal);
        requests.save(req);

        return buildRequestResponse(req);
    }

    // ==================== 3. GÁN SERIAL CHO LINH KIỆN TỪ KHO ====================

    @Transactional
    public AdminWarrantyRequestResponse assignSerialsToPart(Long requestId, Long partId, WarrantyPartAssignSerialsRequest req) {
        WarrantyRequest warrantyReq = requests.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu bảo hành", "id", requestId));

        if (warrantyReq.getStatus() == WarrantyRequestStatus.COMPLETED || warrantyReq.getStatus() == WarrantyRequestStatus.REJECTED) {
            throw new BadRequestException("Phiếu bảo hành đã hoàn tất hoặc bị từ chối, không thể gán serial.");
        }

        WarrantyPart part = parts.findById(partId)
                .orElseThrow(() -> new ResourceNotFoundException("Linh kiện bảo hành", "id", partId));

        if (!Objects.equals(part.getWarrantyRequestId(), requestId)) {
            throw new BadRequestException("Linh kiện không thuộc phiếu bảo hành này.");
        }

        if (part.getStatus() != WarrantyPartStatus.PENDING && part.getStatus() != WarrantyPartStatus.RESERVED) {
            throw new BadRequestException("Chỉ có thể gán serial cho linh kiện ở trạng thái PENDING hoặc RESERVED.");
        }

        if (part.getProductId() == null) {
            throw new BadRequestException("Linh kiện này không gắn sản phẩm cụ thể nên không cần gán serial kho.");
        }

        Set<Long> serialIds = new HashSet<>();
        if (req.productSerialIds() != null && !req.productSerialIds().isEmpty()) {
            serialIds.addAll(req.productSerialIds());
        }
        if (req.serialNumbers() != null && !req.serialNumbers().isEmpty()) {
            for (String sn : req.serialNumbers()) {
                if (sn != null && !sn.isBlank()) {
                    productSerials.findFirstBySerialNumberAndIsTerminatedFalse(sn.trim())
                            .ifPresent(ps -> serialIds.add(ps.getId()));
                }
            }
        }

        if (serialIds.size() != part.getQuantity()) {
            throw new BadRequestException("Số lượng serial (" + serialIds.size() + ") phải khớp với số lượng linh kiện (" + part.getQuantity() + ").");
        }

        // Hoàn trả serial cũ nếu trước đó đã gán (để gán bộ serial mới)
        List<WarrantyPartSerial> oldLinks = partSerials.findByWarrantyPartId(partId);
        for (WarrantyPartSerial link : oldLinks) {
            productSerials.findById(link.getProductSerialId()).ifPresent(ps -> {
                if (ps.getStatus() == ProductSerialStatus.USING_BY_TECHNICAL) {
                    ps.setStatus(ProductSerialStatus.IN_STOCK);
                    productSerials.save(ps);
                }
            });
        }
        partSerials.deleteByWarrantyPartId(partId);

        // Khóa bi quan và kiểm tra các serial mới
        List<ProductSerial> locked = productSerials.lockByIds(serialIds);
        if (locked.size() != serialIds.size()) {
            throw new BadRequestException("Một số serial không tìm thấy trong hệ thống.");
        }

        for (ProductSerial ps : locked) {
            if (ps.isTerminated() || ps.getStatus() != ProductSerialStatus.IN_STOCK
                    || !Objects.equals(ps.getProductId(), part.getProductId())) {
                throw new BadRequestException("Serial không hợp lệ hoặc không còn trong kho: " + ps.getSerialNumber());
            }
            ps.setStatus(ProductSerialStatus.USING_BY_TECHNICAL);
            productSerials.save(ps);

            var link = new WarrantyPartSerial();
            link.setWarrantyPartId(part.getId());
            link.setProductSerialId(ps.getId());
            partSerials.save(link);
        }

        part.setStatus(WarrantyPartStatus.RESERVED);
        parts.save(part);

        if (systemLogPublisher != null) {
            systemLogPublisher.publish(SystemLogCreateCommand.builder()
                    .module(SystemLogModule.WARRANTY)
                    .action(SystemLogAction.ASSIGN_SERIAL)
                    .resourceType("WARRANTY_REQUEST")
                    .resourceId(warrantyReq.getId())
                    .resourceCode(warrantyReq.getRequestCode())
                    .description("Gán serial linh kiện thay thế cho phiếu bảo hành " + warrantyReq.getRequestCode())
                    .detailMap(Map.of("partId", part.getId(), "serialCount", locked.size()))
                    .build());
        }

        return buildRequestResponse(warrantyReq);
    }

    @Transactional
    public AdminWarrantyRequestResponse removeSerialFromPart(Long requestId, Long partId, Long productSerialId) {
        WarrantyRequest warrantyReq = requests.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu bảo hành", "id", requestId));
        WarrantyPart part = parts.findById(partId)
                .orElseThrow(() -> new ResourceNotFoundException("Linh kiện bảo hành", "id", partId));

        var linkOpt = partSerials.findByWarrantyPartIdAndProductSerialId(partId, productSerialId);
        if (linkOpt.isPresent()) {
            partSerials.delete(linkOpt.get());
            productSerials.findById(productSerialId).ifPresent(ps -> {
                if (ps.getStatus() == ProductSerialStatus.USING_BY_TECHNICAL) {
                    ps.setStatus(ProductSerialStatus.IN_STOCK);
                    productSerials.save(ps);
                }
            });
        }

        List<WarrantyPartSerial> remaining = partSerials.findByWarrantyPartId(partId);
        if (remaining.isEmpty()) {
            part.setStatus(WarrantyPartStatus.PENDING);
            parts.save(part);
        }

        return buildRequestResponse(warrantyReq);
    }

    // ==================== 4. SPARE PARTS ====================

    public PageResponse<AdminSparePartResponse> getAllSpareParts(String keyword, WarrantyPartStatus status, Pageable pageable) {
        List<WarrantyPart> all = parts.findAllOrderByCreatedDateDesc();

        List<AdminSparePartResponse> mapped = all.stream()
                .map(this::buildSparePartResponse)
                .filter(res -> {
                    if (status != null && res.status() != status) {
                        return false;
                    }
                    if (keyword != null && !keyword.isBlank()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean match = (res.requestCode() != null && res.requestCode().toLowerCase().contains(kw))
                                || (res.productName() != null && res.productName().toLowerCase().contains(kw))
                                || (res.productCode() != null && res.productCode().toLowerCase().contains(kw))
                                || (res.assignedTechnician() != null && res.assignedTechnician().toLowerCase().contains(kw));
                        if (!match) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        int totalElements = mapped.size();
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<AdminSparePartResponse> content = mapped.subList(fromIndex, toIndex);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return PageResponse.<AdminSparePartResponse>builder()
                .content(content)
                .pageNumber(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .empty(content.isEmpty())
                .build();
    }

    @Transactional
    public AdminSparePartResponse updateSparePartStatus(Long partId, WarrantyPartStatus status) {
        WarrantyPart part = parts.findById(partId)
                .orElseThrow(() -> new ResourceNotFoundException("Linh kiện bảo hành", "id", partId));
        part.setStatus(status);
        WarrantyPart saved = parts.save(part);
        return buildSparePartResponse(saved);
    }

    // ==================== 5. TIMELINE HISTORY ====================

    public WarrantyHistoryResponse getWarrantyHistoryBySerial(String serialNumber) {
        String cleanSerial = serialNumber.trim();
        List<Warranty> ws = warranties.findBySerialNumber(cleanSerial);
        if (ws.isEmpty()) {
            throw new ResourceNotFoundException("Lịch sử bảo hành", "serialNumber", cleanSerial);
        }
        Warranty w = ws.get(0);
        AdminWarrantyResponse wRes = buildWarrantyResponse(w, Instant.now());
        List<WarrantyRequest> reqs = requests.findBySerialNumber(cleanSerial);

        List<WarrantyHistoryResponse.TimelineEvent> timeline = new ArrayList<>();

        // Event 1: Sale & activation
        timeline.add(new WarrantyHistoryResponse.TimelineEvent(
                "evt-sale-" + w.getId(),
                w.getStartDate(),
                "Bàn giao thiết bị & Kích hoạt bảo hành",
                "Đơn hàng " + (wRes.orderCode() != null ? wRes.orderCode() : "") + " xuất kho thành công. Chính sách: \"" +
                        (wRes.policyName() != null ? wRes.policyName() : "Tiêu chuẩn") + "\" (" + wRes.durationDays() + " ngày).",
                "Hệ thống kho & bán hàng",
                "SALE",
                "blue"
        ));

        // Request events
        for (WarrantyRequest r : reqs) {
            timeline.add(new WarrantyHistoryResponse.TimelineEvent(
                    "evt-intake-" + r.getId(),
                    r.getCreatedDate(),
                    "Tiếp nhận yêu cầu bảo hành (" + r.getRequestCode() + ")",
                    "Hiện tượng lỗi: " + (r.getIssueDescription() != null ? r.getIssueDescription() : "Không ghi chú"),
                    wRes.customerName() != null ? wRes.customerName() : "Khách hàng",
                    "INTAKE",
                    "amber"
            ));

            if (r.getInspectionNotes() != null && !r.getInspectionNotes().isBlank()) {
                String techName = "Kỹ thuật viên";
                if (r.getTechnicianId() != null) {
                    techName = users.findById(r.getTechnicianId()).map(User::getFullName).orElse(techName);
                }
                timeline.add(new WarrantyHistoryResponse.TimelineEvent(
                        "evt-insp-" + r.getId(),
                        r.getLastModifiedDate() != null ? r.getLastModifiedDate() : r.getCreatedDate(),
                        "Giám định kỹ thuật & Chẩn đoán lỗi",
                        r.getInspectionNotes(),
                        techName,
                        "INSPECTION",
                        "purple"
                ));
            }

            List<WarrantyPart> rParts = parts.findByWarrantyRequestId(r.getId());
            if (!rParts.isEmpty()) {
                String partsDesc = rParts.stream()
                        .map(p -> {
                            String pName = p.getProductId() != null ?
                                    products.findById(p.getProductId()).map(Product::getName).orElse("Linh kiện #" + p.getProductId())
                                    : "Dịch vụ sửa chữa";
                            return "[" + getPartTypeName(p.getType()) + "] " + pName + " (SL: " + p.getQuantity() + ")";
                        })
                        .collect(Collectors.joining("; "));
                timeline.add(new WarrantyHistoryResponse.TimelineEvent(
                        "evt-part-" + r.getId(),
                        r.getLastModifiedDate() != null ? r.getLastModifiedDate() : r.getCreatedDate(),
                        "Linh kiện & chi phí kỹ thuật",
                        partsDesc,
                        "Kho linh kiện & kỹ thuật",
                        "PARTS",
                        "cyan"
                ));
            }

            if (r.getSolution() != null && !r.getSolution().isBlank()) {
                String techName = "Kỹ thuật viên";
                if (r.getTechnicianId() != null) {
                    techName = users.findById(r.getTechnicianId()).map(User::getFullName).orElse(techName);
                }
                timeline.add(new WarrantyHistoryResponse.TimelineEvent(
                        "evt-sol-" + r.getId(),
                        r.getCompletedAt() != null ? r.getCompletedAt() : r.getLastModifiedDate(),
                        "Xử lý thành công & Kiểm thử hoàn tất",
                        r.getSolution(),
                        techName,
                        "REPAIR",
                        "emerald"
                ));
            }

            if (r.getStatus() == WarrantyRequestStatus.COMPLETED) {
                timeline.add(new WarrantyHistoryResponse.TimelineEvent(
                        "evt-comp-" + r.getId(),
                        r.getCompletedAt() != null ? r.getCompletedAt() : Instant.now(),
                        "Bàn giao thiết bị cho khách hàng",
                        "Phiếu bảo hành " + r.getRequestCode() + " đã hoàn tất. Tổng thanh toán: " + String.format("%,d đ", r.getTotalCost()),
                        "Nhân viên dịch vụ khách hàng",
                        "COMPLETE",
                        "green"
                ));
            }
        }

        return new WarrantyHistoryResponse(
                wRes.serialNumber(),
                wRes.productName(),
                wRes.productCode(),
                wRes.customerName(),
                wRes.customerPhone(),
                wRes.orderCode(),
                w.getStartDate(),
                w.getStartDate(),
                w.getEndDate(),
                wRes.policyName(),
                wRes.status(),
                reqs.size(),
                timeline
        );
    }

    // ==================== HELPER BUILDERS ====================

    private String getPartTypeName(WarrantyPartType type) {
        if (type == null) return "Bảo hành";
        return switch (type) {
            case WARRANTY -> "Bảo hành";
            case REPAIR -> "Sửa chữa tính phí";
            case UPGRADE -> "Nâng cấp mua mới";
        };
    }

    private AdminWarrantyResponse buildWarrantyResponse(Warranty w, Instant now) {
        String serialNumber = "";
        Long productSerialId = null;
        Long productId = null;
        String productName = "Sản phẩm bảo hành";
        String productCode = "";
        String brandName = "";
        String categoryName = "";
        Long orderId = null;
        String orderCode = "";
        Long customerId = null;
        String customerName = "Khách hàng";
        String customerPhone = "";
        String customerEmail = "";

        if (w.getOrderItemSerialId() != null) {
            Optional<OrderItemSerial> oisOpt = orderItemSerials.findById(w.getOrderItemSerialId());
            if (oisOpt.isPresent()) {
                OrderItemSerial ois = oisOpt.get();
                productSerialId = ois.getProductSerialId();
                if (productSerialId != null) {
                    Optional<ProductSerial> psOpt = productSerials.findById(productSerialId);
                    if (psOpt.isPresent()) {
                        serialNumber = psOpt.get().getSerialNumber();
                    }
                }
                if (ois.getOrderItemId() != null) {
                    Optional<OrderItem> oiOpt = orderItems.findById(ois.getOrderItemId());
                    if (oiOpt.isPresent()) {
                        OrderItem oi = oiOpt.get();
                        productId = oi.getProductId();
                        if (productId != null) {
                            Optional<Product> pOpt = products.findById(productId);
                            if (pOpt.isPresent()) {
                                Product p = pOpt.get();
                                productName = p.getName();
                                productCode = p.getCode();
                                if (p.getBrandId() != null) {
                                    brandName = brands.findById(p.getBrandId()).map(b -> b.getName()).orElse("");
                                }
                            }
                        }
                        if (oi.getOrder() != null) {
                            orderId = oi.getOrder().getId();
                            orderCode = oi.getOrder().getCode();
                            customerId = oi.getOrder().getUserId();
                            if (customerId != null) {
                                Optional<User> uOpt = users.findById(customerId);
                                if (uOpt.isPresent()) {
                                    User u = uOpt.get();
                                    customerName = u.getFullName();
                                    customerPhone = u.getPhoneNumber() != null ? u.getPhoneNumber() : "";
                                    customerEmail = u.getEmail() != null ? u.getEmail() : "";
                                }
                            }
                        }
                    }
                }
            }
        }

        String policyName = "Chính sách tiêu chuẩn";
        int durationDays = 365;
        if (w.getWarrantyPolicyId() != null) {
            Optional<WarrantyPolicy> wpOpt = policies.findById(w.getWarrantyPolicyId());
            if (wpOpt.isPresent()) {
                WarrantyPolicy wp = wpOpt.get();
                policyName = wp.getName();
                durationDays = wp.getDurationDays();
            }
        }

        long remainingDays = 0;
        if (w.getEndDate() != null && w.getEndDate().isAfter(now)) {
            remainingDays = Duration.between(now, w.getEndDate()).toDays();
        }

        String statusStr;
        if (!w.isStatus() || now.isAfter(w.getEndDate())) {
            statusStr = "EXPIRED";
        } else if (w.getEndDate().isBefore(now.plus(30, ChronoUnit.DAYS))) {
            statusStr = "EXPIRING_SOON";
        } else {
            statusStr = "ACTIVE";
        }

        long requestsCount = requests.countByWarrantyId(w.getId());

        return new AdminWarrantyResponse(
                w.getId(),
                w.getOrderItemSerialId(),
                orderId,
                orderCode,
                productSerialId != null ? productSerialId : 0L,
                serialNumber,
                productId,
                productName,
                productCode,
                brandName,
                categoryName,
                customerId,
                customerName,
                customerPhone,
                customerEmail,
                w.getWarrantyPolicyId(),
                policyName,
                durationDays,
                w.getStartDate(),
                w.getEndDate(),
                w.isStatus() && now.isBefore(w.getEndDate()),
                statusStr,
                remainingDays,
                w.getNote(),
                requestsCount
        );
    }

    private AdminWarrantyRequestResponse buildRequestResponse(WarrantyRequest r) {
        String serialNumber = "";
        Long productId = null;
        String productName = "Sản phẩm";
        String productCode = "";
        String orderCode = "";
        Long orderId = null;

        if (r.getWarrantyId() != null) {
            Optional<Warranty> wOpt = warranties.findById(r.getWarrantyId());
            if (wOpt.isPresent()) {
                AdminWarrantyResponse wRes = buildWarrantyResponse(wOpt.get(), Instant.now());
                serialNumber = wRes.serialNumber();
                productId = wRes.productId();
                productName = wRes.productName();
                productCode = wRes.productCode();
                orderCode = wRes.orderCode();
                orderId = wRes.orderId();
            }
        }

        String customerName = "Khách hàng";
        String customerPhone = "";
        String customerEmail = "";
        if (r.getCustomerId() != null) {
            Optional<User> uOpt = users.findById(r.getCustomerId());
            if (uOpt.isPresent()) {
                User u = uOpt.get();
                customerName = u.getFullName();
                customerPhone = u.getPhoneNumber() != null ? u.getPhoneNumber() : "";
                customerEmail = u.getEmail() != null ? u.getEmail() : "";
            }
        }

        String techName = null;
        String techPhone = null;
        if (r.getTechnicianId() != null) {
            Optional<User> tOpt = users.findById(r.getTechnicianId());
            if (tOpt.isPresent()) {
                User t = tOpt.get();
                techName = t.getFullName();
                techPhone = t.getPhoneNumber();
            }
        }

        List<WarrantyPart> rParts = parts.findByWarrantyRequestId(r.getId());
        List<AdminWarrantyRequestResponse.Part> mappedParts = rParts.stream().map(p -> {
            String pName = "Dịch vụ sửa chữa";
            String pCode = "";
            if (p.getProductId() != null) {
                Optional<Product> prodOpt = products.findById(p.getProductId());
                if (prodOpt.isPresent()) {
                    pName = prodOpt.get().getName();
                    pCode = prodOpt.get().getCode();
                }
            }
            List<WarrantyPartSerial> links = partSerials.findByWarrantyPartId(p.getId());
            List<Long> serialIds = links.stream().map(WarrantyPartSerial::getProductSerialId).collect(Collectors.toList());
            List<String> serialNums = new ArrayList<>();
            for (Long sId : serialIds) {
                productSerials.findById(sId).ifPresent(ps -> serialNums.add(ps.getSerialNumber()));
            }

            return new AdminWarrantyRequestResponse.Part(
                    p.getId(),
                    p.getWarrantyRequestId(),
                    p.getType(),
                    getPartTypeName(p.getType()),
                    p.getProductId(),
                    pName,
                    pCode,
                    p.getQuantity(),
                    p.getOriginalPrice(),
                    p.getWarrantyCoveragePercent(),
                    p.getUnitCost(),
                    p.getUnitCost() * p.getQuantity(),
                    p.getStatus(),
                    serialIds,
                    serialNums
            );
        }).collect(Collectors.toList());

        return new AdminWarrantyRequestResponse(
                r.getId(),
                r.getRequestCode(),
                r.getWarrantyId(),
                serialNumber,
                productId,
                productName,
                productCode,
                orderCode,
                orderId,
                r.getCustomerId(),
                customerName,
                customerPhone,
                customerEmail,
                r.getTechnicianId(),
                techName,
                techPhone,
                r.getIssueDescription(),
                "Thân máy và linh kiện kèm theo",
                "Ngoại quan theo biên bản tiếp nhận",
                r.getInspectionNotes(),
                "MANUFACTURER",
                r.getSolution(),
                r.getStatus(),
                r.getTotalCost(),
                r.getCompletedAt(),
                r.getCreatedDate(),
                r.getLastModifiedDate(),
                mappedParts
        );
    }

    private AdminSparePartResponse buildSparePartResponse(WarrantyPart p) {
        String requestCode = "";
        String techName = "Chưa phân công";
        Instant createdDate = p.getCreatedDate();
        if (p.getWarrantyRequestId() != null) {
            Optional<WarrantyRequest> reqOpt = requests.findById(p.getWarrantyRequestId());
            if (reqOpt.isPresent()) {
                WarrantyRequest req = reqOpt.get();
                requestCode = req.getRequestCode();
                if (req.getTechnicianId() != null) {
                    techName = users.findById(req.getTechnicianId()).map(User::getFullName).orElse(techName);
                }
            }
        }

        String productName = "Dịch vụ sửa chữa";
        String productCode = "";
        String productCategory = "Dịch vụ";
        if (p.getProductId() != null) {
            Optional<Product> prodOpt = products.findById(p.getProductId());
            if (prodOpt.isPresent()) {
                productName = prodOpt.get().getName();
                productCode = prodOpt.get().getCode();
                productCategory = "Linh kiện thay thế";
            }
        }

        List<WarrantyPartSerial> links = partSerials.findByWarrantyPartId(p.getId());
        List<String> serialNums = new ArrayList<>();
        for (WarrantyPartSerial link : links) {
            productSerials.findById(link.getProductSerialId()).ifPresent(ps -> serialNums.add(ps.getSerialNumber()));
        }

        long totalCost = p.getUnitCost() * p.getQuantity();

        return new AdminSparePartResponse(
                p.getId(),
                p.getWarrantyRequestId(),
                requestCode,
                p.getType(),
                getPartTypeName(p.getType()),
                p.getProductId(),
                productName,
                productCode,
                productCategory,
                p.getQuantity(),
                p.getOriginalPrice(),
                p.getWarrantyCoveragePercent(),
                p.getUnitCost(),
                totalCost,
                p.getStatus(),
                serialNums,
                techName,
                createdDate,
                p.getLastModifiedDate()
        );
    }
}
