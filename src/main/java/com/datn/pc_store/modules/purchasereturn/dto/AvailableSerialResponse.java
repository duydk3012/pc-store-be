package com.datn.pc_store.modules.purchasereturn.dto;

import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin serial sản phẩm lỗi khả dụng để hoàn trả cho NCC")
public class AvailableSerialResponse {
    @Schema(description = "ID serial sản phẩm", example = "10014")
    private Long productSerialId;

    @Schema(description = "Mã serial", example = "SN-ASUS-MB-001")
    private String serialNumber;

    @Schema(description = "ID sản phẩm", example = "2001")
    private Long productId;

    @Schema(description = "Mã sản phẩm", example = "MB-ASUS-B760M")
    private String productCode;

    @Schema(description = "Tên sản phẩm", example = "Bo mạch chủ ASUS ROG STRIX B760-G GAMING")
    private String productName;

    @Schema(description = "Trạng thái hiện tại của serial", example = "DEFECTIVE")
    private ProductSerialStatus status;

    @Schema(description = "Giá vốn lúc nhập kho (VNĐ)", example = "5200000")
    private long unitCost;

    @Schema(description = "Mã phiếu nhập kho gốc", example = "GR-20261015-001")
    private String receiptCode;

    @Schema(description = "Thời gian nhận hàng gốc")
    private Instant receiptDate;
}
