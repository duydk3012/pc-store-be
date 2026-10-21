package com.datn.pc_store.modules.purchaseorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu sửa đơn DRAFT; thay thế toàn bộ danh sách mặt hàng và tự tính lại tổng tiền")
public class PurchaseOrderUpdateRequest {
    @NotBlank(message = "Mã đơn nhập không được để trống")
    @Size(max = 50, message = "Mã đơn nhập tối đa 50 ký tự")
    @Schema(description = "Mã đơn nhập duy nhất, tối đa 50 ký tự; mã của đơn đã xóa vẫn được giữ lại", example = "PO-20261021-001")
    private String code;

    @Size(max = 100, message = "Tên đơn nhập tối đa 100 ký tự")
    @Schema(description = "Tên đơn nhập, tối đa 100 ký tự (không bắt buộc)", example = "Nhập linh kiện tháng 10")
    private String name;

    @NotNull(message = "Nhà cung cấp không được để trống")
    @Positive(message = "ID nhà cung cấp phải lớn hơn 0")
    @Schema(description = "ID nhà cung cấp đang ACTIVE và chưa bị xóa", example = "1")
    private Long supplierId;

    @Schema(description = "Thời điểm đặt hàng ISO 8601; giữ thời điểm đặt hàng cũ nếu không nhập", example = "2026-10-21T08:00:00Z")
    private Instant orderDate;

    @Schema(description = "Thời điểm giao dự kiến ISO 8601; không được trước orderDate (không bắt buộc)", example = "2026-10-28T08:00:00Z")
    private Instant expectedDate;

    @Size(max = 16000, message = "Mô tả tối đa 16000 ký tự")
    @Schema(description = "Ghi chú hoặc thỏa thuận với nhà cung cấp, tối đa 16000 ký tự", example = "Giao hàng trong giờ hành chính")
    private String description;

    @Valid
    @Size(max = 500, message = "Một đơn nhập tối đa 500 mặt hàng")
    @Schema(description = "Danh sách mặt hàng đặt mua; có thể rỗng khi lưu nháp, phải có ít nhất một mặt hàng khi gửi duyệt")
    private List<@NotNull PurchaseOrderItemRequest> items;
}
