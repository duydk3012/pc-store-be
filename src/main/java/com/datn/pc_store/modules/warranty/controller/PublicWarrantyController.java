package com.datn.pc_store.modules.warranty.controller;

import com.datn.pc_store.modules.warranty.dto.PublicWarrantyResponse;
import com.datn.pc_store.modules.warranty.service.PublicWarrantyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/warranties")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Warranties (Public)", description = "API tra cứu bảo hành công khai cho khách hàng theo số serial")
public class PublicWarrantyController {

    private final PublicWarrantyService publicWarrantyService;

    @GetMapping("/serials")
    @Operation(
            summary = "Tra cứu bảo hành theo số serial",
            description = "Khách hàng nhập chính xác số serial để tra cứu thời hạn và trạng thái bảo hành. Chỉ trả về thông tin không nhạy cảm."
    )
    public ResponseEntity<PublicWarrantyResponse> lookupBySerial(
            @Parameter(description = "Số serial chính xác của sản phẩm", required = true)
            @RequestParam String serialNumber
    ) {
        return ResponseEntity.ok(publicWarrantyService.lookupByExactSerial(serialNumber));
    }
}
