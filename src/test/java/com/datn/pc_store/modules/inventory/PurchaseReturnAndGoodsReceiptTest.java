package com.datn.pc_store.modules.inventory;

import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.modules.auth.security.CustomUserDetails;
import com.datn.pc_store.modules.delivery.service.DeliveryService;
import com.datn.pc_store.modules.goodsreceipt.dto.GoodsReceiptResponse;
import com.datn.pc_store.modules.goodsreceipt.entity.GoodsReceipt;
import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import com.datn.pc_store.modules.goodsreceipt.entity.ReceiptItem;
import com.datn.pc_store.modules.goodsreceipt.enums.GoodsReceiptStatus;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import com.datn.pc_store.modules.goodsreceipt.mapper.GoodsReceiptMapper;
import com.datn.pc_store.modules.goodsreceipt.repository.GoodsReceiptRepository;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.goodsreceipt.service.GoodsReceiptService;
import com.datn.pc_store.modules.goodsreceipt.service.ProductSerialService;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.purchaseorder.entity.PurchaseOrder;
import com.datn.pc_store.modules.purchaseorder.entity.PurchaseOrderItem;
import com.datn.pc_store.modules.purchaseorder.enums.PurchaseOrderStatus;
import com.datn.pc_store.modules.purchaseorder.repository.PurchaseOrderRepository;
import com.datn.pc_store.modules.purchasereturn.dto.PurchaseReturnItemResponse;
import com.datn.pc_store.modules.purchasereturn.dto.PurchaseReturnResponse;
import com.datn.pc_store.modules.purchasereturn.dto.PurchaseReturnUpdateRequest;
import com.datn.pc_store.modules.purchasereturn.entity.PurchaseReturn;
import com.datn.pc_store.modules.purchasereturn.entity.PurchaseReturnItem;
import com.datn.pc_store.modules.purchasereturn.enums.PurchaseReturnStatus;
import com.datn.pc_store.modules.purchasereturn.mapper.PurchaseReturnMapper;
import com.datn.pc_store.modules.purchasereturn.repository.PurchaseReturnItemRepository;
import com.datn.pc_store.modules.purchasereturn.repository.PurchaseReturnRepository;
import com.datn.pc_store.modules.purchasereturn.service.PurchaseReturnService;
import com.datn.pc_store.modules.suppliers.entity.Supplier;
import com.datn.pc_store.modules.suppliers.repository.SupplierRepository;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.enums.RoleType;
import com.datn.pc_store.modules.user.enums.UserStatus;
import com.datn.pc_store.modules.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseReturnAndGoodsReceiptTest {

    @Mock
    private GoodsReceiptRepository goodsReceiptRepository;
    @Mock
    private ProductSerialRepository productSerialRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private GoodsReceiptMapper goodsReceiptMapper;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private GoodsReceiptService goodsReceiptService;

    @InjectMocks
    private ProductSerialService productSerialService;

    @Mock
    private PurchaseReturnRepository purchaseReturnRepository;
    @Mock
    private PurchaseReturnItemRepository purchaseReturnItemRepository;
    @Mock
    private PurchaseReturnMapper purchaseReturnMapper;
    @Mock
    private DeliveryService deliveryService;

    @InjectMocks
    private PurchaseReturnService purchaseReturnService;

    @BeforeEach
    void setUpAuth() {
        User admin = User.builder().id(1L).username("admin").role(RoleType.ADMIN).status(UserStatus.ACTIVE).build();
        CustomUserDetails principal = new CustomUserDetails(admin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GoodsReceiptService.updateStatus(CONFIRMED) khóa PurchaseOrder bằng findForUpdate và chặn vượt SL đặt")
    void confirmReceiptLocksPoAndRejectsOverReceive() {
        PurchaseOrderItem poItem = PurchaseOrderItem.builder().id(11L).productId(5L).quantityOrder(2).quantityReceived(1).unitCost(1_000_000L).subtotal(2_000_000L).build();
        PurchaseOrder po = PurchaseOrder.builder().id(20L).code("PO-1").status(PurchaseOrderStatus.APPROVED).items(List.of(poItem)).build();
        poItem.setPurchaseOrder(po);

        ReceiptItem receiptItem = ReceiptItem.builder().id(30L).purchaseOrderItemId(11L).quantityReceived(2).quantityAccepted(2).quantityDefective(0).build();
        GoodsReceipt receipt = GoodsReceipt.builder().id(40L).code("GR-1").purchaseOrderId(20L).status(GoodsReceiptStatus.PENDING).items(List.of(receiptItem)).build();
        receiptItem.setGoodsReceipt(receipt);

        when(goodsReceiptRepository.findForUpdate(40L)).thenReturn(Optional.of(receipt));
        when(purchaseOrderRepository.findForUpdate(20L)).thenReturn(Optional.of(po));

        assertThrows(BadRequestException.class,
                () -> goodsReceiptService.updateStatus(40L, GoodsReceiptStatus.CONFIRMED));
        verify(purchaseOrderRepository).findForUpdate(20L);
    }

    @Test
    @DisplayName("ProductSerialService chặn đổi trạng thái serial đã terminate hoặc thuộc luồng bán hàng/bảo hành")
    void productSerialBlocksTerminatedOrWorkflowManagedSerials() {
        ProductSerial terminated = ProductSerial.builder().id(1L).serialNumber("SN-T").status(ProductSerialStatus.IN_STOCK).isTerminated(true).build();
        when(productSerialRepository.findById(1L)).thenReturn(Optional.of(terminated));

        assertThrows(BadRequestException.class,
                () -> productSerialService.updateStatus(1L, ProductSerialStatus.DEFECTIVE));

        ProductSerial sold = ProductSerial.builder().id(2L).serialNumber("SN-S").status(ProductSerialStatus.SOLD).isTerminated(false).build();
        when(productSerialRepository.findById(2L)).thenReturn(Optional.of(sold));

        assertThrows(BadRequestException.class,
                () -> productSerialService.updateStatus(2L, ProductSerialStatus.IN_STOCK));
    }

    @Test
    @DisplayName("PurchaseReturnService.deleteReturn khôi phục serial từ PENDING_RETURN về DEFECTIVE")
    void deleteReturnRestoresSerialsToDefective() {
        ProductSerial ps = ProductSerial.builder().id(501L).serialNumber("SN-RET").status(ProductSerialStatus.PENDING_RETURN).build();
        PurchaseReturn pr = PurchaseReturn.builder().id(80L).code("PR-1").status(PurchaseReturnStatus.PENDING).items(new ArrayList<>()).build();
        PurchaseReturnItem item = PurchaseReturnItem.builder().id(1L).purchaseReturn(pr).productSerial(ps).build();
        pr.getItems().add(item);

        when(purchaseReturnRepository.findForUpdate(80L)).thenReturn(Optional.of(pr));

        purchaseReturnService.deleteReturn(80L);

        assertEquals(ProductSerialStatus.DEFECTIVE, ps.getStatus());
        verify(purchaseReturnRepository).delete(pr);
    }

    @Test
    @DisplayName("PurchaseReturnService.updateReturn trả serial bị bỏ về DEFECTIVE và khóa serial mới sang PENDING_RETURN")
    void updateReturnReleasesRemovedAndLocksAddedSerials() {
        GoodsReceipt gr = GoodsReceipt.builder().id(10L).purchaseOrderId(20L).build();
        ReceiptItem receiptItem = ReceiptItem.builder().id(11L).goodsReceipt(gr).actualUnitCost(1_000_000L).build();
        PurchaseOrder po = PurchaseOrder.builder().id(20L).supplierId(3L).build();

        ProductSerial oldSerial = ProductSerial.builder().id(601L).productId(9L).serialNumber("SN-OLD").status(ProductSerialStatus.PENDING_RETURN).receiptItem(receiptItem).build();
        ProductSerial newSerial = ProductSerial.builder().id(602L).productId(9L).serialNumber("SN-NEW").status(ProductSerialStatus.DEFECTIVE).receiptItem(receiptItem).build();

        PurchaseReturn pr = PurchaseReturn.builder()
                .id(81L).code("PR-81").supplierId(3L).status(PurchaseReturnStatus.PENDING)
                .items(new ArrayList<>())
                .build();
        pr.getItems().add(PurchaseReturnItem.builder().id(1L).purchaseReturn(pr).productSerial(oldSerial).unitCost(1_000_000L).build());

        PurchaseReturnUpdateRequest req = PurchaseReturnUpdateRequest.builder()
                .code("PR-81")
                .serialNumbers(List.of("SN-NEW"))
                .build();

        when(purchaseReturnRepository.findForUpdate(81L)).thenReturn(Optional.of(pr));
        when(purchaseReturnRepository.countByCode("PR-81", 81L)).thenReturn(0L);
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(Supplier.builder().id(3L).name("NCC A").build()));
        when(purchaseReturnRepository.findActiveSerialsWithReceiptBySerialNumber("SN-NEW")).thenReturn(List.of(newSerial));
        when(purchaseReturnItemRepository.findSerialIdsInActiveReturns(any(), any(), eq(81L))).thenReturn(Set.of());
        when(purchaseOrderRepository.findAllById(any())).thenReturn(List.of(po));
        when(purchaseReturnRepository.saveAndFlush(any(PurchaseReturn.class))).thenAnswer(inv -> inv.getArgument(0));
        when(purchaseReturnMapper.toResponse(any())).thenReturn(PurchaseReturnResponse.builder().id(81L).build());
        when(purchaseReturnMapper.toItemResponse(any())).thenReturn(new PurchaseReturnItemResponse());

        purchaseReturnService.updateReturn(81L, req);

        assertEquals(ProductSerialStatus.DEFECTIVE, oldSerial.getStatus());
        assertEquals(ProductSerialStatus.PENDING_RETURN, newSerial.getStatus());
    }
}
