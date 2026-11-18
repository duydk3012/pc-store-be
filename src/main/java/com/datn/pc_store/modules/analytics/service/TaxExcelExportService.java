package com.datn.pc_store.modules.analytics.service;

import com.datn.pc_store.modules.analytics.dto.request.TaxReportFilterRequest;
import com.datn.pc_store.modules.analytics.dto.response.TaxQuarterDto;
import com.datn.pc_store.modules.analytics.dto.response.TaxQuarterlyResponse;
import com.datn.pc_store.modules.analytics.dto.response.TaxSummaryResponse;
import com.datn.pc_store.modules.analytics.repository.AnalyticsTaxRepository;
import com.datn.pc_store.modules.analytics.util.AnalyticsDateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxExcelExportService {

    private final TaxCalculationService taxCalculationService;
    private final AnalyticsTaxRepository taxRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    public byte[] exportTaxReportToExcel(TaxReportFilterRequest filter) {
        int year = (filter != null && filter.getYear() != null)
                ? filter.getYear()
                : java.time.LocalDate.now().getYear();

        TaxSummaryResponse summary = taxCalculationService.calculateTaxSummary(filter);
        TaxQuarterlyResponse quarterly = taxCalculationService.getTaxQuarterlyBreakdown(year);

        Instant[] range = (filter != null && filter.getQuarter() != null)
                ? AnalyticsDateHelper.resolveQuarterRange(year, filter.getQuarter())
                : AnalyticsDateHelper.resolveYearRange(year);
        List<Object[]> invoiceRows = taxRepository.queryAllTaxInvoicesForExport(range[0], range[1]);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Styles
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle subTitleStyle = createSubTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle labelBoldStyle = createBoldStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle borderCellStyle = createBorderCellStyle(workbook);
            CellStyle totalRowStyle = createTotalRowStyle(workbook);

            // ==========================================
            // SHEET 1: Tờ Khai Thuế & Tổng Hợp
            // ==========================================
            SXSSFSheet sheet1 = workbook.createSheet("Tờ Khai Thuế & Tổng Hợp");
            sheet1.trackAllColumnsForAutoSizing();

            int rowIdx = 0;

            // Title banner
            Row rowTitle = sheet1.createRow(rowIdx++);
            Cell cellTitle = rowTitle.createCell(0);
            cellTitle.setCellValue("HỆ THỐNG BÁN LẺ MÁY TÍNH & CÔNG NGHỆ PC STORE");
            cellTitle.setCellStyle(titleStyle);
            sheet1.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            Row rowSub = sheet1.createRow(rowIdx++);
            Cell cellSub = rowSub.createCell(0);
            String periodName = (filter != null && filter.getQuarter() != null)
                    ? "QUÝ " + filter.getQuarter() + " NĂM " + year
                    : "NĂM TÀI CHÍNH " + year;
            cellSub.setCellValue("BẢNG HOẠCH TOÁN NGHĨA VỤ THUẾ DOANH THU - " + periodName);
            cellSub.setCellStyle(subTitleStyle);
            sheet1.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

            rowIdx++; // Blank row

            // Section 1: Thông tin tổng quát
            Row rMeta1 = sheet1.createRow(rowIdx++);
            rMeta1.createCell(0).setCellValue("Thời điểm xuất báo cáo:");
            rMeta1.createCell(1).setCellValue(DATE_FMT.format(Instant.now()));

            Row rMeta2 = sheet1.createRow(rowIdx++);
            rMeta2.createCell(0).setCellValue("Số lượng hóa đơn hợp lệ:");
            rMeta2.createCell(1).setCellValue(summary.getTotalPaidInvoices() + " hóa đơn");

            rowIdx++; // Blank row

            // Section 2: Bảng tính nghĩa vụ thuế
            Row rSec1 = sheet1.createRow(rowIdx++);
            Cell cSec1 = rSec1.createCell(0);
            cSec1.setCellValue("I. TỔNG HỢP NGHĨA VỤ THUẾ THEO KHUNG DOANH THU");
            cSec1.setCellStyle(labelBoldStyle);

            Row rH1 = sheet1.createRow(rowIdx++);
            createCell(rH1, 0, "Chỉ tiêu kế toán / Thuế", headerStyle);
            createCell(rH1, 1, "Tỷ lệ áp dụng", headerStyle);
            createCell(rH1, 2, "Số tiền (VNĐ)", headerStyle);
            createCell(rH1, 3, "Ghi chú quy định", headerStyle);

            Row r1 = sheet1.createRow(rowIdx++);
            createCell(r1, 0, "1. Doanh thu kế toán chịu thuế", borderCellStyle);
            createCell(r1, 1, "-", borderCellStyle);
            createNumericCell(r1, 2, summary.getAnnualNetRevenue(), numberStyle);
            createCell(r1, 3, "Doanh thu thuần từ hóa đơn đã thanh toán", borderCellStyle);

            Row r2 = sheet1.createRow(rowIdx++);
            createCell(r2, 0, "2. Bậc khung thuế suất", borderCellStyle);
            createCell(r2, 1, summary.getAppliedTier().name(), borderCellStyle);
            createCell(r2, 2, summary.getTierDescription(), borderCellStyle);
            createCell(r2, 3, "Áp dụng phương pháp trực tiếp trên doanh thu", borderCellStyle);

            Row r3 = sheet1.createRow(rowIdx++);
            createCell(r3, 0, "3. Thuế Giá trị gia tăng (GTGT)", borderCellStyle);
            createCell(r3, 1, String.format("%.1f%%", summary.getVatRate() * 100), borderCellStyle);
            createNumericCell(r3, 2, summary.getVatAmount(), numberStyle);
            createCell(r3, 3, "Nộp vào ngân sách nhà nước", borderCellStyle);

            Row r4 = sheet1.createRow(rowIdx++);
            createCell(r4, 0, "4. Thuế Thu nhập cá nhân (TNCN)", borderCellStyle);
            createCell(r4, 1, String.format("%.1f%%", summary.getPitRate() * 100), borderCellStyle);
            createNumericCell(r4, 2, summary.getPitAmount(), numberStyle);
            createCell(r4, 3, "Nộp vào ngân sách nhà nước", borderCellStyle);

            Row r5 = sheet1.createRow(rowIdx++);
            createCell(r5, 0, "TỔNG THUẾ TRỰC TIẾP PHẢI NỘP", totalRowStyle);
            createCell(r5, 1, String.format("%.1f%%", summary.getTotalTaxRate() * 100), totalRowStyle);
            createNumericCell(r5, 2, summary.getTotalTaxAmount(), totalRowStyle);
            createCell(r5, 3, "Tổng GTGT + TNCN", totalRowStyle);

            rowIdx += 2; // Blank rows

            // Section 3: Bảng phân bổ 4 Quý
            Row rSec2 = sheet1.createRow(rowIdx++);
            Cell cSec2 = rSec2.createCell(0);
            cSec2.setCellValue("II. BẢNG PHÂN BỔ NGHĨA VỤ THUẾ TẠM TÍNH 4 QUÝ");
            cSec2.setCellStyle(labelBoldStyle);

            Row rHQ = sheet1.createRow(rowIdx++);
            createCell(rHQ, 0, "Kỳ tính thuế", headerStyle);
            createCell(rHQ, 1, "Số hóa đơn", headerStyle);
            createCell(rHQ, 2, "Doanh thu (VNĐ)", headerStyle);
            createCell(rHQ, 3, "Thuế GTGT (VNĐ)", headerStyle);
            createCell(rHQ, 4, "Thuế TNCN (VNĐ)", headerStyle);
            createCell(rHQ, 5, "Tổng thuế (VNĐ)", headerStyle);

            for (TaxQuarterDto q : quarterly.getQuarters()) {
                Row rQ = sheet1.createRow(rowIdx++);
                createCell(rQ, 0, "Quý " + q.getQuarter() + "/" + year, borderCellStyle);
                createNumericCell(rQ, 1, q.getInvoiceCount(), borderCellStyle);
                createNumericCell(rQ, 2, q.getQuarterRevenue(), numberStyle);
                createNumericCell(rQ, 3, q.getVatAmount(), numberStyle);
                createNumericCell(rQ, 4, q.getPitAmount(), numberStyle);
                createNumericCell(rQ, 5, q.getTotalTax(), numberStyle);
            }

            rowIdx += 3; // Space for signatures
            Row rSig = sheet1.createRow(rowIdx++);
            createCell(rSig, 0, "Người lập biểu", labelBoldStyle);
            createCell(rSig, 2, "Kế toán trưởng", labelBoldStyle);
            createCell(rSig, 4, "Đại diện pháp luật / Chủ cơ sở", labelBoldStyle);

            Row rSigSub = sheet1.createRow(rowIdx++);
            createCell(rSigSub, 0, "(Ký, họ tên)", borderCellStyle);
            createCell(rSigSub, 2, "(Ký, họ tên)", borderCellStyle);
            createCell(rSigSub, 4, "(Ký, đóng dấu)", borderCellStyle);

            for (int col = 0; col <= 5; col++) {
                sheet1.autoSizeColumn(col);
                sheet1.setColumnWidth(col, Math.max(sheet1.getColumnWidth(col), 5000));
            }

            // ==========================================
            // SHEET 2: Bảng Kê Hóa Đơn Bán Ra
            // ==========================================
            SXSSFSheet sheet2 = workbook.createSheet("Bảng Kê Hóa Đơn Bán Ra");
            sheet2.trackAllColumnsForAutoSizing();

            int rowIdx2 = 0;
            Row rTitle2 = sheet2.createRow(rowIdx2++);
            Cell cT2 = rTitle2.createCell(0);
            cT2.setCellValue("BẢNG KÊ CHI TIẾT HÓA ĐƠN BÁN HÀNG VÀ CHỨNG TỪ DOANH THU");
            cT2.setCellStyle(titleStyle);
            sheet2.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

            rowIdx2++; // Blank

            Row rHead2 = sheet2.createRow(rowIdx2++);
            createCell(rHead2, 0, "STT", headerStyle);
            createCell(rHead2, 1, "Số hóa đơn", headerStyle);
            createCell(rHead2, 2, "Ngày phát hành", headerStyle);
            createCell(rHead2, 3, "Mã đơn hàng", headerStyle);
            createCell(rHead2, 4, "Tên khách hàng", headerStyle);
            createCell(rHead2, 5, "Số điện thoại", headerStyle);
            createCell(rHead2, 6, "Tổng tiền hàng (VNĐ)", headerStyle);
            createCell(rHead2, 7, "Chiết khấu (VNĐ)", headerStyle);
            createCell(rHead2, 8, "Doanh thu tính thuế (VNĐ)", headerStyle);
            createCell(rHead2, 9, "Thuế GTGT (VNĐ)", headerStyle);
            createCell(rHead2, 10, "Thuế TNCN (VNĐ)", headerStyle);

            double vatRate = summary.getVatRate();
            double pitRate = summary.getPitRate();

            int stt = 1;
            long sumRevenue = 0L;
            long sumVat = 0L;
            long sumPit = 0L;

            for (Object[] r : invoiceRows) {
                Row row = sheet2.createRow(rowIdx2++);

                String invCode = (String) r[1];
                Instant invDate = null;
                if (r[2] instanceof Timestamp ts) invDate = ts.toInstant();
                else if (r[2] instanceof Instant inst) invDate = inst;
                String invDateStr = invDate != null ? DATE_FMT.format(invDate) : "";

                String orderCode = (String) r[3];
                String custName = (String) r[4];
                String custPhone = (String) r[5];
                long totalAmt = r[6] != null ? ((Number) r[6]).longValue() : 0L;
                long discAmt = r[7] != null ? ((Number) r[7]).longValue() : 0L;
                long amount = r[8] != null ? ((Number) r[8]).longValue() : 0L;

                long vatLine = Math.round(amount * vatRate);
                long pitLine = Math.round(amount * pitRate);

                sumRevenue += amount;
                sumVat += vatLine;
                sumPit += pitLine;

                createNumericCell(row, 0, stt++, borderCellStyle);
                createCell(row, 1, invCode, borderCellStyle);
                createCell(row, 2, invDateStr, borderCellStyle);
                createCell(row, 3, orderCode, borderCellStyle);
                createCell(row, 4, custName, borderCellStyle);
                createCell(row, 5, custPhone, borderCellStyle);
                createNumericCell(row, 6, totalAmt, numberStyle);
                createNumericCell(row, 7, discAmt, numberStyle);
                createNumericCell(row, 8, amount, numberStyle);
                createNumericCell(row, 9, vatLine, numberStyle);
                createNumericCell(row, 10, pitLine, numberStyle);
            }

            // Dòng tổng cộng
            Row rTotal = sheet2.createRow(rowIdx2);
            createCell(rTotal, 0, "TỔNG CỘNG", totalRowStyle);
            createCell(rTotal, 1, "", totalRowStyle);
            createCell(rTotal, 2, "", totalRowStyle);
            createCell(rTotal, 3, "", totalRowStyle);
            createCell(rTotal, 4, "", totalRowStyle);
            createCell(rTotal, 5, "", totalRowStyle);
            createCell(rTotal, 6, "", totalRowStyle);
            createCell(rTotal, 7, "", totalRowStyle);
            createNumericCell(rTotal, 8, sumRevenue, totalRowStyle);
            createNumericCell(rTotal, 9, sumVat, totalRowStyle);
            createNumericCell(rTotal, 10, sumPit, totalRowStyle);

            for (int col = 0; col <= 10; col++) {
                sheet2.autoSizeColumn(col);
                sheet2.setColumnWidth(col, Math.max(sheet2.getColumnWidth(col), 4000));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Lỗi khi kết xuất file Excel báo cáo thuế", e);
            throw new RuntimeException("Không thể tạo file Excel báo cáo thuế: " + e.getMessage(), e);
        }
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        if (style != null) cell.setCellStyle(style);
    }

    private void createNumericCell(Row row, int col, long value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        if (style != null) cell.setCellStyle(style);
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 14);
        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createSubTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 12);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        addBorders(style);
        return style;
    }

    private CellStyle createBoldStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createBorderCellStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Arial");
        style.setFont(font);
        addBorders(style);
        return style;
    }

    private CellStyle createNumberStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Arial");
        style.setFont(font);
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        addBorders(style);
        return style;
    }

    private CellStyle createTotalRowStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setBold(true);
        style.setFont(font);
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        addBorders(style);
        return style;
    }

    private void addBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
