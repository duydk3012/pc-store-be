package com.datn.pc_store.modules.warranty;

import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.brand.repository.BrandRepository;
import com.datn.pc_store.modules.category.repository.CategoryRepository;
import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.order.repository.OrderItemRepository;
import com.datn.pc_store.modules.order.repository.OrderItemSerialRepository;
import com.datn.pc_store.modules.order.repository.OrderRepository;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.repository.UserRepository;
import com.datn.pc_store.modules.warranty.dto.TechnicalHandlingUpdateRequest;
import com.datn.pc_store.modules.warranty.dto.WarrantyExtendRequest;
import com.datn.pc_store.modules.warranty.entity.Warranty;
import com.datn.pc_store.modules.warranty.entity.WarrantyPart;
import com.datn.pc_store.modules.warranty.entity.WarrantyPartSerial;
import com.datn.pc_store.modules.warranty.entity.WarrantyRequest;
import com.datn.pc_store.modules.warranty.enums.WarrantyPartStatus;
import com.datn.pc_store.modules.warranty.enums.WarrantyPolicyStatus;
import com.datn.pc_store.modules.warranty.enums.WarrantyRequestStatus;
import com.datn.pc_store.modules.warranty.mapper.WarrantyPolicyMapper;
import com.datn.pc_store.modules.warranty.repository.*;
import com.datn.pc_store.modules.warranty.service.AdminWarrantyService;
import com.datn.pc_store.modules.warranty.service.WarrantyPolicyService;
import com.datn.pc_store.modules.warranty.service.WarrantyRequestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminWarrantyServiceFixTest {

    @Mock private WarrantyRepository warranties;
    @Mock private WarrantyPolicyRepository policies;
    @Mock private WarrantyRequestRepository requests;
    @Mock private WarrantyPartRepository parts;
    @Mock private WarrantyPartSerialRepository partSerials;
    @Mock private OrderItemSerialRepository orderItemSerials;
    @Mock private OrderItemRepository orderItems;
    @Mock private OrderRepository orders;
    @Mock private ProductSerialRepository productSerials;
    @Mock private ProductRepository products;
    @Mock private BrandRepository brands;
    @Mock private UserRepository users;
    @Mock private WarrantyRequestService warrantyRequestService;

    @InjectMocks
    private AdminWarrantyService adminWarrantyService;

    @Mock private WarrantyPolicyMapper policyMapper;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks
    private WarrantyPolicyService warrantyPolicyService;

    @Test
    @DisplayName("extendWarranty chặn gia hạn bảo hành đã bị vô hiệu hóa (status=false)")
    void extendWarrantyRejectsVoidedWarranty() {
        Warranty voided = new Warranty();
        voided.setId(1L);
        voided.setStatus(false);
        voided.setEndDate(Instant.now().plusSeconds(86400));

        when(warranties.findById(1L)).thenReturn(Optional.of(voided));

        assertThrows(BadRequestException.class,
                () -> adminWarrantyService.extendWarranty(1L, new WarrantyExtendRequest(30, "test")));
    }

    @Test
    @DisplayName("updateTechnicalHandling chặn chuyển trạng thái ngược từ COMPLETED, kiểm tra technicianId và nhả linh kiện khi REJECTED")
    void updateTechnicalHandlingEnforcesFlowAndReleasesPartsOnReject() {
        WarrantyRequest completedReq = new WarrantyRequest();
        completedReq.setId(10L);
        completedReq.setStatus(WarrantyRequestStatus.COMPLETED);
        when(requests.findById(10L)).thenReturn(Optional.of(completedReq));

        assertThrows(BadRequestException.class,
                () -> adminWarrantyService.updateTechnicalHandling(
                        10L, new TechnicalHandlingUpdateRequest(null, null, "n", null, null, WarrantyRequestStatus.IN_PROGRESS)));

        WarrantyRequest inProgressReq = new WarrantyRequest();
        inProgressReq.setId(11L);
        inProgressReq.setWarrantyId(1L);
        inProgressReq.setStatus(WarrantyRequestStatus.IN_PROGRESS);
        when(requests.findById(11L)).thenReturn(Optional.of(inProgressReq));
        when(users.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> adminWarrantyService.updateTechnicalHandling(
                        11L, new TechnicalHandlingUpdateRequest(999L, null, "n", null, null, WarrantyRequestStatus.IN_PROGRESS)));

        // REJECTED releases RESERVED / PENDING part serials back to IN_STOCK
        WarrantyPart reservedPart = new WarrantyPart();
        reservedPart.setId(50L);
        reservedPart.setWarrantyRequestId(11L);
        reservedPart.setStatus(WarrantyPartStatus.RESERVED);

        WarrantyPartSerial wps = new WarrantyPartSerial();
        wps.setWarrantyPartId(50L);
        wps.setProductSerialId(700L);

        ProductSerial serial = ProductSerial.builder().id(700L).serialNumber("SP-700").status(ProductSerialStatus.USING_BY_TECHNICAL).build();

        when(parts.findByWarrantyRequestId(11L)).thenReturn(List.of(reservedPart));
        when(partSerials.findByWarrantyPartId(50L)).thenReturn(List.of(wps));
        when(productSerials.lockByIds(List.of(700L))).thenReturn(List.of(serial));
        when(requests.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(warranties.findById(1L)).thenReturn(Optional.empty());

        adminWarrantyService.updateTechnicalHandling(
                11L, new TechnicalHandlingUpdateRequest(null, null, "Hết hạn hãng", null, null, WarrantyRequestStatus.REJECTED));

        assertEquals(WarrantyPartStatus.CANCELLED, reservedPart.getStatus());
        assertEquals(ProductSerialStatus.IN_STOCK, serial.getStatus());
    }

    @Test
    @DisplayName("addPartToRequest kiểm tra dữ liệu đầu vào và từ chối khi phiếu đã REJECTED/COMPLETED")
    void addPartToRequestValidatesInputAndRequestState() {
        WarrantyRequest completed = new WarrantyRequest();
        completed.setId(20L);
        completed.setStatus(WarrantyRequestStatus.COMPLETED);
        when(requests.findById(20L)).thenReturn(Optional.of(completed));

        assertThrows(BadRequestException.class,
                () -> adminWarrantyService.addPartToRequest(20L, 5L, 1, 100_000L));

        assertThrows(BadRequestException.class,
                () -> adminWarrantyService.addPartToRequest(20L, null, 1, 100_000L));
    }

    @Test
    @DisplayName("updateSparePartStatus trả serial USING_BY_TECHNICAL về IN_STOCK khi RETURNED")
    void updateSparePartStatusReleasesSerialOnReturn() {
        WarrantyPart part = new WarrantyPart();
        part.setId(60L);
        part.setWarrantyRequestId(30L);
        part.setProductId(8L);
        part.setQuantity(1);
        part.setUnitCost(500_000L);
        part.setStatus(WarrantyPartStatus.RESERVED);

        WarrantyPartSerial wps = new WarrantyPartSerial();
        wps.setWarrantyPartId(60L);
        wps.setProductSerialId(800L);

        ProductSerial serial = ProductSerial.builder().id(800L).serialNumber("SN-800").status(ProductSerialStatus.USING_BY_TECHNICAL).build();

        when(parts.findById(60L)).thenReturn(Optional.of(part));
        when(partSerials.findByWarrantyPartId(60L)).thenReturn(List.of(wps));
        when(productSerials.lockByIds(List.of(800L))).thenReturn(List.of(serial));
        when(parts.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = adminWarrantyService.updateSparePartStatus(60L, WarrantyPartStatus.RETURNED);

        assertEquals(WarrantyPartStatus.RETURNED, response.status());
        assertEquals(ProductSerialStatus.IN_STOCK, serial.getStatus());
    }

    @Test
    @DisplayName("WarrantyPolicyService từ chối sort field không nằm trong whitelist")
    void warrantyPolicyRejectsInvalidSortField() {
        PageRequest invalidPageable = PageRequest.of(0, 10, Sort.by("passwordHash"));
        assertThrows(BadRequestException.class,
                () -> warrantyPolicyService.getAllPolicies(null, WarrantyPolicyStatus.ACTIVE, null, null, invalidPageable));
    }
}
