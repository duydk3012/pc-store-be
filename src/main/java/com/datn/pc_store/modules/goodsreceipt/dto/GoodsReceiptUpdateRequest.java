package com.datn.pc_store.modules.goodsreceipt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dữ liệu cập nhật phiếu nhập hàng (chỉ thực hiện khi phiếu ở trạng thái DRAFT)")
public class GoodsReceiptUpdateRequest {
    @NotBlank(message = "Mã phiếu nhập không được để trống")
    @Size(max = 50, message = "Mã phiếu nhập tối đa 50 ký tự")
    @Schema(description = "Mã phiếu nhập duy nhất", example = "GR-20261026-001")
    private String code;

    @Size(max = 100, message = "Tên phiếu nhập tối đa 100 ký tự")
    @Schema(description = "Tên phiếu nhập", example = "Phiếu nhận hàng đợt 1 cập nhật")
    private String name;

    @Schema(description = "Thời gian nhận hàng")
    private Instant receivedAt;

    @Valid
    @Schema(description = "Danh sách mặt hàng thực nhận (sẽ thay thế toàn bộ danh sách cũ)")
    private List<ReceiptItemRequest> items;
}
