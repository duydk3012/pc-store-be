package com.datn.pc_store.modules.productprice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin một phiên bản giá bán sản phẩm")
public class ProductPriceResponse {
    @Schema(description = "ID phiên bản giá", example = "1")
    private Long id;
    @Schema(description = "ID sản phẩm", example = "1")
    private Long productId;
    @Schema(description = "Giá bán, số nguyên VND", example = "12990000")
    private long price;
    @Schema(description = "Thời điểm bắt đầu hiệu lực do server ghi nhận, ISO 8601")
    private Instant effectiveFrom;
    @Schema(description = "Thời điểm kết thúc hiệu lực (không bao gồm mốc này); null nếu chưa đóng giá")
    private Instant effectiveTo;
    @JsonProperty("isPresentPrice")
    @Schema(description = "Bản giá được chọn áp dụng; phải đồng thời còn trong khoảng hiệu lực để là giá hiện tại", example = "true")
    private boolean presentPrice;
    @Schema(description = "ID người tạo phiên bản giá")
    private Long createdBy;
    @Schema(description = "Thời điểm tạo phiên bản giá")
    private Instant createdDate;
}
