package com.datn.pc_store.modules.warranty.service;

import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.brand.entity.Brand;
import com.datn.pc_store.modules.brand.repository.BrandRepository;
import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.order.entity.OrderItem;
import com.datn.pc_store.modules.order.entity.OrderItemSerial;
import com.datn.pc_store.modules.order.repository.OrderItemRepository;
import com.datn.pc_store.modules.order.repository.OrderItemSerialRepository;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.warranty.dto.PublicWarrantyResponse;
import com.datn.pc_store.modules.warranty.entity.WarrantyPolicy;
import com.datn.pc_store.modules.warranty.entity.Warranty;
import com.datn.pc_store.modules.warranty.repository.WarrantyPolicyRepository;
import com.datn.pc_store.modules.warranty.repository.WarrantyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicWarrantyService {

    private final WarrantyRepository warranties;
    private final WarrantyPolicyRepository policies;
    private final OrderItemSerialRepository orderItemSerials;
    private final OrderItemRepository orderItems;
    private final ProductSerialRepository productSerials;
    private final ProductRepository products;
    private final BrandRepository brands;

    /**
     * Tra cứu thông tin bảo hành công khai theo số serial chính xác.
     * Chỉ trả về thông tin sản phẩm và thời hạn bảo hành, không lộ thông tin khách hàng hay đơn hàng.
     */
    public PublicWarrantyResponse lookupByExactSerial(String serialNumber) {
        if (serialNumber == null || serialNumber.isBlank()) {
            throw new BadRequestException("Số serial không được để trống");
        }
        String trimmed = serialNumber.trim();
        List<Warranty> matched = warranties.findBySerialNumber(trimmed);
        if (matched.isEmpty()) {
            throw new ResourceNotFoundException("Bảo hành cho serial", "serialNumber", trimmed);
        }

        Instant now = Instant.now();
        Warranty warranty = matched.stream()
                .filter(w -> isActive(w, now))
                .findFirst()
                .orElse(matched.get(0));

        return toPublicResponse(warranty, trimmed, now);
    }

    private PublicWarrantyResponse toPublicResponse(Warranty w, String fallbackSerial, Instant now) {
        String serialNumber = fallbackSerial;
        Long productId = null;
        String productName = "Sản phẩm bảo hành";
        String productCode = "";
        String brandName = "";

        if (w.getOrderItemSerialId() != null) {
            OrderItemSerial ois = orderItemSerials.findById(w.getOrderItemSerialId()).orElse(null);
            if (ois != null) {
                if (ois.getProductSerialId() != null) {
                    serialNumber = productSerials.findById(ois.getProductSerialId())
                            .map(ProductSerial::getSerialNumber)
                            .orElse(serialNumber);
                }
                if (ois.getOrderItemId() != null) {
                    OrderItem oi = orderItems.findById(ois.getOrderItemId()).orElse(null);
                    if (oi != null && oi.getProductId() != null) {
                        productId = oi.getProductId();
                        Product p = products.findById(productId).orElse(null);
                        if (p != null) {
                            productName = p.getName();
                            productCode = p.getCode();
                            if (p.getBrandId() != null) {
                                brandName = brands.findById(p.getBrandId())
                                        .map(Brand::getName)
                                        .orElse("");
                            }
                        }
                    }
                }
            }
        }

        String policyName = "Chính sách tiêu chuẩn";
        int durationDays = 365;
        if (w.getWarrantyPolicyId() != null) {
            WarrantyPolicy wp = policies.findById(w.getWarrantyPolicyId()).orElse(null);
            if (wp != null) {
                policyName = wp.getName();
                durationDays = wp.getDurationDays();
            }
        }

        boolean active = isActive(w, now);
        long remainingDays = active ? Duration.between(now, w.getEndDate()).toDays() : 0L;
        String status = resolveStatus(w, active, now);

        return new PublicWarrantyResponse(
                serialNumber,
                productId,
                productName,
                productCode,
                brandName,
                policyName,
                durationDays,
                w.getStartDate(),
                w.getEndDate(),
                active,
                status,
                remainingDays
        );
    }

    private boolean isActive(Warranty w, Instant now) {
        return w.isStatus() && w.getEndDate() != null && now.isBefore(w.getEndDate());
    }

    private String resolveStatus(Warranty w, boolean active, Instant now) {
        if (!active) {
            return "EXPIRED";
        }
        if (w.getEndDate().isBefore(now.plus(30, ChronoUnit.DAYS))) {
            return "EXPIRING_SOON";
        }
        return "ACTIVE";
    }
}
