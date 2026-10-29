package com.datn.pc_store.modules.purchasereturn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Chi tiết một dòng serial trong phiếu hoàn trả")
public class PurchaseReturnItemResponse {
    @Schema(description = "ID dòng hoàn trả", example = "1")
    private Long id;

    @Schema(description = "ID phiếu hoàn trả", example = "1")
    private Long purchaseReturnId;

    @Schema(description = "ID serial sản phẩm", example = "10014")
    private Long productSerialId;

    @Schema(description = "Mã serial của sản phẩm", example = "SN-ASUS-MB-001")
    private String serialNumber;

    @Schema(description = "ID sản phẩm", example = "2001")
    private Long productId;

    @Schema(description = "Mã sản phẩm", example = "MB-ASUS-B760M")
    private String productCode;

    @Schema(description = "Tên sản phẩm", example = "Bo mạch chủ ASUS ROG STRIX B760-G GAMING")
    private String productName;

    @Schema(description = "Đơn giá vốn xuất trả (lấy từ phiếu nhập kho gốc)", example = "5200000")
    private long unitCost;

    @Schema(description = "Mã phiếu nhập gốc", example = "GR-20261015-001")
    private String receiptCode;

    @Schema(description = "Thời gian nhận hàng gốc")
    private Instant receiptDate;

    @Schema(description = "Thời gian tạo")
    private Instant createdDate;

    @Schema(description = "ID người tạo")
    private Long createdBy;

    @Schema(description = "Thời gian cập nhật")
    private Instant lastModifiedDate;

    @Schema(description = "ID người cập nhật")
    private Long lastModifiedBy;
}
