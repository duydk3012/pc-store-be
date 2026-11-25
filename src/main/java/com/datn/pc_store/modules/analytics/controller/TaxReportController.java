package com.datn.pc_store.modules.analytics.controller;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.modules.analytics.dto.request.TaxReportFilterRequest;
import com.datn.pc_store.modules.analytics.dto.response.TaxInvoiceDetailDto;
import com.datn.pc_store.modules.analytics.dto.response.TaxQuarterlyResponse;
import com.datn.pc_store.modules.analytics.dto.response.TaxSummaryResponse;
import com.datn.pc_store.modules.analytics.service.TaxCalculationService;
import com.datn.pc_store.modules.analytics.service.TaxExcelExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics/tax")
@RequiredArgsConstructor
@Tag(name = "Analytics - Tax", description = "Báo cáo Hoạch toán Thuế & Xuất Excel")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class TaxReportController {

    private final TaxCalculationService taxCalculationService;
    private final TaxExcelExportService taxExcelExportService;

    @GetMapping("/summary")
    @Operation(summary = "Tính toán nghĩa vụ thuế luỹ tiến theo Doanh thu năm/quý",
            description = "Áp dụng công thức thuế trực tiếp: < 1 tỷ: 0%; 1 - 3 tỷ: GTGT 1%, TNCN 0.5% (Tổng 1.5%); 3 - 50 tỷ: GTGT 8%, TNCN 17% (Tổng 25%); > 50 tỷ: GTGT 8%, TNCN 20% (Tổng 28%)")
    public ResponseEntity<TaxSummaryResponse> getTaxSummary(@Valid @ParameterObject TaxReportFilterRequest filter) {
        return ResponseEntity.ok(taxCalculationService.calculateTaxSummary(filter));
    }

    @GetMapping("/quarterly-breakdown")
    @Operation(summary = "Bảng kê phân bổ nghĩa vụ thuế tạm tính 4 Quý trong năm")
    public ResponseEntity<TaxQuarterlyResponse> getTaxQuarterlyBreakdown(
            @Parameter(description = "Năm tính thuế", example = "2026")
            @RequestParam int year) {
        return ResponseEntity.ok(taxCalculationService.getTaxQuarterlyBreakdown(year));
    }

    @GetMapping("/invoices")
    @Operation(summary = "Danh sách hóa đơn chi tiết cấu thành doanh thu tính thuế (hỗ trợ phân trang, kiểm toán)")
    public ResponseEntity<PageResponse<TaxInvoiceDetailDto>> getTaxInvoices(
            @Valid @ParameterObject TaxReportFilterRequest filter,
            @PageableDefault(size = 20, sort = "invoiceDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(taxCalculationService.getTaxInvoices(filter, pageable));
    }

    @GetMapping("/export/excel")
    @Operation(summary = "Xuất file Excel (.xlsx) Báo cáo Hoạch toán Thuế (Sheet 1: Tờ khai tổng hợp, Sheet 2: Bảng kê hóa đơn)")
    public ResponseEntity<byte[]> exportTaxExcel(@Valid @ParameterObject TaxReportFilterRequest filter) {
        int year = (filter != null && filter.getYear() != null)
                ? filter.getYear()
                : java.time.LocalDate.now(com.datn.pc_store.modules.analytics.util.AnalyticsDateHelper.VN_ZONE).getYear();
        byte[] excelBytes = taxExcelExportService.exportTaxReportToExcel(filter);

        String filename = "Bao_Cao_Hoach_Toan_Thue_" + year + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}
