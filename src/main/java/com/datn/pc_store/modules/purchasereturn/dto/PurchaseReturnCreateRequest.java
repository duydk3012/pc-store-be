package com.datn.pc_store.modules.purchasereturn.dto;

import com.datn.pc_store.modules.purchasereturn.enums.PurchaseReturnStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu tạo mới phiếu hoàn trả hàng nhà cung cấp")
public class PurchaseReturnCreateRequest {

    @NotBlank(message = "Mã phiếu hoàn trả không được để trống")
    @Size(max = 50, message = "Mã phiếu hoàn trả tối đa 50 ký tự")
    @Schema(description = "Mã phiếu hoàn trả duy nhất", example = "PR-20261029-001")
    private String code;

    @Size(max = 255, message = "Tên phiếu tối đa 255 ký tự")
    @Schema(description = "Tên phiếu hoàn trả", example = "Hoàn trả lô mainboard lỗi tháng 10")
    private String name;

    @NotNull(message = "Nhà cung cấp không được để trống")
    @Schema(description = "ID nhà cung cấp nhận hàng hoàn trả", example = "10001")
    private Long supplierId;

    @Size(max = 255, message = "Lý do hoàn trả tối đa 255 ký tự")
    @Schema(description = "Lý do hoàn trả", example = "Lỗi chip cầu nam phát sinh DOA")
    private String reason;

    @Size(max = 500, message = "Mô tả chi tiết tối đa 500 ký tự")
    @Schema(description = "Mô tả chi tiết", example = "Biên bản kiểm tra kỹ thuật số 45/BB-KT")
    private String description;

    @Schema(description = "Thời gian xuất trả hàng, ISO 8601")
    private Instant returnDate;

    @Schema(description = "Trạng thái khởi tạo: PENDING (0) hoặc DRAFT (4). Mặc định là PENDING nếu không truyền.", example = "PENDING")
    private PurchaseReturnStatus status;

    @Schema(description = "Danh sách chuỗi mã serial quét từ máy quét barcode (hoặc chọn thủ công)")
    private List<String> serialNumbers;

    @Schema(description = "Danh sách ID của serial (tuỳ chọn nếu chọn từ danh mục có sẵn ID)")
    private List<Long> productSerialIds;
}
