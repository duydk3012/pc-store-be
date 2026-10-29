package com.datn.pc_store.modules.purchasereturn;

import com.datn.pc_store.modules.auth.security.CustomUserDetails;
import com.datn.pc_store.modules.goodsreceipt.entity.GoodsReceipt;
import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import com.datn.pc_store.modules.goodsreceipt.entity.ReceiptItem;
import com.datn.pc_store.modules.goodsreceipt.enums.GoodsReceiptStatus;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import com.datn.pc_store.modules.goodsreceipt.repository.GoodsReceiptRepository;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.goodsreceipt.repository.ReceiptItemRepository;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.enums.ProductStatus;
import com.datn.pc_store.modules.product.enums.ProductType;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.purchaseorder.entity.PurchaseOrder;
import com.datn.pc_store.modules.purchaseorder.entity.PurchaseOrderItem;
import com.datn.pc_store.modules.purchaseorder.enums.PurchaseOrderStatus;
import com.datn.pc_store.modules.purchaseorder.repository.PurchaseOrderRepository;
import com.datn.pc_store.modules.purchasereturn.dto.PurchaseReturnCreateRequest;
import com.datn.pc_store.modules.purchasereturn.dto.PurchaseReturnResponse;
import com.datn.pc_store.modules.purchasereturn.dto.PurchaseReturnStatusUpdateRequest;
import com.datn.pc_store.modules.purchasereturn.enums.PurchaseReturnStatus;
import com.datn.pc_store.modules.purchasereturn.repository.PurchaseReturnRepository;
import com.datn.pc_store.modules.purchasereturn.service.PurchaseReturnService;
import com.datn.pc_store.modules.suppliers.entity.Supplier;
import com.datn.pc_store.modules.suppliers.enums.SupplierStatus;
import com.datn.pc_store.modules.suppliers.repository.SupplierRepository;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.enums.RoleType;
import com.datn.pc_store.modules.user.enums.UserStatus;
import com.datn.pc_store.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:purchase-return-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.admin.password=",
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
@Transactional
class PurchaseReturnIntegrationTest {

    @Autowired private PurchaseReturnService purchaseReturnService;
    @Autowired private PurchaseReturnRepository purchaseReturnRepository;
    @Autowired private ProductSerialRepository productSerialRepository;
    @Autowired private GoodsReceiptRepository goodsReceiptRepository;
    @Autowired private ReceiptItemRepository receiptItemRepository;
    @Autowired private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

    private User managerUser;
    private Supplier supplier1;
    private Supplier supplier2;
    private Product product;
    private ProductSerial serial1;
    private ProductSerial serial2;

    @BeforeEach
    void setUp() {
        managerUser = userRepository.save(User.builder()
                .username("manager_test")
                .email("manager_test@pcstore.com")
                .passwordHash("secret")
                .fullName("Quản lý Test")
                .role(RoleType.MANAGER)
                .status(UserStatus.ACTIVE)
                .build());

        CustomUserDetails userDetails = new CustomUserDetails(managerUser);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        supplier1 = supplierRepository.save(Supplier.builder()
                .code("SUP-ASUS")
                .name("ASUS Vietnam")
                .email("asus@supplier.com")
                .status(SupplierStatus.ACTIVE)
                .build());

        supplier2 = supplierRepository.save(Supplier.builder()
                .code("SUP-MSI")
                .name("MSI Vietnam")
                .email("msi@supplier.com")
                .status(SupplierStatus.ACTIVE)
                .build());

        product = productRepository.save(Product.builder()
                .code("MB-B760M")
                .name("Mainboard ASUS B760M")
                .productType(ProductType.OTHER)
                .status(ProductStatus.ACTIVE)
                .build());

        PurchaseOrder po1 = PurchaseOrder.builder()
                .code("PO-001")
                .supplierId(supplier1.getId())
                .status(PurchaseOrderStatus.RECEIVED)
                .build();

        PurchaseOrderItem poi1 = PurchaseOrderItem.builder()
                .purchaseOrder(po1)
                .productId(product.getId())
                .quantityOrder(10)
                .unitCost(4000000L)
                .build();
        po1.getItems().add(poi1);
        PurchaseOrder savedPo = purchaseOrderRepository.saveAndFlush(po1);
        Long poiId = savedPo.getItems().get(0).getId();

        GoodsReceipt gr1 = goodsReceiptRepository.save(GoodsReceipt.builder()
                .code("GR-001")
                .purchaseOrderId(savedPo.getId())
                .status(GoodsReceiptStatus.CONFIRMED)
                .receivedAt(Instant.now())
                .build());

        ReceiptItem ri1 = receiptItemRepository.save(ReceiptItem.builder()
                .goodsReceipt(gr1)
                .purchaseOrderItemId(poiId)
                .actualUnitCost(4000000L)
                .quantityReceived(2)
                .quantityAccepted(1)
                .quantityDefective(1)
                .build());

        serial1 = productSerialRepository.save(ProductSerial.builder()
                .productId(product.getId())
                .receiptItem(ri1)
                .serialNumber("SN-TEST-001")
                .status(ProductSerialStatus.DEFECTIVE)
                .build());

        serial2 = productSerialRepository.save(ProductSerial.builder()
                .productId(product.getId())
                .receiptItem(ri1)
                .serialNumber("SN-TEST-002")
                .status(ProductSerialStatus.PENDING_RETURN)
                .build());
    }

    @Test
    void createReturn_Success_WhenValidData_AndUpdatesSerialsToPendingReturn() {
        PurchaseReturnCreateRequest req = PurchaseReturnCreateRequest.builder()
                .code("PR-TEST-001")
                .name("Hoàn hàng lỗi test")
                .supplierId(supplier1.getId())
                .reason("Lỗi nguồn")
                .status(PurchaseReturnStatus.PENDING)
                .serialNumbers(List.of(serial1.getSerialNumber()))
                .build();

        PurchaseReturnResponse res = purchaseReturnService.createReturn(req);

        assertThat(res).isNotNull();
        assertThat(res.getCode()).isEqualTo("PR-TEST-001");
        assertThat(res.getTotalAmount()).isEqualTo(4000000L);
        assertThat(res.getStatus()).isEqualTo(PurchaseReturnStatus.PENDING);
        assertThat(res.getItems()).hasSize(1);
        assertThat(res.getItems().get(0).getSerialNumber()).isEqualTo("SN-TEST-001");

        ProductSerial updatedSerial = productSerialRepository.findById(serial1.getId()).orElseThrow();
        assertThat(updatedSerial.getStatus()).isEqualTo(ProductSerialStatus.PENDING_RETURN);
    }

    @Test
    void createReturn_Fails_WhenSerialBelongsToDifferentSupplier() {
        PurchaseReturnCreateRequest req = PurchaseReturnCreateRequest.builder()
                .code("PR-TEST-WRONG-SUP")
                .supplierId(supplier2.getId()) // supplier2 != supplier1
                .status(PurchaseReturnStatus.PENDING)
                .serialNumbers(List.of(serial1.getSerialNumber()))
                .build();

        assertThatThrownBy(() -> purchaseReturnService.createReturn(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("thuộc Nhà cung cấp");
    }

    @Test
    void updateStatus_CompleteLifecycle_FromPendingToDeliveringToReturned() {
        PurchaseReturnCreateRequest req = PurchaseReturnCreateRequest.builder()
                .code("PR-LIFECYCLE-01")
                .supplierId(supplier1.getId())
                .status(PurchaseReturnStatus.PENDING)
                .serialNumbers(List.of(serial1.getSerialNumber()))
                .build();

        PurchaseReturnResponse res = purchaseReturnService.createReturn(req);
        Long returnId = res.getId();

        // 1. Chuyển sang DELIVERING
        PurchaseReturnResponse deliveringRes = purchaseReturnService.updateStatus(
                returnId,
                PurchaseReturnStatusUpdateRequest.builder()
                        .status(PurchaseReturnStatus.DELIVERING)
                        .note("Đã giao bưu tá")
                        .build()
        );
        assertThat(deliveringRes.getStatus()).isEqualTo(PurchaseReturnStatus.DELIVERING);
        ProductSerial inTransitSerial = productSerialRepository.findById(serial1.getId()).orElseThrow();
        assertThat(inTransitSerial.getStatus()).isEqualTo(ProductSerialStatus.RETURNING);

        // 2. Chuyển sang RETURNED
        PurchaseReturnResponse returnedRes = purchaseReturnService.updateStatus(
                returnId,
                PurchaseReturnStatusUpdateRequest.builder()
                        .status(PurchaseReturnStatus.RETURNED)
                        .note("NCC đã nhận và ký biên bản")
                        .build()
        );
        assertThat(returnedRes.getStatus()).isEqualTo(PurchaseReturnStatus.RETURNED);
        ProductSerial completedSerial = productSerialRepository.findById(serial1.getId()).orElseThrow();
        assertThat(completedSerial.getStatus()).isEqualTo(ProductSerialStatus.RETURNED);
        assertThat(completedSerial.isTerminated()).isTrue();
    }

    @Test
    void updateStatus_Cancelled_RollsBackSerialsToDefective() {
        PurchaseReturnCreateRequest req = PurchaseReturnCreateRequest.builder()
                .code("PR-CANCEL-01")
                .supplierId(supplier1.getId())
                .status(PurchaseReturnStatus.PENDING)
                .serialNumbers(List.of(serial1.getSerialNumber()))
                .build();

        PurchaseReturnResponse res = purchaseReturnService.createReturn(req);
        Long returnId = res.getId();

        PurchaseReturnResponse cancelledRes = purchaseReturnService.updateStatus(
                returnId,
                PurchaseReturnStatusUpdateRequest.builder()
                        .status(PurchaseReturnStatus.CANCELLED)
                        .note("Hủy trả hàng do giữ lại sửa chữa")
                        .build()
        );

        assertThat(cancelledRes.getStatus()).isEqualTo(PurchaseReturnStatus.CANCELLED);
        ProductSerial rolledBackSerial = productSerialRepository.findById(serial1.getId()).orElseThrow();
        assertThat(rolledBackSerial.getStatus()).isEqualTo(ProductSerialStatus.DEFECTIVE);
        assertThat(rolledBackSerial.isTerminated()).isFalse();
    }

    @Test
    void getAvailableSerials_ReturnsEligibleSerialsForSupplier() {
        var list = purchaseReturnService.getAvailableSerialsForReturn(supplier1.getId(), null, null);

        assertThat(list).isNotEmpty();
        assertThat(list).extracting("serialNumber").contains("SN-TEST-001", "SN-TEST-002");
    }

    @Test
    void validateSerialForReturn_ReturnsDetailsWhenValid() {
        var detail = purchaseReturnService.validateSerialForReturn(supplier1.getId(), "SN-TEST-001");

        assertThat(detail).isNotNull();
        assertThat(detail.getSerialNumber()).isEqualTo("SN-TEST-001");
        assertThat(detail.getUnitCost()).isEqualTo(4000000L);
        assertThat(detail.getReceiptCode()).isEqualTo("GR-001");
    }
}
