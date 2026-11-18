package com.datn.pc_store.modules.analytics.dto.response;

import com.datn.pc_store.modules.order.enums.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dòng chi tiết hóa đơn trong bảng kê thuế")
public class TaxInvoiceDetailDto {

    @Schema(description = "ID hóa đơn", example = "1")
    private Long invoiceId;

    @Schema(description = "Số hóa đơn tài chính", example = "HD-20261101-ABC123")
    private String invoiceCode;

    @Schema(description = "Ngày lập hóa đơn")
    private Instant invoiceDate;

    @Schema(description = "Mã đơn hàng liên kết", example = "ORD-20261101-001")
    private String orderCode;

    @Schema(description = "Tên khách hàng", example = "Nguyễn Văn A")
    private String customerName;

    @Schema(description = "Số điện thoại", example = "0987654321")
    private String customerPhone;

    @Schema(description = "Tổng tiền hàng trước giảm (VND)", example = "25000000")
    private long totalAmount;

    @Schema(description = "Tiền chiết khấu / giảm giá (VND)", example = "1000000")
    private long discountAmount;

    @Schema(description = "Doanh thu tính thuế (VND)", example = "24000000")
    private long amount;

    @Schema(description = "Thuế GTGT tính trên hóa đơn này (VND)", example = "1920000")
    private long vatAmount;

    @Schema(description = "Thuế TNCN tính trên hóa đơn này (VND)", example = "4080000")
    private long pitAmount;

    @Schema(description = "Trạng thái hóa đơn")
    private InvoiceStatus status;
}
