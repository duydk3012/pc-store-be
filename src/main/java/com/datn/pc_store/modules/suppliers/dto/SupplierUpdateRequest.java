package com.datn.pc_store.modules.suppliers.dto;

import com.datn.pc_store.modules.suppliers.enums.SupplierStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu chỉnh sửa thông tin nhà cung cấp")
public class SupplierUpdateRequest {

    @NotBlank(message = "Mã nhà cung cấp không được để trống")
    @Size(max = 50, message = "Mã nhà cung cấp tối đa 50 ký tự")
    @Schema(description = "Mã định danh nhà cung cấp", example = "NCC_ASUS")
    private String code;

    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    @Size(max = 100, message = "Tên nhà cung cấp tối đa 100 ký tự")
    @Schema(description = "Tên nhà cung cấp", example = "Công ty TNHH Asus Việt Nam")
    private String name;

    @Size(max = 255, message = "Người liên hệ tối đa 255 ký tự")
    @Schema(description = "Tên người đại diện liên hệ", example = "Nguyễn Văn A")
    private String contactPerson;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    @Schema(description = "Email liên hệ chính thức", example = "contact@asus.vn")
    private String email;

    @Pattern(regexp = "^$|^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ")
    @Schema(description = "Số điện thoại liên hệ", example = "0901234567")
    private String phoneNumber;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    @Schema(description = "Địa chỉ trụ sở", example = "Tầng 5, Tòa nhà Bitexco, Q.1, TP.HCM")
    private String address;

    @Size(max = 50, message = "Mã số thuế tối đa 50 ký tự")
    @Schema(description = "Mã số thuế doanh nghiệp", example = "0312345678")
    private String taxCode;

    @Schema(description = "Trạng thái hoạt động", example = "ACTIVE")
    private SupplierStatus status;
}
