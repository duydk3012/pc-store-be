package com.datn.pc_store.modules.goodsreceipt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin serial sản phẩm")
public class ProductSerialResponse {
    @Schema(description = "ID serial", example = "1")
    private Long id;

    @Schema(description = "ID sản phẩm", example = "10")
    private Long productId;

    @Schema(description = "ID dòng nhận hàng", example = "5")
    private Long receiptItemId;

    @Schema(description = "Số serial", example = "SN-20261026-0001")
    private String serialNumber;

    @Schema(description = "Trạng thái serial", example = "GOOD_CONDITION")
    private ProductSerialStatus status;

    @JsonProperty("isTerminated")
    @Schema(description = "Đã bị kết thúc/hủy (không sử dụng trong kinh doanh, chỉ xem lịch sử)", example = "false")
    private boolean isTerminated;

    @Schema(description = "Thời gian tạo")
    private Instant createdDate;

    @Schema(description = "ID người tạo")
    private Long createdBy;

    @Schema(description = "Thời gian cập nhật")
    private Instant lastModifiedDate;

    @Schema(description = "ID người cập nhật")
    private Long lastModifiedBy;
}
