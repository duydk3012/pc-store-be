package com.datn.pc_store.modules.suppliers.dto;

import com.datn.pc_store.modules.suppliers.enums.SupplierStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin phản hồi của nhà cung cấp")
public class SupplierResponse {

    @Schema(description = "ID nhà cung cấp", example = "1")
    private Long id;

    @Schema(description = "Mã nhà cung cấp", example = "NCC_ASUS")
    private String code;

    @Schema(description = "Tên nhà cung cấp", example = "Công ty TNHH Asus Việt Nam")
    private String name;

    @Schema(description = "Tên người đại diện liên hệ", example = "Nguyễn Văn A")
    private String contactPerson;

    @Schema(description = "Email liên hệ chính thức", example = "contact@asus.vn")
    private String email;

    @Schema(description = "Số điện thoại liên hệ", example = "0901234567")
    private String phoneNumber;

    @Schema(description = "Địa chỉ trụ sở", example = "Tầng 5, Tòa nhà Bitexco, Q.1, TP.HCM")
    private String address;

    @Schema(description = "Mã số thuế", example = "0312345678")
    private String taxCode;

    @Schema(description = "Trạng thái hoạt động", example = "ACTIVE")
    private SupplierStatus status;

    @Schema(description = "Thời gian tạo")
    private Instant createdDate;

    @Schema(description = "ID người tạo")
    private Long createdBy;

    @Schema(description = "Thời gian cập nhật gần nhất")
    private Instant lastModifiedDate;

    @Schema(description = "ID người cập nhật gần nhất")
    private Long lastModifiedBy;
}
