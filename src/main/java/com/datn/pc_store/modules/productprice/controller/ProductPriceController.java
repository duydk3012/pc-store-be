package com.datn.pc_store.modules.productprice.controller;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.modules.productprice.dto.*;
import com.datn.pc_store.modules.productprice.service.ProductPriceService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/product-prices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Product Prices (Admin)", description = "Quản lý giá bán và lịch sử giá sản phẩm dành cho Admin")
public class ProductPriceController {
    private final ProductPriceService productPriceService;

    @GetMapping
    @Operation(summary = "Xem danh sách và lịch sử giá", description = "Phân trang, lọc theo sản phẩm và cờ isPresentPrice. Cờ được chọn áp dụng còn phải thỏa khoảng hiệu lực để là giá hiện tại. Mặc định sort=createdDate,desc. Trường sort hợp lệ: id, productId, price, effectiveFrom, effectiveTo, presentPrice, createdBy, createdDate. Sort sai trả về 400.")
    public ResponseEntity<PageResponse<ProductPriceResponse>> getPrices(
            @Parameter(description = "ID sản phẩm cần xem lịch sử giá") @RequestParam(required = false) Long productId,
            @Parameter(description = "Lọc cờ bản giá được chọn áp dụng (true) hoặc giá đã đóng (false)")
            @RequestParam(name = "isPresentPrice", required = false) Boolean presentPrice,
            @ParameterObject @PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(productPriceService.getPrices(productId, presentPrice, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết phiên bản giá", description = "Lấy phiên bản giá theo ID, bao gồm giá lịch sử. Trả về 404 nếu không tồn tại.")
    public ResponseEntity<ProductPriceResponse> getPriceById(@Parameter(description = "ID phiên bản giá") @PathVariable Long id) {
        return ResponseEntity.ok(productPriceService.getPriceById(id));
    }

    @GetMapping("/products/{productId}/current")
    @Operation(summary = "Xem giá hiện tại của sản phẩm", description = "Giá phải được chọn áp dụng và thỏa effectiveFrom ≤ hiện tại < effectiveTo (mốc null không giới hạn). Trả về 404 nếu sản phẩm đã xóa hoặc chưa có giá hiệu lực, 409 nếu dữ liệu có nhiều giá cùng hiệu lực.")
    public ResponseEntity<ProductPriceResponse> getCurrentPrice(@Parameter(description = "ID sản phẩm") @PathVariable Long productId) {
        return ResponseEntity.ok(productPriceService.getCurrentPrice(productId, false));
    }

    @PostMapping
    @Operation(summary = "Áp dụng giá bán mới", description = "Tạo phiên bản có hiệu lực ngay, đồng thời đóng và lưu lại các giá cũ của sản phẩm. Thời gian và cờ áp dụng do server quản lý. Không hỗ trợ đặt lịch giá tương lai hoặc ghi lùi thời gian. Trả về 201 khi thành công, 400 nếu giá không hợp lệ, 404 nếu sản phẩm không tồn tại.")
    public ResponseEntity<ProductPriceResponse> createPrice(@Valid @RequestBody ProductPriceCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productPriceService.createPrice(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Thay đổi giá bán hiện tại", description = "Thay phiên bản giá hiện tại bằng phiên bản mới có ID mới và trả về phiên bản mới. Giữ nguyên số tiền, người tạo và ngày tạo của bản giá cũ; chỉ đóng hiệu lực của bản cũ. Trả về 409 nếu ID đã là giá lịch sử hoặc bị thay thế bởi yêu cầu khác.")
    public ResponseEntity<ProductPriceResponse> updatePrice(@Parameter(description = "ID phiên bản giá hiện tại") @PathVariable Long id,
                                                           @Valid @RequestBody ProductPriceUpdateRequest req) {
        return ResponseEntity.ok(productPriceService.updatePrice(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Ngừng áp dụng giá hiện tại", description = "Đóng hiệu lực giá, giữ bản ghi lịch sử; sản phẩm sẽ chưa có giá hiện tại cho đến khi áp dụng giá mới. Không xóa giá lịch sử hoặc tự kích hoạt lại giá cũ. Trả về 204 khi thành công, 409 nếu ID không phải giá hiện tại.")
    public ResponseEntity<Void> deactivatePrice(@Parameter(description = "ID phiên bản giá hiện tại") @PathVariable Long id) {
        productPriceService.deactivatePrice(id);
        return ResponseEntity.noContent().build();
    }
}
