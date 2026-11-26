package com.datn.pc_store.modules.warranty;

import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.GlobalExceptionHandler;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.brand.entity.Brand;
import com.datn.pc_store.modules.brand.repository.BrandRepository;
import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.order.entity.OrderItem;
import com.datn.pc_store.modules.order.entity.OrderItemSerial;
import com.datn.pc_store.modules.order.repository.OrderItemRepository;
import com.datn.pc_store.modules.order.repository.OrderItemSerialRepository;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.warranty.controller.PublicWarrantyController;
import com.datn.pc_store.modules.warranty.dto.PublicWarrantyResponse;
import com.datn.pc_store.modules.warranty.entity.WarrantPolicy;
import com.datn.pc_store.modules.warranty.entity.Warranty;
import com.datn.pc_store.modules.warranty.repository.WarrantyPolicyRepository;
import com.datn.pc_store.modules.warranty.repository.WarrantyRepository;
import com.datn.pc_store.modules.warranty.service.PublicWarrantyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicWarrantyLookupTest {

    @Mock
    private WarrantyRepository warranties;
    @Mock
    private WarrantyPolicyRepository policies;
    @Mock
    private OrderItemSerialRepository orderItemSerials;
    @Mock
    private OrderItemRepository orderItems;
    @Mock
    private ProductSerialRepository productSerials;
    @Mock
    private ProductRepository products;
    @Mock
    private BrandRepository brands;

    @InjectMocks
    private PublicWarrantyService publicWarrantyService;

    private Warranty buildWarranty(Instant start, Instant end, boolean status) {
        Warranty w = new Warranty();
        w.setId(100L);
        w.setOrderItemSerialId(200L);
        w.setWarrantyPolicyId(300L);
        w.setStartDate(start);
        w.setEndDate(end);
        w.setStatus(status);
        w.setNote("Internal staff note");
        return w;
    }

    private void stubProductChain(String serial) {
        OrderItemSerial ois = new OrderItemSerial();
        ois.setId(200L);
        ois.setOrderItemId(400L);
        ois.setProductSerialId(500L);
        when(orderItemSerials.findById(200L)).thenReturn(Optional.of(ois));

        ProductSerial ps = ProductSerial.builder().id(500L).productId(600L).serialNumber(serial).build();
        when(productSerials.findById(500L)).thenReturn(Optional.of(ps));

        OrderItem oi = new OrderItem();
        oi.setId(400L);
        oi.setProductId(600L);
        when(orderItems.findById(400L)).thenReturn(Optional.of(oi));

        Product product = new Product();
        product.setId(600L);
        product.setName("RTX 4070 Super");
        product.setCode("VGA-4070S");
        product.setBrandId(700L);
        when(products.findById(600L)).thenReturn(Optional.of(product));

        Brand brand = new Brand();
        brand.setId(700L);
        brand.setName("ASUS");
        when(brands.findById(700L)).thenReturn(Optional.of(brand));

        WarrantPolicy policy = WarrantPolicy.builder()
                .id(300L)
                .name("Bảo hành chính hãng 36 tháng")
                .durationDays(1095)
                .build();
        when(policies.findById(300L)).thenReturn(Optional.of(policy));
    }

    @Test
    @DisplayName("Trả về BadRequestException khi serial rỗng hoặc chỉ chứa khoảng trắng")
    void throwsBadRequestWhenSerialBlank() {
        assertThrows(BadRequestException.class, () -> publicWarrantyService.lookupByExactSerial("   "));
        assertThrows(BadRequestException.class, () -> publicWarrantyService.lookupByExactSerial(null));
    }

    @Test
    @DisplayName("Trả về ResourceNotFoundException khi không tìm thấy bảo hành cho serial")
    void throwsNotFoundWhenSerialMissing() {
        when(warranties.findBySerialNumber("SN-NOT-FOUND")).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class,
                () -> publicWarrantyService.lookupByExactSerial("  SN-NOT-FOUND  "));
    }

    @Test
    @DisplayName("Tra cứu thành công bảo hành đang ACTIVE và chỉ trả thông tin không nhạy cảm")
    void returnsActivePublicWarrantyInfo() {
        Instant now = Instant.now();
        Warranty w = buildWarranty(now.minus(30, ChronoUnit.DAYS), now.plus(100, ChronoUnit.DAYS), true);
        when(warranties.findBySerialNumber("SN-ACTIVE-001")).thenReturn(List.of(w));
        stubProductChain("SN-ACTIVE-001");

        PublicWarrantyResponse res = publicWarrantyService.lookupByExactSerial(" SN-ACTIVE-001 ");

        assertEquals("SN-ACTIVE-001", res.serialNumber());
        assertEquals(600L, res.productId());
        assertEquals("RTX 4070 Super", res.productName());
        assertEquals("VGA-4070S", res.productCode());
        assertEquals("ASUS", res.brandName());
        assertEquals("Bảo hành chính hãng 36 tháng", res.policyName());
        assertEquals(1095, res.durationDays());
        assertTrue(res.active());
        assertEquals("ACTIVE", res.status());
        assertTrue(res.remainingDays() >= 99);
    }

    @Test
    @DisplayName("Trả về EXPIRING_SOON khi bảo hành còn dưới 30 ngày")
    void returnsExpiringSoonWhenUnder30Days() {
        Instant now = Instant.now();
        Warranty w = buildWarranty(now.minus(350, ChronoUnit.DAYS), now.plus(10, ChronoUnit.DAYS), true);
        when(warranties.findBySerialNumber("SN-SOON-001")).thenReturn(List.of(w));
        stubProductChain("SN-SOON-001");

        PublicWarrantyResponse res = publicWarrantyService.lookupByExactSerial("SN-SOON-001");

        assertTrue(res.active());
        assertEquals("EXPIRING_SOON", res.status());
        assertTrue(res.remainingDays() >= 9 && res.remainingDays() <= 10);
    }

    @Test
    @DisplayName("Trả về EXPIRED và remainingDays = 0 khi bảo hành bị hủy hiệu lực")
    void returnsExpiredWhenVoided() {
        Instant now = Instant.now();
        Warranty w = buildWarranty(now.minus(30, ChronoUnit.DAYS), now.plus(100, ChronoUnit.DAYS), false);
        when(warranties.findBySerialNumber("SN-VOID-001")).thenReturn(List.of(w));
        stubProductChain("SN-VOID-001");

        PublicWarrantyResponse res = publicWarrantyService.lookupByExactSerial("SN-VOID-001");

        assertFalse(res.active());
        assertEquals("EXPIRED", res.status());
        assertEquals(0L, res.remainingDays());
    }

    @Test
    @DisplayName("Endpoint GET /api/public/warranties/serials trả về HTTP 200 và không chứa trường nhạy cảm")
    void controllerEndpointReturns200WithoutSensitiveFields() throws Exception {
        Instant now = Instant.now();
        Warranty w = buildWarranty(now.minus(10, ChronoUnit.DAYS), now.plus(60, ChronoUnit.DAYS), true);
        when(warranties.findBySerialNumber("SN-HTTP-001")).thenReturn(List.of(w));
        stubProductChain("SN-HTTP-001");

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new PublicWarrantyController(publicWarrantyService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/public/warranties/serials").param("serialNumber", "SN-HTTP-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serialNumber").value("SN-HTTP-001"))
                .andExpect(jsonPath("$.productName").value("RTX 4070 Super"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.customerName").doesNotExist())
                .andExpect(jsonPath("$.customerPhone").doesNotExist())
                .andExpect(jsonPath("$.customerEmail").doesNotExist())
                .andExpect(jsonPath("$.orderCode").doesNotExist())
                .andExpect(jsonPath("$.note").doesNotExist());
    }
}
