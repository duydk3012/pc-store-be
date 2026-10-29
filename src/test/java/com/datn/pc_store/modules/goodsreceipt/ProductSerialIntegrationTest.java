package com.datn.pc_store.modules.goodsreceipt;

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
import com.datn.pc_store.modules.suppliers.entity.Supplier;
import com.datn.pc_store.modules.suppliers.enums.SupplierStatus;
import com.datn.pc_store.modules.suppliers.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:product-serial-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.admin.password=",
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
@Transactional
class ProductSerialIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductSerialRepository productSerialRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired private GoodsReceiptRepository goodsReceiptRepository;
    @Autowired private ReceiptItemRepository receiptItemRepository;

    private MockMvc mvc;
    private Product product;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        product = productRepository.saveAndFlush(Product.builder()
                .name("CPU Intel i9")
                .code("CPU-I9-9900")
                .slug("cpu-intel-i9")
                .status(ProductStatus.ACTIVE)
                .productType(ProductType.OTHER)
                .build());
    }

    @Test
    void securityRolesForProductSerials() throws Exception {
        mvc.perform(get("/api/product-serials/by-product/" + product.getId()).with(anonymous()))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/product-serials/by-product/" + product.getId()).with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/product-serials/by-product/" + product.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/product-serials/by-product/" + product.getId()).with(user("tech").roles("STAFF_TECH")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getActiveSerialsByProductWithPaginationAndStatusFilter() throws Exception {
        // Tạo 3 serial active: 2 cái IN_STOCK, 1 cái GOOD_CONDITION
        productSerialRepository.saveAndFlush(ProductSerial.builder()
                .productId(product.getId())
                .serialNumber("SN-ACTIVE-01")
                .status(ProductSerialStatus.IN_STOCK)
                .isTerminated(false)
                .build());

        productSerialRepository.saveAndFlush(ProductSerial.builder()
                .productId(product.getId())
                .serialNumber("SN-ACTIVE-02")
                .status(ProductSerialStatus.IN_STOCK)
                .isTerminated(false)
                .build());

        productSerialRepository.saveAndFlush(ProductSerial.builder()
                .productId(product.getId())
                .serialNumber("SN-ACTIVE-03")
                .status(ProductSerialStatus.GOOD_CONDITION)
                .isTerminated(false)
                .build());

        // Tạo 1 serial đã terminated (không được trả về)
        productSerialRepository.saveAndFlush(ProductSerial.builder()
                .productId(product.getId())
                .serialNumber("SN-TERMINATED")
                .status(ProductSerialStatus.RETURNED)
                .isTerminated(true)
                .build());

        // 1. Lấy tất cả serial còn hiệu lực (không lọc status) -> trả về 3
        mvc.perform(get("/api/product-serials/by-product/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content", hasSize(3)));

        // 2. Lọc theo status = IN_STOCK -> trả về 2
        mvc.perform(get("/api/product-serials/by-product/" + product.getId()).param("status", "IN_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].status").value("IN_STOCK"))
                .andExpect(jsonPath("$.content[1].status").value("IN_STOCK"));

        // 3. Phân trang: size = 1, page = 0
        mvc.perform(get("/api/product-serials/by-product/" + product.getId())
                        .param("size", "1")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.pageSize").value(1))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.content", hasSize(1)));

        // 4. Lọc theo sản phẩm không tồn tại -> 404
        mvc.perform(get("/api/product-serials/by-product/999999"))
                .andExpect(status().isNotFound());

        // 5. Sort không hợp lệ -> 400
        mvc.perform(get("/api/product-serials/by-product/" + product.getId()).param("sort", "invalidField,desc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchSerialsComprehensiveEndpoint() throws Exception {
        Supplier sup = supplierRepository.save(Supplier.builder()
                .code("SUP-SERIAL-01")
                .name("Supplier Test Serials")
                .email("sup_serials@test.com")
                .status(SupplierStatus.ACTIVE)
                .build());

        PurchaseOrder po = PurchaseOrder.builder()
                .code("PO-SERIAL-01")
                .supplierId(sup.getId())
                .status(PurchaseOrderStatus.RECEIVED)
                .build();
        PurchaseOrderItem poi = PurchaseOrderItem.builder()
                .purchaseOrder(po)
                .productId(product.getId())
                .quantityOrder(5)
                .unitCost(1000000L)
                .build();
        po.getItems().add(poi);
        PurchaseOrder savedPo = purchaseOrderRepository.saveAndFlush(po);
        Long poiId = savedPo.getItems().get(0).getId();

        GoodsReceipt gr = goodsReceiptRepository.save(GoodsReceipt.builder()
                .code("GR-SERIAL-01")
                .purchaseOrderId(savedPo.getId())
                .status(GoodsReceiptStatus.CONFIRMED)
                .receivedAt(Instant.now())
                .build());

        ReceiptItem ri = receiptItemRepository.save(ReceiptItem.builder()
                .goodsReceipt(gr)
                .purchaseOrderItemId(poiId)
                .actualUnitCost(1000000L)
                .quantityReceived(2)
                .quantityAccepted(2)
                .build());

        productSerialRepository.saveAndFlush(ProductSerial.builder()
                .productId(product.getId())
                .receiptItem(ri)
                .serialNumber("SN-SUP-MATCH-01")
                .status(ProductSerialStatus.IN_STOCK)
                .isTerminated(false)
                .build());

        productSerialRepository.saveAndFlush(ProductSerial.builder()
                .productId(product.getId())
                .receiptItem(ri)
                .serialNumber("SN-SUP-MATCH-02")
                .status(ProductSerialStatus.DEFECTIVE)
                .isTerminated(false)
                .build());

        // 1. Search by supplierId only
        mvc.perform(get("/api/product-serials").param("supplierId", sup.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // 2. Search by supplierId and status = DEFECTIVE
        mvc.perform(get("/api/product-serials")
                        .param("supplierId", sup.getId().toString())
                        .param("status", "DEFECTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].serialNumber").value("SN-SUP-MATCH-02"));

        // 3. Search by keyword
        mvc.perform(get("/api/product-serials")
                        .param("supplierId", sup.getId().toString())
                        .param("keyword", "MATCH-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].serialNumber").value("SN-SUP-MATCH-01"));
    }
}
