package com.datn.pc_store.modules.purchaseorder.dto;

import com.datn.pc_store.modules.purchaseorder.enums.PurchaseOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin đơn nhập hàng và danh sách mặt hàng đặt mua")
public class PurchaseOrderResponse {
    @Schema(description = "ID đơn nhập", example = "1")
    private Long id;
    @Schema(description = "Mã đơn nhập duy nhất", example = "PO-20261021-001")
    private String code;
    @Schema(description = "Tên đơn nhập", example = "Nhập linh kiện tháng 10")
    private String name;
    @Schema(description = "ID nhà cung cấp", example = "1")
    private Long supplierId;
    @Schema(description = "Thời điểm đặt hàng, ISO 8601")
    private Instant orderDate;
    @Schema(description = "Thời điểm giao dự kiến, ISO 8601")
    private Instant expectedDate;
    @Schema(description = "Trạng thái hiện tại", example = "DRAFT")
    private PurchaseOrderStatus status;
    @Schema(description = "Ghi chú hoặc thỏa thuận với nhà cung cấp", example = "Giao hàng trong giờ hành chính")
    private String description;
    @Schema(description = "Tổng thành tiền các mặt hàng do hệ thống tính, VND", example = "50000000", accessMode = Schema.AccessMode.READ_ONLY)
    private long totalAmount;
    @Schema(description = "Chi tiết mặt hàng đặt mua")
    private List<PurchaseOrderItemResponse> items;
    @Schema(description = "Thời gian tạo")
    private Instant createdDate;
    @Schema(description = "ID người tạo")
    private Long createdBy;
    @Schema(description = "Thời gian cập nhật gần nhất")
    private Instant lastModifiedDate;
    @Schema(description = "ID người cập nhật gần nhất")
    private Long lastModifiedBy;
}
