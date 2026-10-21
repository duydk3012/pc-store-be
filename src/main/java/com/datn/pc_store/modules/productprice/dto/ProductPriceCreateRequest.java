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
@Schema(description = "Yêu cầu áp dụng giá bán mới ngay lập tức; giá cũ được đóng hiệu lực và giữ lịch sử")
public class ProductPriceCreateRequest {
    @NotNull(message = "Sản phẩm không được để trống")
    @Positive(message = "ID sản phẩm phải lớn hơn 0")
    @Schema(description = "ID sản phẩm chưa bị xóa", example = "1")
    private Long productId;

    @NotNull(message = "Giá bán không được để trống")
    @Positive(message = "Giá bán phải lớn hơn 0")
    @Digits(integer = 19, fraction = 0, message = "Giá bán phải là số nguyên VND")
    @DecimalMax(value = "9223372036854775807", message = "Giá bán vượt giới hạn cho phép")
    @Schema(description = "Giá bán mới, số nguyên VND lớn hơn 0", type = "integer", format = "int64", example = "12990000")
    private BigDecimal price;
}
