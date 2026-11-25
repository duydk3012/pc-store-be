package com.datn.pc_store.modules.cart.dto;

import com.datn.pc_store.modules.payment.enums.PaymentMethodCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu thanh toán từ giỏ hàng để chuyển thành đơn hàng")
public class CartCheckoutRequest {

    @Schema(description = "Danh sách ID sản phẩm được chọn thanh toán (bỏ trống để thanh toán toàn bộ giỏ)", example = "[10003, 10011]")
    private List<Long> selectedProductIds;

    @Schema(description = "Phương thức thanh toán (COD hoặc ZALOPAY, mặc định COD)", example = "COD")
    private PaymentMethodCode paymentMethod;

    @Schema(description = "Tên người nhận hàng (mặc định lấy từ hồ sơ tài khoản nếu bỏ trống)", example = "Nguyễn Minh An")
    private String recipientName;

    @Schema(description = "Số điện thoại người nhận (mặc định lấy từ hồ sơ tài khoản nếu bỏ trống)", example = "0901000011")
    private String recipientPhone;

    @Schema(description = "Địa chỉ nhận hàng (mặc định lấy từ hồ sơ tài khoản nếu bỏ trống)", example = "Số 18 Hoàng Quốc Việt, Cầu Giấy, Hà Nội")
    private String address;

    @Schema(description = "Ghi chú đơn hàng", example = "Giao giờ hành chính")
    private String note;

    @Min(value = 0, message = "Số tiền giảm giá không được âm")
    @Schema(description = "Số tiền giảm giá đơn hàng (nếu có)", example = "0")
    private Long discountAmount;
}
