package com.datn.pc_store.modules.product.dto;

import com.datn.pc_store.modules.product.enums.ProductStatus;
import com.datn.pc_store.modules.product.enums.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Set;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu thêm mới sản phẩm")
public class ProductCreateRequest {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm tối đa 255 ký tự")
    @Schema(description = "Tên sản phẩm, tối đa 255 ký tự", example = "Card màn hình ASUS RTX 5070")
    private String name;

    @NotBlank(message = "Mã sản phẩm không được để trống")
    @Size(max = 50, message = "Mã sản phẩm tối đa 50 ký tự")
    @Schema(description = "Mã sản phẩm duy nhất, tối đa 50 ký tự", example = "ASUS-RTX5070-12G")
    private String code;

    @Positive(message = "Giá bán phải lớn hơn 0")
    @Digits(integer = 19, fraction = 0, message = "Giá bán phải là số nguyên VND")
    @DecimalMax(value = "9223372036854775807", message = "Giá bán vượt giới hạn cho phép")
    @Schema(description = "Giá bán hiện tại, số nguyên VND; nếu nhập sẽ tự tạo bản lịch sử giá đầu tiên", type = "integer", format = "int64", example = "12990000")
    private BigDecimal price;

    @Size(max = 255, message = "Slug tối đa 255 ký tự")
    @Schema(description = "Đường dẫn duy nhất; tự động tạo từ tên nếu không nhập", example = "card-man-hinh-asus-rtx-5070")
    private String slug;

    @Size(max = 16000, message = "Mô tả tối đa 16000 ký tự")
    @Schema(description = "Mô tả sản phẩm, tối đa 16000 ký tự (không bắt buộc)", example = "Card đồ họa dành cho chơi game và thiết kế")
    private String description;

    @Size(max = 16000, message = "Thông số kỹ thuật tối đa 16000 ký tự")
    @Schema(description = "Thông số kỹ thuật, tối đa 16000 ký tự (không bắt buộc)", example = "VRAM: 12GB GDDR7; giao tiếp: PCIe")
    private String specification;

    @Schema(description = "Loại sản phẩm: COMPONENT (linh kiện), LAPTOP hoặc PC; mặc định COMPONENT nếu không nhập", example = "COMPONENT")
    private ProductType productType;

    @Schema(description = "Trạng thái: INACTIVE (ẩn), ACTIVE (hoạt động), OUT_OF_STOCK (hết hàng); mặc định ACTIVE nếu không nhập", example = "ACTIVE")
    private ProductStatus status;

    @Size(max = 500, message = "Đường dẫn ảnh tối đa 500 ký tự")
    @Schema(description = "Đường dẫn ảnh sản phẩm, tối đa 500 ký tự (không bắt buộc)", example = "https://example.com/images/rtx5070.jpg")
    private String imageUrl;

    @Positive(message = "ID thương hiệu phải lớn hơn 0")
    @Schema(description = "ID thương hiệu chưa bị xóa; null nếu không có thương hiệu", example = "1")
    private Long brandId;

    @Schema(description = "Danh sách ID danh mục chưa bị xóa; null hoặc rỗng nếu không có danh mục", example = "[1, 2]")
    private Set<@NotNull @Positive Long> categoryIds;

    @Positive(message = "ID danh mục chính phải lớn hơn 0")
    @Schema(description = "ID danh mục chính, phải nằm trong categoryIds; null nếu không chọn danh mục chính", example = "1")
    private Long primaryCategoryId;
}
