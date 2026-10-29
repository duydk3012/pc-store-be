package com.datn.pc_store.modules.purchasereturn.dto;

import com.datn.pc_store.modules.purchasereturn.enums.PurchaseReturnStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin chi tiết phiếu hoàn trả hàng nhà cung cấp")
public class PurchaseReturnResponse {
    @Schema(description = "ID phiếu hoàn trả", example = "1")
    private Long id;

    @Schema(description = "Mã phiếu hoàn trả", example = "PR-20261029-001")
    private String code;

    @Schema(description = "Tên phiếu hoàn trả", example = "Hoàn trả lô mainboard lỗi tháng 10")
    private String name;

    @Schema(description = "ID nhà cung cấp", example = "10001")
    private Long supplierId;

    @Schema(description = "Mã nhà cung cấp", example = "SUP001")
    private String supplierCode;

    @Schema(description = "Tên nhà cung cấp", example = "Công ty TNHH ASUS Việt Nam")
    private String supplierName;

    @Schema(description = "Lý do hoàn trả", example = "Lỗi chip cầu nam phát sinh DOA")
    private String reason;

    @Schema(description = "Mô tả chi tiết", example = "Biên bản kiểm tra kỹ thuật số 45/BB-KT")
    private String description;

    @Schema(description = "Thời gian xuất trả hàng")
    private Instant returnDate;

    @Schema(description = "Tổng giá trị tiền hoàn trả (VNĐ)", example = "15600000")
    private long totalAmount;

    @Schema(description = "Tổng số lượng serial hoàn trả", example = "3")
    private int totalItems;

    @Schema(description = "Trạng thái phiếu hoàn trả", example = "PENDING")
    private PurchaseReturnStatus status;

    @Schema(description = "Chi tiết các thiết bị/serial hoàn trả")
    private List<PurchaseReturnItemResponse> items;

    @Schema(description = "Thời gian tạo")
    private Instant createdDate;

    @Schema(description = "ID người tạo")
    private Long createdBy;

    @Schema(description = "Thời gian cập nhật")
    private Instant lastModifiedDate;

    @Schema(description = "ID người cập nhật")
    private Long lastModifiedBy;
}
