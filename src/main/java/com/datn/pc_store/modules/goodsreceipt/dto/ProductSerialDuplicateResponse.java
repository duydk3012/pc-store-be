package com.datn.pc_store.modules.goodsreceipt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dữ liệu mẫu serial sản phẩm khi nhân bản phiếu nhập hàng")
public class ProductSerialDuplicateResponse {

    @Schema(description = "Số serial sản phẩm", example = "SN-20261026-0001")
    private String serialNumber;

    @Schema(description = "Trạng thái khởi tạo lại: GOOD_CONDITION hoặc DEFECTIVE", example = "GOOD_CONDITION")
    private ProductSerialStatus status;

    @Schema(description = "Trạng thái gốc trên phiếu nhập cũ", example = "GOOD_CONDITION")
    private ProductSerialStatus originalStatus;

    @JsonProperty("isAlreadyInUse")
    @Schema(description = "Cảnh báo: Số serial này hiện đã tồn tại và đang hoạt động trong hệ thống hay chưa", example = "false")
    private boolean isAlreadyInUse;
}
