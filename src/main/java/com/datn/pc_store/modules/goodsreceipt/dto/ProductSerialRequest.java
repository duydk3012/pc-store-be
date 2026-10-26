package com.datn.pc_store.modules.goodsreceipt.dto;

import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin serial sản phẩm khi nhận hàng")
public class ProductSerialRequest {
    @Size(max = 100, message = "Số serial tối đa 100 ký tự")
    @Schema(description = "Số serial duy nhất của sản phẩm", example = "SN-20261026-0001")
    private String serialNumber;

    @Schema(description = "Tình trạng khi nhận hàng: GOOD_CONDITION (hàng tốt) hoặc DEFECTIVE (lỗi hỏng)", example = "GOOD_CONDITION")
    private ProductSerialStatus status;
}
