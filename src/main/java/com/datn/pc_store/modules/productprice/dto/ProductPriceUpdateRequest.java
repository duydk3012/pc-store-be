package com.datn.pc_store.modules.productprice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Đổi giá hiện tại bằng một phiên bản giá mới; không ghi đè giá lịch sử")
public class ProductPriceUpdateRequest {
    @NotNull(message = "Giá bán không được để trống")
    @Positive(message = "Giá bán phải lớn hơn 0")
    @Digits(integer = 19, fraction = 0, message = "Giá bán phải là số nguyên VND")
    @DecimalMax(value = "9223372036854775807", message = "Giá bán vượt giới hạn cho phép")
    @Schema(description = "Giá bán mới, số nguyên VND lớn hơn 0", type = "integer", format = "int64", example = "11990000")
    private BigDecimal price;
}
