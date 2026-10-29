package com.datn.pc_store.modules.goodsreceipt.dto;

import com.datn.pc_store.modules.goodsreceipt.enums.GoodsReceiptStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dữ liệu tạo phiếu nhập hàng")
public class GoodsReceiptCreateRequest {
    @NotBlank(message = "Mã phiếu nhập không được để trống")
    @Size(max = 50, message = "Mã phiếu nhập tối đa 50 ký tự")
    @Schema(description = "Mã phiếu nhập duy nhất", example = "GR-20261026-001")
    private String code;

    @Size(max = 100, message = "Tên phiếu nhập tối đa 100 ký tự")
    @Schema(description = "Tên phiếu nhập", example = "Phiếu nhận hàng đợt 1")
    private String name;

    @NotNull(message = "purchaseOrderId không được để trống")
    @Schema(description = "ID đơn đặt hàng tương ứng", example = "1")
    private Long purchaseOrderId;

    @Schema(description = "Thời gian nhận hàng")
    private Instant receivedAt;

    @Schema(description = "Trạng thái khởi tạo: mặc định PENDING (hoặc DRAFT nếu lưu nháp)", example = "PENDING")
    private GoodsReceiptStatus status;

    @Valid
    @Schema(description = "Danh sách mặt hàng thực nhận")
    private List<ReceiptItemRequest> items;
}
