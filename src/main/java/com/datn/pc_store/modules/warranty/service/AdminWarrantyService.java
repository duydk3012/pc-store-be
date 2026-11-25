package com.datn.pc_store.modules.warranty.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.brand.entity.Brand;
import com.datn.pc_store.modules.brand.repository.BrandRepository;
import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.order.entity.Order;
import com.datn.pc_store.modules.order.entity.OrderItem;
import com.datn.pc_store.modules.order.entity.OrderItemSerial;
import com.datn.pc_store.modules.order.repository.OrderItemRepository;
import com.datn.pc_store.modules.order.repository.OrderItemSerialRepository;
import com.datn.pc_store.modules.order.repository.OrderRepository;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.repository.UserRepository;
import com.datn.pc_store.modules.warranty.dto.*;
import com.datn.pc_store.modules.warranty.entity.*;
import com.datn.pc_store.modules.warranty.enums.WarrantyPartStatus;
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

    // ==================== 1. PRODUCT WARRANTIES ====================

    public PageResponse<AdminWarrantyResponse> getAllWarranties(
            String keyword, String serialNumber, String orderCode, String status, Pageable pageable) {
        List<Warranty> all = warranties.findAll();
        Instant now = Instant.now();
        BatchLookupCache cache = preloadForWarranties(all);

        List<AdminWarrantyResponse> mapped = all.stream()
                .map(w -> buildWarrantyResponse(w, now, cache))
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
        if (!w.isStatus()) {
            throw new BadRequestException("Không thể gia hạn gói bảo hành đã bị vô hiệu hóa.");
        }
        int days = req.additionalDays() != null ? req.additionalDays() : 0;
        if (days <= 0) {
            throw new BadRequestException("Số ngày gia hạn phải lớn hơn 0.");
        }
        w.setEndDate(w.getEndDate().plus(days, ChronoUnit.DAYS));
        if (req.note() != null && !req.note().isBlank()) {
            w.setNote(w.getNote() != null ? w.getNote() + "; " + req.note() : req.note());
        }
        Warranty saved = warranties.save(w);
        return buildWarrantyResponse(saved, Instant.now());
    }

    @Transactional
    public AdminWarrantyResponse voidWarranty(Long id) {
        Warranty w = warranties.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bảo hành sản phẩm", "id", id));
        w.setStatus(false);
        Warranty saved = warranties.save(w);
        return buildWarrantyResponse(saved, Instant.now());
    }

    // ==================== 2. WARRANTY REQUESTS & TECHNICAL ====================

    public PageResponse<AdminWarrantyRequestResponse> getAllRequests(
            String keyword, WarrantyRequestStatus status, Long technicianId, Pageable pageable) {
        List<WarrantyRequest> all = requests.findAll();
        BatchLookupCache cache = preloadForRequests(all);

        List<AdminWarrantyRequestResponse> mapped = all.stream()
                .map(r -> buildRequestResponse(r, cache))
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
        return buildRequestResponse(created);
    }

    @Transactional
    public AdminWarrantyRequestResponse updateTechnicalHandling(Long requestId, TechnicalHandlingUpdateRequest input) {
        WarrantyRequest req = requests.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu bảo hành", "id", requestId));

        if (req.getStatus() == WarrantyRequestStatus.COMPLETED || req.getStatus() == WarrantyRequestStatus.REJECTED) {
            throw new BadRequestException("Phiếu bảo hành đã kết thúc (" + req.getStatus() + "), không thể cập nhật thêm.");
        }

        if (input.technicianId() != null) {
            if (!users.existsById(input.technicianId())) {
                throw new ResourceNotFoundException("Kỹ thuật viên", "id", input.technicianId());
            }
            req.setTechnicianId(input.technicianId());
        }
        if (input.inspectionNotes() != null) {
            req.setInspectionNotes(input.inspectionNotes());
        }
        if (input.solution() != null) {
            req.setSolution(input.solution());
        }
        if (input.status() != null && input.status() != req.getStatus()) {
            WarrantyRequestStatus current = req.getStatus();
            WarrantyRequestStatus target = input.status();
            boolean allowed = switch (current) {
                case PENDING -> target == WarrantyRequestStatus.IN_PROGRESS || target == WarrantyRequestStatus.REJECTED;
                case IN_PROGRESS -> target == WarrantyRequestStatus.COMPLETED || target == WarrantyRequestStatus.REJECTED;
                case COMPLETED, REJECTED -> false;
            };
            if (!allowed) {
                throw new BadRequestException("Không thể chuyển phiếu bảo hành từ " + current + " sang " + target + ".");
            }
            req.setStatus(target);
            if (target == WarrantyRequestStatus.COMPLETED) {
                req.setCompletedAt(Instant.now());
                List<WarrantyPart> reqParts = parts.findByWarrantyRequestId(requestId);
                for (WarrantyPart p : reqParts) {
                    if (p.getStatus() == WarrantyPartStatus.RESERVED || p.getStatus() == WarrantyPartStatus.PENDING) {
                        p.setStatus(WarrantyPartStatus.INSTALLED);
                        parts.save(p);
                    }
                }
            } else if (target == WarrantyRequestStatus.REJECTED) {
                List<WarrantyPart> reqParts = parts.findByWarrantyRequestId(requestId);
                for (WarrantyPart p : reqParts) {
                    if (p.getStatus() == WarrantyPartStatus.RESERVED || p.getStatus() == WarrantyPartStatus.PENDING) {
                        releasePartSerialsToStock(p.getId());
                        p.setStatus(WarrantyPartStatus.CANCELLED);
                        parts.save(p);
                    }
                }
            }
        }

        WarrantyRequest saved = requests.save(req);
        return buildRequestResponse(saved);
    }

    @Transactional
    public AdminWarrantyRequestResponse addPartToRequest(Long requestId, Long productId, int quantity, long unitCost) {
        WarrantyRequest req = requests.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu bảo hành", "id", requestId));
        if (req.getStatus() == WarrantyRequestStatus.COMPLETED || req.getStatus() == WarrantyRequestStatus.REJECTED) {
            throw new BadRequestException("Không thể thêm linh kiện cho phiếu bảo hành đã kết thúc.");
        }
        if (productId == null || productId <= 0) {
            throw new BadRequestException("ID sản phẩm linh kiện không hợp lệ.");
        }
        if (!products.existsById(productId)) {
            throw new ResourceNotFoundException("Sản phẩm linh kiện", "id", productId);
        }
        if (quantity <= 0) {
            throw new BadRequestException("Số lượng linh kiện phải lớn hơn 0.");
        }
        if (unitCost < 0) {
            throw new BadRequestException("Đơn giá linh kiện không được âm.");
        }
        WarrantyPart part = new WarrantyPart();
        part.setWarrantyRequestId(requestId);
        part.setProductId(productId);
        part.setQuantity(quantity);
        part.setUnitCost(unitCost);
        part.setStatus(WarrantyPartStatus.PENDING);
        parts.save(part);
        return buildRequestResponse(req);
    }

    // ==================== 3. SPARE PARTS ====================

    public PageResponse<AdminSparePartResponse> getAllSpareParts(String keyword, WarrantyPartStatus status, Pageable pageable) {
        List<WarrantyPart> all = parts.findAllOrderByCreatedDateDesc();
        BatchLookupCache cache = preloadForSpareParts(all);

        List<AdminSparePartResponse> mapped = all.stream()
                .map(p -> buildSparePartResponse(p, cache))
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
        WarrantyPartStatus current = part.getStatus();
        if (current == status) {
            return buildSparePartResponse(part);
        }
        boolean allowed = switch (current) {
            case PENDING -> status == WarrantyPartStatus.RESERVED || status == WarrantyPartStatus.CANCELLED;
            case RESERVED -> status == WarrantyPartStatus.INSTALLED || status == WarrantyPartStatus.RETURNED || status == WarrantyPartStatus.CANCELLED;
            case INSTALLED -> status == WarrantyPartStatus.RETURNED;
            case RETURNED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new BadRequestException("Không thể chuyển trạng thái linh kiện từ " + current + " sang " + status + ".");
        }
        if (status == WarrantyPartStatus.RETURNED || status == WarrantyPartStatus.CANCELLED) {
            releasePartSerialsToStock(part.getId());
        }
        part.setStatus(status);
        WarrantyPart saved = parts.save(part);
        return buildSparePartResponse(saved);
    }

    private void releasePartSerialsToStock(Long partId) {
        List<WarrantyPartSerial> links = partSerials.findByWarrantyPartId(partId);
        if (links.isEmpty()) {
            return;
        }
        List<Long> serialIds = links.stream().map(WarrantyPartSerial::getProductSerialId).toList();
        List<ProductSerial> serials = productSerials.lockByIds(serialIds);
        for (ProductSerial ps : serials) {
            if (!ps.isTerminated() && ps.getStatus() == ProductSerialStatus.USING_BY_TECHNICAL) {
                ps.setStatus(ProductSerialStatus.IN_STOCK);
            }
        }
        productSerials.saveAll(serials);
    }

    // ==================== 4. TIMELINE HISTORY ====================

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
                            String pName = products.findById(p.getProductId()).map(Product::getName).orElse("Linh kiện #" + p.getProductId());
                            return pName + " (SL: " + p.getQuantity() + ")";
                        })
                        .collect(Collectors.joining("; "));
                timeline.add(new WarrantyHistoryResponse.TimelineEvent(
                        "evt-part-" + r.getId(),
                        r.getLastModifiedDate() != null ? r.getLastModifiedDate() : r.getCreatedDate(),
                        "Xuất linh kiện thay thế phục vụ sửa chữa",
                        partsDesc,
                        "Kho linh kiện kỹ thuật",
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
                        "Phiếu bảo hành " + r.getRequestCode() + " đã hoàn tất đóng ca.",
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

    private record BatchLookupCache(
            Map<Long, OrderItemSerial> oisMap,
            Map<Long, ProductSerial> psMap,
            Map<Long, OrderItem> oiMap,
            Map<Long, Order> orderMap,
            Map<Long, Product> productMap,
            Map<Long, Brand> brandMap,
            Map<Long, User> userMap,
            Map<Long, WarrantPolicy> policyMap,
            Map<Long, Long> reqCountMap,
            Map<Long, Warranty> warrantyMap,
            Map<Long, WarrantyRequest> requestMap,
            Map<Long, List<WarrantyPart>> partsByRequestMap,
            Map<Long, List<WarrantyPartSerial>> partSerialsMap
    ) {}

    private BatchLookupCache preloadForWarranties(List<Warranty> list) {
        if (list.isEmpty()) {
            return new BatchLookupCache(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }
        Set<Long> oisIds = list.stream().map(Warranty::getOrderItemSerialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, OrderItemSerial> oisMap = oisIds.isEmpty() ? Map.of()
                : orderItemSerials.findAllById(oisIds).stream().collect(Collectors.toMap(OrderItemSerial::getId, x -> x));

        Set<Long> psIds = oisMap.values().stream().map(OrderItemSerial::getProductSerialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ProductSerial> psMap = psIds.isEmpty() ? Map.of()
                : productSerials.findAllById(psIds).stream().collect(Collectors.toMap(ProductSerial::getId, x -> x));

        Set<Long> oiIds = oisMap.values().stream().map(OrderItemSerial::getOrderItemId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, OrderItem> oiMap = oiIds.isEmpty() ? Map.of()
                : orderItems.findAllById(oiIds).stream().collect(Collectors.toMap(OrderItem::getId, x -> x));

        Set<Long> orderIds = oiMap.values().stream().map(oi -> oi.getOrder() != null ? oi.getOrder().getId() : null)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Order> orderMap = orderIds.isEmpty() ? Map.of()
                : orders.findAllById(orderIds).stream().collect(Collectors.toMap(Order::getId, x -> x));

        Set<Long> productIds = oiMap.values().stream().map(OrderItem::getProductId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty() ? Map.of()
                : products.findAllById(productIds).stream().collect(Collectors.toMap(Product::getId, x -> x));

        Set<Long> brandIds = productMap.values().stream().map(Product::getBrandId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Brand> brandMap = brandIds.isEmpty() ? Map.of()
                : brands.findAllById(brandIds).stream().collect(Collectors.toMap(Brand::getId, x -> x));

        Set<Long> userIds = orderMap.values().stream().map(Order::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : users.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, x -> x));

        Set<Long> policyIds = list.stream().map(Warranty::getWarrantyPolicyId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, WarrantPolicy> policyMap = policyIds.isEmpty() ? Map.of()
                : policies.findAllById(policyIds).stream().collect(Collectors.toMap(WarrantPolicy::getId, x -> x));

        List<Long> wIds = list.stream().map(Warranty::getId).toList();
        Map<Long, Long> reqCountMap = requests.findByWarrantyIdIn(wIds).stream()
                .collect(Collectors.groupingBy(WarrantyRequest::getWarrantyId, Collectors.counting()));

        return new BatchLookupCache(oisMap, psMap, oiMap, orderMap, productMap, brandMap, userMap, policyMap, reqCountMap, Map.of(), Map.of(), Map.of(), Map.of());
    }

    private BatchLookupCache preloadForRequests(List<WarrantyRequest> list) {
        if (list.isEmpty()) {
            return new BatchLookupCache(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }
        Set<Long> wIds = list.stream().map(WarrantyRequest::getWarrantyId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<Warranty> warrantyList = wIds.isEmpty() ? List.of() : warranties.findAllById(wIds);
        Map<Long, Warranty> warrantyMap = warrantyList.stream().collect(Collectors.toMap(Warranty::getId, x -> x));
        BatchLookupCache base = preloadForWarranties(warrantyList);

        List<Long> reqIds = list.stream().map(WarrantyRequest::getId).toList();
        List<WarrantyPart> allParts = parts.findByWarrantyRequestIdIn(reqIds);
        Map<Long, List<WarrantyPart>> partsByReq = allParts.stream()
                .collect(Collectors.groupingBy(WarrantyPart::getWarrantyRequestId));

        List<Long> partIds = allParts.stream().map(WarrantyPart::getId).toList();
        List<WarrantyPartSerial> allPartSerials = partIds.isEmpty() ? List.of() : partSerials.findByWarrantyPartIdIn(partIds);
        Map<Long, List<WarrantyPartSerial>> partSerialsMap = allPartSerials.stream()
                .collect(Collectors.groupingBy(WarrantyPartSerial::getWarrantyPartId));

        Set<Long> extraProductIds = new HashSet<>(base.productMap().keySet());
        allParts.stream().map(WarrantyPart::getProductId).filter(Objects::nonNull).forEach(extraProductIds::add);
        Map<Long, Product> productMap = extraProductIds.isEmpty() ? Map.of()
                : products.findAllById(extraProductIds).stream().collect(Collectors.toMap(Product::getId, x -> x));

        Set<Long> extraPsIds = new HashSet<>(base.psMap().keySet());
        allPartSerials.stream().map(WarrantyPartSerial::getProductSerialId).filter(Objects::nonNull).forEach(extraPsIds::add);
        Map<Long, ProductSerial> psMap = extraPsIds.isEmpty() ? Map.of()
                : productSerials.findAllById(extraPsIds).stream().collect(Collectors.toMap(ProductSerial::getId, x -> x));

        Set<Long> extraUserIds = new HashSet<>(base.userMap().keySet());
        list.stream().map(WarrantyRequest::getCustomerId).filter(Objects::nonNull).forEach(extraUserIds::add);
        list.stream().map(WarrantyRequest::getTechnicianId).filter(Objects::nonNull).forEach(extraUserIds::add);
        Map<Long, User> userMap = extraUserIds.isEmpty() ? Map.of()
                : users.findAllById(extraUserIds).stream().collect(Collectors.toMap(User::getId, x -> x));

        return new BatchLookupCache(
                base.oisMap(), psMap, base.oiMap(), base.orderMap(), productMap, base.brandMap(),
                userMap, base.policyMap(), base.reqCountMap(), warrantyMap, Map.of(), partsByReq, partSerialsMap
        );
    }

    private BatchLookupCache preloadForSpareParts(List<WarrantyPart> list) {
        if (list.isEmpty()) {
            return new BatchLookupCache(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }
        Set<Long> reqIds = list.stream().map(WarrantyPart::getWarrantyRequestId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, WarrantyRequest> requestMap = reqIds.isEmpty() ? Map.of()
                : requests.findAllById(reqIds).stream().collect(Collectors.toMap(WarrantyRequest::getId, x -> x));

        Set<Long> techIds = requestMap.values().stream().map(WarrantyRequest::getTechnicianId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = techIds.isEmpty() ? Map.of()
                : users.findAllById(techIds).stream().collect(Collectors.toMap(User::getId, x -> x));

        Set<Long> prodIds = list.stream().map(WarrantyPart::getProductId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Product> productMap = prodIds.isEmpty() ? Map.of()
                : products.findAllById(prodIds).stream().collect(Collectors.toMap(Product::getId, x -> x));

        List<Long> partIds = list.stream().map(WarrantyPart::getId).toList();
        List<WarrantyPartSerial> links = partSerials.findByWarrantyPartIdIn(partIds);
        Map<Long, List<WarrantyPartSerial>> partSerialsMap = links.stream()
                .collect(Collectors.groupingBy(WarrantyPartSerial::getWarrantyPartId));

        Set<Long> psIds = links.stream().map(WarrantyPartSerial::getProductSerialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ProductSerial> psMap = psIds.isEmpty() ? Map.of()
                : productSerials.findAllById(psIds).stream().collect(Collectors.toMap(ProductSerial::getId, x -> x));

        return new BatchLookupCache(Map.of(), psMap, Map.of(), Map.of(), productMap, Map.of(), userMap, Map.of(), Map.of(), Map.of(), requestMap, Map.of(), partSerialsMap);
    }

    private AdminWarrantyResponse buildWarrantyResponse(Warranty w, Instant now) {
        return buildWarrantyResponse(w, now, null);
    }

    private AdminWarrantyResponse buildWarrantyResponse(Warranty w, Instant now, BatchLookupCache cache) {
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
            Optional<OrderItemSerial> oisOpt = cache != null
                    ? Optional.ofNullable(cache.oisMap().get(w.getOrderItemSerialId()))
                    : orderItemSerials.findById(w.getOrderItemSerialId());
            if (oisOpt.isPresent()) {
                OrderItemSerial ois = oisOpt.get();
                productSerialId = ois.getProductSerialId();
                if (productSerialId != null) {
                    Optional<ProductSerial> psOpt = cache != null
                            ? Optional.ofNullable(cache.psMap().get(productSerialId))
                            : productSerials.findById(productSerialId);
                    if (psOpt.isPresent()) {
                        serialNumber = psOpt.get().getSerialNumber();
                    }
                }
                if (ois.getOrderItemId() != null) {
                    Optional<OrderItem> oiOpt = cache != null
                            ? Optional.ofNullable(cache.oiMap().get(ois.getOrderItemId()))
                            : orderItems.findById(ois.getOrderItemId());
                    if (oiOpt.isPresent()) {
                        OrderItem oi = oiOpt.get();
                        productId = oi.getProductId();
                        if (productId != null) {
                            Optional<Product> pOpt = cache != null
                                    ? Optional.ofNullable(cache.productMap().get(productId))
                                    : products.findById(productId);
                            if (pOpt.isPresent()) {
                                Product p = pOpt.get();
                                productName = p.getName();
                                productCode = p.getCode();
                                if (p.getBrandId() != null) {
                                    brandName = (cache != null
                                            ? Optional.ofNullable(cache.brandMap().get(p.getBrandId()))
                                            : brands.findById(p.getBrandId()))
                                            .map(Brand::getName).orElse("");
                                }
                            }
                        }
                        if (oi.getOrder() != null) {
                            Order ord = cache != null
                                    ? cache.orderMap().get(oi.getOrder().getId())
                                    : oi.getOrder();
                            if (ord != null) {
                                orderId = ord.getId();
                                orderCode = ord.getCode();
                                customerId = ord.getUserId();
                                if (customerId != null) {
                                    Optional<User> uOpt = cache != null
                                            ? Optional.ofNullable(cache.userMap().get(customerId))
                                            : users.findById(customerId);
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
        }

        String policyName = "Chính sách tiêu chuẩn";
        int durationDays = 365;
        if (w.getWarrantyPolicyId() != null) {
            Optional<WarrantPolicy> wpOpt = cache != null
                    ? Optional.ofNullable(cache.policyMap().get(w.getWarrantyPolicyId()))
                    : policies.findById(w.getWarrantyPolicyId());
            if (wpOpt.isPresent()) {
                WarrantPolicy wp = wpOpt.get();
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

        long requestsCount = cache != null
                ? cache.reqCountMap().getOrDefault(w.getId(), 0L)
                : requests.countByWarrantyId(w.getId());

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
        return buildRequestResponse(r, null);
    }

    private AdminWarrantyRequestResponse buildRequestResponse(WarrantyRequest r, BatchLookupCache cache) {
        String serialNumber = "";
        Long productId = null;
        String productName = "Sản phẩm";
        String productCode = "";
        String orderCode = "";
        Long orderId = null;

        if (r.getWarrantyId() != null) {
            Optional<Warranty> wOpt = cache != null
                    ? Optional.ofNullable(cache.warrantyMap().get(r.getWarrantyId()))
                    : warranties.findById(r.getWarrantyId());
            if (wOpt.isPresent()) {
                AdminWarrantyResponse wRes = buildWarrantyResponse(wOpt.get(), Instant.now(), cache);
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
            Optional<User> uOpt = cache != null
                    ? Optional.ofNullable(cache.userMap().get(r.getCustomerId()))
                    : users.findById(r.getCustomerId());
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
            Optional<User> tOpt = cache != null
                    ? Optional.ofNullable(cache.userMap().get(r.getTechnicianId()))
                    : users.findById(r.getTechnicianId());
            if (tOpt.isPresent()) {
                User t = tOpt.get();
                techName = t.getFullName();
                techPhone = t.getPhoneNumber();
            }
        }

        List<WarrantyPart> rParts = cache != null
                ? cache.partsByRequestMap().getOrDefault(r.getId(), List.of())
                : parts.findByWarrantyRequestId(r.getId());
        List<AdminWarrantyRequestResponse.Part> mappedParts = rParts.stream().map(p -> {
            String pName = "Linh kiện thay thế";
            String pCode = "";
            if (p.getProductId() != null) {
                Optional<Product> prodOpt = cache != null
                        ? Optional.ofNullable(cache.productMap().get(p.getProductId()))
                        : products.findById(p.getProductId());
                if (prodOpt.isPresent()) {
                    pName = prodOpt.get().getName();
                    pCode = prodOpt.get().getCode();
                }
            }
            List<WarrantyPartSerial> links = cache != null
                    ? cache.partSerialsMap().getOrDefault(p.getId(), List.of())
                    : partSerials.findByWarrantyPartId(p.getId());
            List<Long> serialIds = links.stream().map(WarrantyPartSerial::getProductSerialId).collect(Collectors.toList());
            List<String> serialNums = new ArrayList<>();
            for (Long sId : serialIds) {
                Optional<ProductSerial> psOpt = cache != null
                        ? Optional.ofNullable(cache.psMap().get(sId))
                        : productSerials.findById(sId);
                psOpt.ifPresent(ps -> serialNums.add(ps.getSerialNumber()));
            }

            return new AdminWarrantyRequestResponse.Part(
                    p.getId(),
                    p.getWarrantyRequestId(),
                    p.getProductId(),
                    pName,
                    pCode,
                    p.getQuantity(),
                    p.getUnitCost(),
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
                r.getCompletedAt(),
                r.getCreatedDate(),
                r.getLastModifiedDate(),
                mappedParts
        );
    }

    private AdminSparePartResponse buildSparePartResponse(WarrantyPart p) {
        return buildSparePartResponse(p, null);
    }

    private AdminSparePartResponse buildSparePartResponse(WarrantyPart p, BatchLookupCache cache) {
        String requestCode = "";
        String techName = "Chưa phân công";
        Instant createdDate = p.getCreatedDate();
        if (p.getWarrantyRequestId() != null) {
            Optional<WarrantyRequest> reqOpt = cache != null
                    ? Optional.ofNullable(cache.requestMap().get(p.getWarrantyRequestId()))
                    : requests.findById(p.getWarrantyRequestId());
            if (reqOpt.isPresent()) {
                WarrantyRequest req = reqOpt.get();
                requestCode = req.getRequestCode();
                if (req.getTechnicianId() != null) {
                    techName = (cache != null
                            ? Optional.ofNullable(cache.userMap().get(req.getTechnicianId()))
                            : users.findById(req.getTechnicianId()))
                            .map(User::getFullName).orElse(techName);
                }
            }
        }

        String productName = "Linh kiện thay thế";
        String productCode = "";
        if (p.getProductId() != null) {
            Optional<Product> prodOpt = cache != null
                    ? Optional.ofNullable(cache.productMap().get(p.getProductId()))
                    : products.findById(p.getProductId());
            if (prodOpt.isPresent()) {
                productName = prodOpt.get().getName();
                productCode = prodOpt.get().getCode();
            }
        }

        List<WarrantyPartSerial> links = cache != null
                ? cache.partSerialsMap().getOrDefault(p.getId(), List.of())
                : partSerials.findByWarrantyPartId(p.getId());
        List<String> serialNums = new ArrayList<>();
        for (WarrantyPartSerial link : links) {
            Optional<ProductSerial> psOpt = cache != null
                    ? Optional.ofNullable(cache.psMap().get(link.getProductSerialId()))
                    : productSerials.findById(link.getProductSerialId());
            psOpt.ifPresent(ps -> serialNums.add(ps.getSerialNumber()));
        }

        long totalCost = p.getUnitCost() * p.getQuantity();

        return new AdminSparePartResponse(
                p.getId(),
                p.getWarrantyRequestId(),
                requestCode,
                p.getProductId(),
                productName,
                productCode,
                "Linh kiện sửa chữa",
                p.getQuantity(),
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
