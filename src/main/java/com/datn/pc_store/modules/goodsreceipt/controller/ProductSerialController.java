package com.datn.pc_store.modules.goodsreceipt.controller;

import com.datn.pc_store.modules.goodsreceipt.dto.ProductSerialResponse;
import com.datn.pc_store.modules.goodsreceipt.service.ProductSerialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-serials")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF_WAREHOUSE', 'STAFF_TECH')")
@Tag(name = "Product Serials", description = "Tra cứu serial sản phẩm. Yêu cầu Bearer token")
public class ProductSerialController {

    private final ProductSerialService productSerialService;

    @GetMapping("/{id}")
    @Operation(summary = "Xem thông tin serial theo ID", description = "Tra cứu thông tin serial sản phẩm theo ID")
    public ResponseEntity<ProductSerialResponse> getById(
            @Parameter(description = "ID serial") @PathVariable Long id
    ) {
        return ResponseEntity.ok(productSerialService.getById(id));
    }

    @GetMapping("/by-serial/{serialNumber}")
    @Operation(summary = "Xem thông tin serial theo số serial", description = "Tra cứu thông tin chi tiết một serial theo số serial")
    public ResponseEntity<ProductSerialResponse> getBySerialNumber(
            @Parameter(description = "Số serial") @PathVariable String serialNumber
    ) {
        return ResponseEntity.ok(productSerialService.getBySerialNumber(serialNumber));
    }

    @GetMapping("/by-product/{productId}")
    @Operation(summary = "Lấy danh sách serial còn hiệu lực theo sản phẩm", description = "Lọc các serial chưa bị kết thúc (is_terminated = false) của một sản phẩm")
    public ResponseEntity<List<ProductSerialResponse>> getActiveSerialsByProduct(
            @Parameter(description = "ID sản phẩm") @PathVariable Long productId
    ) {
        return ResponseEntity.ok(productSerialService.getActiveSerialsByProduct(productId));
    }
}
