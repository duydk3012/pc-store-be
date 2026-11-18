package com.datn.pc_store.modules.order.service;

import com.datn.pc_store.modules.order.entity.Invoice;
import com.datn.pc_store.modules.order.entity.Order;
import com.datn.pc_store.modules.order.enums.InvoiceStatus;
import com.datn.pc_store.modules.order.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public Invoice createInvoiceForCompletedOrder(Order order) {
        if (order == null || order.getId() == null) {
            return null;
        }

        // Kiểm tra xem đơn hàng đã có hóa đơn hợp lệ chưa
        Optional<Invoice> existingOpt = invoiceRepository.findFirstByOrderIdOrderByCreatedDateDesc(order.getId());
        if (existingOpt.isPresent() && existingOpt.get().getStatus() != InvoiceStatus.CANCELED) {
            log.info("Đơn hàng id={} đã có hóa đơn code={}", order.getId(), existingOpt.get().getCode());
            return existingOpt.get();
        }

        String code = generateInvoiceCode();
        Instant invoiceTime = order.getLastModifiedDate() != null ? order.getLastModifiedDate() : Instant.now();

        Invoice invoice = Invoice.builder()
                .code(code)
                .orderId(order.getId())
                .invoiceDate(invoiceTime)
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .taxAmount(0L)
                .amount(order.getFinalAmount())
                .status(InvoiceStatus.PAID)
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Đã phát hành hóa đơn tài chính tự động id={}, code={} cho đơn hàng id={}",
                saved.getId(), saved.getCode(), order.getId());
        return saved;
    }

    @Transactional
    public void cancelInvoiceForOrder(Long orderId) {
        if (orderId == null) return;
        invoiceRepository.findFirstByOrderIdOrderByCreatedDateDesc(orderId).ifPresent(inv -> {
            if (inv.getStatus() != InvoiceStatus.CANCELED) {
                inv.setStatus(InvoiceStatus.CANCELED);
                invoiceRepository.save(inv);
                log.info("Đã hủy hóa đơn code={} của đơn hàng id={}", inv.getCode(), orderId);
            }
        });
    }

    private String generateInvoiceCode() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String code = "HD-" + datePrefix + "-" + randomSuffix;
        while (invoiceRepository.existsByCode(code)) {
            randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            code = "HD-" + datePrefix + "-" + randomSuffix;
        }
        return code;
    }
}
