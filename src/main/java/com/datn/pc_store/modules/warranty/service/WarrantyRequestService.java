package com.datn.pc_store.modules.warranty.service;

import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.order.repository.OrderItemSerialRepository;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.user.repository.UserRepository;
import com.datn.pc_store.modules.warranty.dto.*;
import com.datn.pc_store.modules.warranty.entity.*;
import com.datn.pc_store.modules.warranty.enums.*;
import com.datn.pc_store.modules.warranty.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarrantyRequestService {
    private final WarrantyRepository warranties;
    private final WarrantyRequestRepository requests;
    private final OrderItemSerialRepository soldSerials;
    private final ProductSerialRepository inventory;
    private final ProductRepository products;
    private final UserRepository users;
    private final EntityManager entityManager;

    @Transactional
    public WarrantyRequestResponse create(WarrantyRequestCreateRequest input) {
        // Khóa bi quan serial đang bảo hành để tránh tạo trùng phiếu cùng lúc
        var soldSerial = soldSerials.lockForWarranty(input.warrantyId())
                .orElseThrow(() -> new ResourceNotFoundException("Bảo hành của serial", "id", input.warrantyId()));
        var warranty = warranties.findById(input.warrantyId()).orElseThrow();
        Instant now = Instant.now();
        if (!warranty.isStatus() || now.isBefore(warranty.getStartDate()) || now.isAfter(warranty.getEndDate())) {
            throw new BadRequestException("Bảo hành không còn hiệu lực.");
        }
        if (requests.existsForSerial(soldSerial.getId(), List.of(WarrantyRequestStatus.PENDING, WarrantyRequestStatus.IN_PROGRESS))) {
            throw new BadRequestException("Thiết bị này đã có phiếu bảo hành đang trong quá trình xử lý.");
        }
        if (!users.existsById(input.customerId())) {
            throw new ResourceNotFoundException("Khách hàng", "id", input.customerId());
        }

        WarrantyRequest request = new WarrantyRequest();
        request.setRequestCode("WR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        request.setWarrantyId(warranty.getId());
        request.setCustomerId(input.customerId());
        request.setIssueDescription(input.issueDescription().trim());
        request.setStatus(WarrantyRequestStatus.PENDING);
        request.setTotalCost(0L);
        requests.save(request);

        List<WarrantyRequestCreateRequest.Part> inputParts = input.parts() == null ? List.of() : input.parts();

        // Thu thập các serial IDs nếu có truyền sẵn từ lúc tạo (hỗ trợ cả serialNumbers và productSerialIds)
        Set<Long> preSelectedSerialIds = new HashSet<>();
        for (var partInput : inputParts) {
            List<Long> resolvedIds = resolveSerialIds(partInput);
            if (resolvedIds.isEmpty()) continue;
            if (resolvedIds.size() != partInput.quantity()) {
                throw new BadRequestException("Số serial gán cho linh kiện phải đúng bằng số lượng (" + partInput.quantity() + ").");
            }
            for (Long sId : resolvedIds) {
                if (sId.equals(soldSerial.getProductSerialId()) || !preSelectedSerialIds.add(sId)) {
                    throw new BadRequestException("Serial thay thế bị trùng với máy lỗi hoặc linh kiện khác trong phiếu: ID " + sId);
                }
            }
        }

        Map<Long, ProductSerial> lockedSerials = preSelectedSerialIds.isEmpty() ? Map.of() : inventory.lockByIds(preSelectedSerialIds)
                .stream().collect(Collectors.toMap(ProductSerial::getId, Function.identity()));

        long grandTotalCost = 0L;
        List<WarrantyRequestResponse.Part> resultParts = new ArrayList<>();

        for (var partInput : inputParts) {
            List<Long> resolvedIds = resolveSerialIds(partInput);
            WarrantyRequestResponse.Part savedPart = savePart(request.getId(), partInput, resolvedIds, lockedSerials);
            grandTotalCost += savedPart.subtotal();
            resultParts.add(savedPart);
        }

        request.setTotalCost(grandTotalCost);
        requests.save(request);

        return new WarrantyRequestResponse(
                request.getId(),
                request.getRequestCode(),
                request.getWarrantyId(),
                request.getCustomerId(),
                request.getIssueDescription(),
                request.getStatus(),
                request.getTotalCost(),
                resultParts
        );
    }

    private List<Long> resolveSerialIds(WarrantyRequestCreateRequest.Part input) {
        List<Long> ids = new ArrayList<>();
        if (input.productSerialIds() != null && !input.productSerialIds().isEmpty()) {
            ids.addAll(input.productSerialIds());
        }
        if (input.serialNumbers() != null && !input.serialNumbers().isEmpty()) {
            for (String sn : input.serialNumbers()) {
                if (sn != null && !sn.isBlank()) {
                    inventory.findFirstBySerialNumberAndIsTerminatedFalse(sn.trim())
                            .ifPresent(ps -> ids.add(ps.getId()));
                }
            }
        }
        return ids;
    }

    private WarrantyRequestResponse.Part savePart(
            Long requestId,
            WarrantyRequestCreateRequest.Part input,
            List<Long> serialIds,
            Map<Long, ProductSerial> lockedSerials
    ) {
        WarrantyPartType type = input.type() != null ? input.type() : WarrantyPartType.WARRANTY;
        Long productId = input.productId();
        Product product = null;

        if (type == WarrantyPartType.WARRANTY || type == WarrantyPartType.UPGRADE) {
            if (productId == null) {
                throw new BadRequestException("Linh kiện loại " + type + " bắt buộc phải chọn sản phẩm cụ thể.");
            }
            product = products.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm linh kiện", "id", productId));
        } else if (productId != null) {
            product = products.findById(productId).orElse(null);
        }

        long originalPrice = 0L;
        int coveragePercent = 0;
        long unitCost;

        if (type == WarrantyPartType.WARRANTY) {
            originalPrice = product != null && product.getPrice() != null ? product.getPrice() : 0L;
            coveragePercent = input.warrantyCoveragePercent() != null ? input.warrantyCoveragePercent() : 100;
            if (coveragePercent < 0 || coveragePercent > 100) {
                throw new BadRequestException("Phần trăm bảo hành phải nằm trong khoảng 0 - 100%.");
            }
            // Tự động tính đơn giá: originalPrice * (1 - coveragePercent / 100)
            long autoCost = Math.round(originalPrice * (1.0 - (coveragePercent / 100.0)));
            // Cho phép override nếu người dùng nhập unitCost
            unitCost = input.unitCost() != null ? input.unitCost() : autoCost;
        } else if (type == WarrantyPartType.UPGRADE) {
            originalPrice = product != null && product.getPrice() != null ? product.getPrice() : 0L;
            coveragePercent = 0;
            // Mua mới tính 100% giá
            unitCost = input.unitCost() != null ? input.unitCost() : originalPrice;
        } else { // REPAIR
            originalPrice = product != null && product.getPrice() != null ? product.getPrice() : 0L;
            coveragePercent = 0;
            unitCost = input.unitCost() != null ? input.unitCost() : 0L;
        }

        int quantity = input.quantity() != null && input.quantity() > 0 ? input.quantity() : 1;
        long subtotal = unitCost * quantity;

        WarrantyPart part = new WarrantyPart();
        part.setWarrantyRequestId(requestId);
        part.setType(type);
        part.setProductId(productId);
        part.setQuantity(quantity);
        part.setOriginalPrice(originalPrice);
        part.setWarrantyCoveragePercent(coveragePercent);
        part.setUnitCost(unitCost);
        part.setStatus(serialIds.isEmpty() ? WarrantyPartStatus.PENDING : WarrantyPartStatus.RESERVED);

        entityManager.persist(part);

        // Gán serial nếu có
        for (Long sId : serialIds) {
            ProductSerial serial = lockedSerials.get(sId);
            if (serial == null || serial.isTerminated() || serial.getStatus() != ProductSerialStatus.IN_STOCK
                    || !Objects.equals(serial.getProductId(), part.getProductId())) {
                throw new BadRequestException("Serial không hợp lệ hoặc không còn trong kho: " + sId);
            }
            serial.setStatus(ProductSerialStatus.USING_BY_TECHNICAL);
            var link = new WarrantyPartSerial();
            link.setWarrantyPartId(part.getId());
            link.setProductSerialId(sId);
            entityManager.persist(link);
        }

        return new WarrantyRequestResponse.Part(
                part.getId(),
                part.getProductId(),
                part.getType(),
                part.getQuantity(),
                part.getOriginalPrice(),
                part.getWarrantyCoveragePercent(),
                part.getUnitCost(),
                subtotal,
                part.getStatus(),
                serialIds
        );
    }
}
