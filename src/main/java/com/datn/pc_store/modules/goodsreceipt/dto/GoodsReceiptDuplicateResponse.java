package com.datn.pc_store.modules.goodsreceipt.dto;

import com.datn.pc_store.modules.goodsreceipt.enums.GoodsReceiptStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dữ liệu mẫu nhân bản phiếu nhập hàng (dùng để fill form tạo mới)")
public class GoodsReceiptDuplicateResponse {

    @Schema(description = "ID phiếu nhập gốc đã bị hủy hoặc từ chối", example = "1")
    private Long originalReceiptId;

    @Schema(description = "Mã phiếu nhập gốc", example = "GR-20261026-001")
    private String originalReceiptCode;

    @Schema(description = "Gợi ý mã phiếu nhập mới (đã kiểm tra tính duy nhất)", example = "GR-20261026-001-DUP")
    private String code;

    @Schema(description = "Gợi ý tên phiếu nhập mới", example = "Phiếu nhận đợt 1 (Bản sao)")
    private String name;

    @Schema(description = "ID đơn đặt hàng (PO)", example = "1")
    private Long purchaseOrderId;

    @Schema(description = "Mã đơn đặt hàng (PO)", example = "PO-20261026-001")
    private String purchaseOrderCode;

    @Schema(description = "ID nhà cung cấp", example = "1")
    private Long supplierId;

    @Schema(description = "Tên nhà cung cấp", example = "Công ty TNHH Intel Việt Nam")
    private String supplierName;

    @Schema(description = "Thời gian nhận hàng mặc định (thời điểm hiện tại)")
    private Instant receivedAt;

    @Schema(description = "Trạng thái khởi tạo mặc định (DRAFT)", example = "DRAFT")
    private GoodsReceiptStatus status;

    @Schema(description = "Danh sách chi tiết mặt hàng và serial để điền sẵn vào form tạo mới")
    private List<ReceiptItemDuplicateResponse> items;
}
