package com.datn.pc_store.modules.user.dto;

import com.datn.pc_store.modules.user.enums.RoleType;
import com.datn.pc_store.modules.user.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu quản trị viên tạo tài khoản người dùng")
public class UserCreateRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 4, max = 50, message = "Tên đăng nhập từ 4 đến 50 ký tự")
    @Schema(description = "Tên đăng nhập duy nhất, từ 4 đến 50 ký tự", example = "customer01")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 72, message = "Mật khẩu từ 8 đến 72 ký tự")
    @Schema(description = "Mật khẩu từ 8 đến 72 ký tự, được mã hóa trước khi lưu", example = "Password123!", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
    @Schema(description = "Họ và tên người dùng, tối đa 100 ký tự", example = "Nguyễn Văn An")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email tối đa 255 ký tự")
    @Schema(description = "Email hợp lệ và duy nhất, tối đa 255 ký tự", example = "customer01@example.com")
    private String email;

    @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "Số điện thoại không hợp lệ")
    @Schema(description = "Số điện thoại bắt đầu bằng 0 hoặc +84, theo sau là 9 chữ số (không bắt buộc)", example = "0912345678")
    private String phoneNumber;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    @Schema(description = "Địa chỉ liên hệ, tối đa 500 ký tự (không bắt buộc)", example = "123 Nguyễn Trãi, Hà Nội")
    private String address;

    @Size(max = 20, message = "Số căn cước công dân tối đa 20 ký tự")
    @Schema(description = "Số căn cước công dân, tối đa 20 ký tự (không bắt buộc)", example = "001099012345")
    private String citizenIdentificationCard;

    @Schema(description = "Vai trò tài khoản; mặc định CUSTOMER nếu không nhập", example = "CUSTOMER")
    private RoleType role;

    @Schema(description = "Trạng thái tài khoản; mặc định ACTIVE nếu không nhập", example = "ACTIVE")
    private UserStatus status;
}
