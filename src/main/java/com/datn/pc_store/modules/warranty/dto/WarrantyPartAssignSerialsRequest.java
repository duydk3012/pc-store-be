package com.datn.pc_store.modules.warranty.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Yêu cầu gán serial cho linh kiện bảo hành")
public record WarrantyPartAssignSerialsRequest(
        @Schema(description = "Danh sách mã serial quét từ kho (ưu tiên sử dụng)")
        List<String> serialNumbers,

        @Schema(description = "Danh sách ID của serial (nếu chọn trực tiếp từ hệ thống)")
        List<Long> productSerialIds
) {}
