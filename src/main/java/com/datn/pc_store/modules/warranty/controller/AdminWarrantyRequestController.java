package com.datn.pc_store.modules.warranty.controller;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.modules.warranty.dto.AdminWarrantyRequestResponse;
import com.datn.pc_store.modules.warranty.dto.TechnicalHandlingUpdateRequest;
import com.datn.pc_store.modules.warranty.dto.WarrantyRequestCreateRequest;
import com.datn.pc_store.modules.warranty.enums.WarrantyRequestStatus;
import com.datn.pc_store.modules.warranty.service.AdminWarrantyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/warranty-requests")
@RequiredArgsConstructor
@Tag(name = "Warranty Requests (Admin)", description = "Quản lý yêu cầu bảo hành và xử lý kỹ thuật")
public class AdminWarrantyRequestController {

    private final AdminWarrantyService service;

    @GetMapping
    @Operation(summary = "Danh sách yêu cầu tiếp nhận bảo hành")
    public ResponseEntity<PageResponse<AdminWarrantyRequestResponse>> getAllRequests(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) WarrantyRequestStatus status,
            @RequestParam(required = false) Long technicianId,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.getAllRequests(keyword, status, technicianId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết phiếu yêu cầu bảo hành")
    public ResponseEntity<AdminWarrantyRequestResponse> getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getRequestById(id));
    }

    @PostMapping
    @Operation(summary = "Tiếp nhận máy lỗi tạo phiếu bảo hành mới")
    public ResponseEntity<AdminWarrantyRequestResponse> createRequest(@Valid @RequestBody WarrantyRequestCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createRequest(req));
    }

    @PutMapping("/{id}/technical")
    @Operation(summary = "Cập nhật chẩn đoán và xử lý kỹ thuật cho phiếu bảo hành")
    public ResponseEntity<AdminWarrantyRequestResponse> updateTechnicalHandling(
            @PathVariable Long id,
            @RequestBody TechnicalHandlingUpdateRequest req) {
        return ResponseEntity.ok(service.updateTechnicalHandling(id, req));
    }

    public record AddPartInput(
            @jakarta.validation.constraints.NotNull(message = "ID sản phẩm linh kiện không được để trống")
            @jakarta.validation.constraints.Positive(message = "ID sản phẩm linh kiện phải lớn hơn 0")
            Long productId,
            @jakarta.validation.constraints.NotNull(message = "Số lượng không được để trống")
            @jakarta.validation.constraints.Positive(message = "Số lượng phải lớn hơn 0")
            Integer quantity,
            @jakarta.validation.constraints.PositiveOrZero(message = "Đơn giá không được âm")
            Long unitCost
    ) {}

    @PostMapping("/{id}/parts")
    @Operation(summary = "Thêm linh kiện thay thế vào phiếu bảo hành")
    public ResponseEntity<AdminWarrantyRequestResponse> addPart(
            @PathVariable Long id,
            @Valid @RequestBody AddPartInput input) {
        return ResponseEntity.ok(service.addPartToRequest(
                id,
                input.productId(),
                input.quantity(),
                input.unitCost() != null ? input.unitCost() : 0L
        ));
    }
}
