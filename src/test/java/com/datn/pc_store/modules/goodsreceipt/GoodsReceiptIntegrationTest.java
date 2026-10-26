package com.datn.pc_store.modules.goodsreceipt;

import com.datn.pc_store.modules.goodsreceipt.dto.*;
import com.datn.pc_store.modules.goodsreceipt.enums.GoodsReceiptStatus;
import com.datn.pc_store.modules.goodsreceipt.enums.ProductSerialStatus;
import com.datn.pc_store.modules.goodsreceipt.repository.GoodsReceiptRepository;
import com.datn.pc_store.modules.goodsreceipt.repository.ProductSerialRepository;
import com.datn.pc_store.modules.goodsreceipt.repository.ReceiptItemRepository;
import com.datn.pc_store.modules.goodsreceipt.service.GoodsReceiptService;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.purchaseorder.dto.PurchaseOrderCreateRequest;
import com.datn.pc_store.modules.purchaseorder.dto.PurchaseOrderItemRequest;
import com.datn.pc_store.modules.purchaseorder.enums.PurchaseOrderStatus;
import com.datn.pc_store.modules.purchaseorder.service.PurchaseOrderService;
import com.datn.pc_store.modules.suppliers.entity.Supplier;
import com.datn.pc_store.modules.suppliers.repository.SupplierRepository;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.enums.RoleType;
import com.datn.pc_store.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:goods-receipt-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.admin.password=",
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
@Transactional
@Sql("/purchaseorder/receipt-schema.sql")
class GoodsReceiptIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private GoodsReceiptService goodsReceiptService;
    @Autowired private GoodsReceiptRepository goodsReceiptRepository;
    @Autowired private ReceiptItemRepository receiptItemRepository;
    @Autowired private ProductSerialRepository productSerialRepository;
    @Autowired private PurchaseOrderService purchaseOrderService;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

    private MockMvc mvc;
    private Supplier supplier;
    private Product product1;
    private Product product2;
    private Long approvedPoId;
    private Long poiId1;
    private Long poiId2;
    private User warehouseUser1;
    private User warehouseUser2;
    private User managerUser;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        warehouseUser1 = userRepository.saveAndFlush(User.builder()
                .username("wh1").email("wh1@test.com").passwordHash("hash").role(RoleType.STAFF_WAREHOUSE).build());
        warehouseUser2 = userRepository.saveAndFlush(User.builder()
                .username("wh2").email("wh2@test.com").passwordHash("hash").role(RoleType.STAFF_WAREHOUSE).build());
        managerUser = userRepository.saveAndFlush(User.builder()
                .username("mgr").email("mgr@test.com").passwordHash("hash").role(RoleType.MANAGER).build());

        supplier = supplierRepository.saveAndFlush(Supplier.builder()
                .code("NCC-GR-01").name("NCC Test").email("ncc@test.com").build());
        product1 = productRepository.saveAndFlush(Product.builder()
                .name("CPU Intel i9").code("CPU-01").slug("cpu-i9").build());
        product2 = productRepository.saveAndFlush(Product.builder()
                .name("VGA RTX 4090").code("VGA-01").slug("vga-4090").build());

        var poResponse = purchaseOrderService.createOrder(PurchaseOrderCreateRequest.builder()
                .code("PO-TEST-001")
                .supplierId(supplier.getId())
                .orderDate(Instant.now())
                .items(List.of(
                        PurchaseOrderItemRequest.builder().productId(product1.getId()).quantityOrder(5).unitCost(10000000L).build(),
                        PurchaseOrderItemRequest.builder().productId(product2.getId()).quantityOrder(2).unitCost(40000000L).build()
                ))
                .build());

        purchaseOrderService.updateStatus(poResponse.getId(), PurchaseOrderStatus.PENDING);
        purchaseOrderService.updateStatus(poResponse.getId(), PurchaseOrderStatus.APPROVED);

        approvedPoId = poResponse.getId();
        var reloadedPo = purchaseOrderService.getOrderById(approvedPoId);
        poiId1 = reloadedPo.getItems().get(0).getId();
        poiId2 = reloadedPo.getItems().get(1).getId();
    }

    @Test
    void createReceiptDefaultPendingAndCalculatesQuantitiesFromSerials() throws Exception {
        String body = """
                {
                    "code": "GR-001",
                    "name": "Phiếu nhập 01",
                    "purchaseOrderId": %d,
                    "items": [
                        {
                            "purchaseOrderItemId": %d,
                            "actualUnitCost": 9800000,
                            "productSerials": [
                                {"serialNumber": "CPU-SN-001", "status": "GOOD_CONDITION"},
                                {"serialNumber": "CPU-SN-002", "status": "GOOD_CONDITION"},
                                {"serialNumber": "CPU-SN-003", "status": "DEFECTIVE"}
                            ]
                        }
                    ]
                }
                """.formatted(approvedPoId, poiId1);

        mvc.perform(post("/api/goods-receipts").with(user("wh1").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("GR-001"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].actualUnitCost").value(9800000))
                .andExpect(jsonPath("$.items[0].quantityReceived").value(3))
                .andExpect(jsonPath("$.items[0].quantityAccepted").value(2))
                .andExpect(jsonPath("$.items[0].quantityDefective").value(1))
                .andExpect(jsonPath("$.items[0].productSerials", hasSize(3)))
                .andExpect(jsonPath("$.items[0].productSerials[0].isTerminated").value(false));

        var serial1 = productSerialRepository.findBySerialNumber("CPU-SN-001").orElseThrow();
        assertThat(serial1.getStatus()).isEqualTo(ProductSerialStatus.GOOD_CONDITION);
        assertThat(serial1.isTerminated()).isFalse();

        var serial3 = productSerialRepository.findBySerialNumber("CPU-SN-003").orElseThrow();
        assertThat(serial3.getStatus()).isEqualTo(ProductSerialStatus.DEFECTIVE);
        assertThat(serial3.isTerminated()).isFalse();
    }

    @Test
    void cannotTransitionFromPendingToDraft() throws Exception {
        var receipt = createReceiptForUser("GR-NO-DRAFT", "wh1");

        // Không cho phép chuyển từ PENDING sang DRAFT
        mvc.perform(patch("/api/goods-receipts/" + receipt.getId() + "/status")
                        .with(user("wh1").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DRAFT\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void onlyCreatorCanCancelPendingReceipt() throws Exception {
        var receipt = createReceiptForUser("GR-CANCEL-TEST", "wh1");

        // Người khác (wh2) cố hủy -> bị cấm 403 Forbidden
        mvc.perform(patch("/api/goods-receipts/" + receipt.getId() + "/status")
                        .with(user("wh2").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CANCELLED\"}"))
                .andExpect(status().isForbidden());

        // Người tạo (wh1) tự hủy -> thành công
        mvc.perform(patch("/api/goods-receipts/" + receipt.getId() + "/status")
                        .with(user("wh1").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        var sn = productSerialRepository.findBySerialNumber("SN-GR-CANCEL-TEST").orElseThrow();
        assertThat(sn.isTerminated()).isTrue();
    }

    @Test
    void onlyManagerOrAdminCanConfirmOrRejectAndCanSelfApprove() throws Exception {
        var receipt = createReceiptForUser("GR-CONFIRM-TEST", "wh1");

        // Nhân viên kho (wh1) cố duyệt/từ chối -> bị cấm 403 Forbidden
        mvc.perform(patch("/api/goods-receipts/" + receipt.getId() + "/status")
                        .with(user("wh1").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isForbidden());

        // Nhân viên kho khác (wh2) cố duyệt -> cũng bị cấm 403 Forbidden
        mvc.perform(patch("/api/goods-receipts/" + receipt.getId() + "/status")
                        .with(user("wh2").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isForbidden());

        // Manager duyệt phiếu do wh1 tạo -> thành công
        mvc.perform(patch("/api/goods-receipts/" + receipt.getId() + "/status")
                        .with(user("mgr").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        var sn = productSerialRepository.findBySerialNumber("SN-GR-CONFIRM-TEST").orElseThrow();
        assertThat(sn.getStatus()).isEqualTo(ProductSerialStatus.IN_STOCK);
        assertThat(sn.isTerminated()).isFalse();

        // Manager tự tạo phiếu và tự duyệt -> thành công
        var mgrReceipt = createReceiptForUser("GR-MGR-SELF", "mgr");
        mvc.perform(patch("/api/goods-receipts/" + mgrReceipt.getId() + "/status")
                        .with(user("mgr").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void warehouseStaffOnlySeesOwnReceipts() throws Exception {
        createReceiptForUser("GR-WH1-A", "wh1");
        createReceiptForUser("GR-WH2-A", "wh2");

        // wh1 xem danh sách -> chỉ thấy 1 bản ghi của wh1
        mvc.perform(get("/api/goods-receipts").with(user("wh1").roles("STAFF_WAREHOUSE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].code").value("GR-WH1-A"));

        // wh2 xem danh sách -> chỉ thấy 1 bản ghi của wh2
        mvc.perform(get("/api/goods-receipts").with(user("wh2").roles("STAFF_WAREHOUSE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].code").value("GR-WH2-A"));
    }

    @Test
    void managerSeesPendingConfirmedRejectedAndOwnDraftsAndOwnCancelledButNotOthersCancelledOrDrafts() throws Exception {
        var r1 = createReceiptForUser("GR-MGR-PENDING", "wh1");
        var r2 = createReceiptForUser("GR-MGR-CONFIRM", "wh2");
        var r3 = createReceiptForUser("GR-WH1-CANCEL", "wh1");
        var r4 = createReceiptForUser("GR-MGR-CANCEL", "mgr");

        // wh1 tạo phiếu DRAFT
        createDraftReceiptForUser("GR-WH1-DRAFT", "wh1");

        // manager tự tạo phiếu DRAFT
        createDraftReceiptForUser("GR-MGR-DRAFT", "mgr");

        // Duyệt r2 sang CONFIRMED
        mvc.perform(patch("/api/goods-receipts/" + r2.getId() + "/status")
                        .with(user("mgr").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isOk());

        // wh1 hủy r3 sang CANCELLED
        mvc.perform(patch("/api/goods-receipts/" + r3.getId() + "/status")
                        .with(user("wh1").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CANCELLED\"}"))
                .andExpect(status().isOk());

        // mgr tự hủy r4 sang CANCELLED
        mvc.perform(patch("/api/goods-receipts/" + r4.getId() + "/status")
                        .with(user("mgr").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CANCELLED\"}"))
                .andExpect(status().isOk());

        // Manager xem danh sách: thấy PENDING (r1), CONFIRMED (r2), DRAFT của manager (GR-MGR-DRAFT)
        // và CANCELLED do chính manager tự hủy (GR-MGR-CANCEL). Tổng 4 bản ghi.
        // KHÔNG thấy CANCELLED của wh1 (GR-WH1-CANCEL) và KHÔNG thấy DRAFT của wh1 (GR-WH1-DRAFT).
        mvc.perform(get("/api/goods-receipts").with(user("mgr").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[*].code", not(hasItem("GR-WH1-CANCEL"))))
                .andExpect(jsonPath("$.content[*].code", not(hasItem("GR-WH1-DRAFT"))))
                .andExpect(jsonPath("$.content[*].code", hasItems("GR-MGR-PENDING", "GR-MGR-CONFIRM", "GR-MGR-DRAFT", "GR-MGR-CANCEL")));

        // Manager lọc status=CANCELLED -> thấy phiếu CANCELLED do chính mình tạo
        mvc.perform(get("/api/goods-receipts").with(user("mgr").roles("MANAGER"))
                        .param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].code").value("GR-MGR-CANCEL"));
    }

    @Test
    void cannotTransitionFromCancelledToConfirmedOrRejected() throws Exception {
        var receipt = createReceiptForUser("GR-CANCELLED-EDGE", "mgr");

        // Manager hủy phiếu
        mvc.perform(patch("/api/goods-receipts/" + receipt.getId() + "/status")
                        .with(user("mgr").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Cố chuyển từ CANCELLED sang CONFIRMED -> 409 Conflict với thông báo lỗi
        mvc.perform(patch("/api/goods-receipts/" + receipt.getId() + "/status")
                        .with(user("mgr").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("phiếu nhập kho đã được hủy, không thực hiện được thao tác này"));

        // Cố chuyển từ CANCELLED sang REJECTED -> 409 Conflict với thông báo lỗi
        mvc.perform(patch("/api/goods-receipts/" + receipt.getId() + "/status")
                        .with(user("mgr").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"REJECTED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("phiếu nhập kho đã được hủy, không thực hiện được thao tác này"));
    }

    @Test
    void onlyDraftReceiptCanBeHardDeletedAndNonDraftCannotBeDeleted() throws Exception {
        // Tạo phiếu DRAFT
        String draftBody = """
                {
                    "code": "GR-DRAFT-DEL",
                    "name": "Nháp cần xóa",
                    "purchaseOrderId": %d,
                    "status": "DRAFT",
                    "items": [
                        {
                            "purchaseOrderItemId": %d,
                            "actualUnitCost": 10000000,
                            "productSerials": [
                                {"serialNumber": "SN-HARD-DEL-1", "status": "GOOD_CONDITION"}
                            ]
                        }
                    ]
                }
                """.formatted(approvedPoId, poiId1);

        String createRes = mvc.perform(post("/api/goods-receipts").with(user("wh1").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON).content(draftBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long draftId = ((Number) com.jayway.jsonpath.JsonPath.read(createRes, "$.id")).longValue();

        assertThat(goodsReceiptRepository.findById(draftId)).isPresent();
        assertThat(productSerialRepository.findBySerialNumber("SN-HARD-DEL-1")).isPresent();

        // Xóa phiếu ở trạng thái DRAFT -> Xóa cứng thành công
        mvc.perform(delete("/api/goods-receipts/" + draftId).with(user("wh1").roles("STAFF_WAREHOUSE")))
                .andExpect(status().isNoContent());

        // Kiểm tra đã xóa hẳn khỏi database (hard delete)
        assertThat(goodsReceiptRepository.findById(draftId)).isEmpty();
        assertThat(receiptItemRepository.findByGoodsReceiptId(draftId)).isEmpty();
        assertThat(productSerialRepository.findBySerialNumber("SN-HARD-DEL-1")).isEmpty();

        // Thử xóa phiếu ở trạng thái PENDING -> 409 Conflict
        var pendingReceipt = createReceiptForUser("GR-PENDING-NO-DEL", "wh1");
        mvc.perform(delete("/api/goods-receipts/" + pendingReceipt.getId()).with(user("wh1").roles("STAFF_WAREHOUSE")))
                .andExpect(status().isConflict());
    }

    @Test
    void allowsReusingSerialNumberWhenTerminatedButRejectsIfActive() throws Exception {
        // Tạo phiếu 1 có serial REUSE-SN
        String body1 = """
                {
                    "code": "GR-REUSE-1",
                    "purchaseOrderId": %d,
                    "status": "PENDING",
                    "items": [
                        {
                            "purchaseOrderItemId": %d,
                            "actualUnitCost": 10000000,
                            "productSerials": [
                                {"serialNumber": "REUSE-SN", "status": "GOOD_CONDITION"}
                            ]
                        }
                    ]
                }
                """.formatted(approvedPoId, poiId1);

        String res1 = mvc.perform(post("/api/goods-receipts").with(user("wh1").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body1))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long gr1Id = ((Number) com.jayway.jsonpath.JsonPath.read(res1, "$.id")).longValue();

        // Trong khi phiếu 1 đang active (is_terminated = false), tạo phiếu 2 có cùng serial REUSE-SN -> bị từ chối 409 Conflict
        String body2 = """
                {
                    "code": "GR-REUSE-2",
                    "purchaseOrderId": %d,
                    "status": "PENDING",
                    "items": [
                        {
                            "purchaseOrderItemId": %d,
                            "actualUnitCost": 10000000,
                            "productSerials": [
                                {"serialNumber": "REUSE-SN", "status": "GOOD_CONDITION"}
                            ]
                        }
                    ]
                }
                """.formatted(approvedPoId, poiId1);

        mvc.perform(post("/api/goods-receipts").with(user("wh1").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body2))
                .andExpect(status().isConflict());

        // Hủy phiếu 1 (CANCELLED) -> serial REUSE-SN chuyển sang is_terminated = true
        mvc.perform(patch("/api/goods-receipts/" + gr1Id + "/status")
                        .with(user("wh1").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CANCELLED\"}"))
                .andExpect(status().isOk());

        // Bây giờ tạo phiếu 2 với cùng serial REUSE-SN -> CHO PHÉP TẠO MỚI thành công!
        mvc.perform(post("/api/goods-receipts").with(user("wh1").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("GR-REUSE-2"))
                .andExpect(jsonPath("$.items[0].productSerials[0].serialNumber").value("REUSE-SN"))
                .andExpect(jsonPath("$.items[0].productSerials[0].isTerminated").value(false));

        // Kiểm tra database có 2 bản ghi mang cùng serialNumber "REUSE-SN" (1 bản ghi is_terminated = true, 1 bản ghi is_terminated = false)
        var allWithSerial = productSerialRepository.findAllBySerialNumber("REUSE-SN");
        assertThat(allWithSerial).hasSize(2);
        assertThat(allWithSerial).anyMatch(ps -> ps.isTerminated());
        assertThat(allWithSerial).anyMatch(ps -> !ps.isTerminated());
    }

    @Test
    void getOrderItemsForReceipt_returnsPoItemsWithRemainingQuantity() throws Exception {
        mvc.perform(get("/api/goods-receipts/purchase-orders/" + approvedPoId + "/order-items")
                        .with(user("wh1").roles("STAFF_WAREHOUSE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].productId").value(product1.getId()))
                .andExpect(jsonPath("$[0].productName").value("CPU Intel i9"))
                .andExpect(jsonPath("$[0].quantityOrder").value(5))
                .andExpect(jsonPath("$[0].quantityReceived").value(0))
                .andExpect(jsonPath("$[0].remainingQuantity").value(5))
                .andExpect(jsonPath("$[0].unitCost").value(10000000));
    }

    @Test
    void createReceiptPendingWithoutItemsOrSerials_failsWithWarning() throws Exception {
        // Thử tạo phiếu PENDING không có items
        String bodyNoItems = """
                {
                    "code": "GR-EMPTY-ITEMS",
                    "purchaseOrderId": %d,
                    "status": "PENDING",
                    "items": []
                }
                """.formatted(approvedPoId);

        mvc.perform(post("/api/goods-receipts").with(user("wh1").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON).content(bodyNoItems))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Chưa thêm danh sách mặt hàng thực nhận (receiptItem) và serial sản phẩm (productSerial)")));

        // Thử tạo phiếu PENDING có item nhưng không có serials
        String bodyNoSerials = """
                {
                    "code": "GR-EMPTY-SERIALS",
                    "purchaseOrderId": %d,
                    "status": "PENDING",
                    "items": [
                        {
                            "purchaseOrderItemId": %d,
                            "actualUnitCost": 10000000,
                            "productSerials": []
                        }
                    ]
                }
                """.formatted(approvedPoId, poiId1);

        mvc.perform(post("/api/goods-receipts").with(user("wh1").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON).content(bodyNoSerials))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Chưa thêm serial sản phẩm (productSerial) cho tất cả các mặt hàng thực nhận")));
    }

    @Test
    void createReceiptWithInvalidPoItemIds_failsImmediatelyWithInvalidIds() throws Exception {
        String body = """
                {
                    "code": "GR-INVALID-POI",
                    "purchaseOrderId": %d,
                    "status": "PENDING",
                    "items": [
                        {
                            "purchaseOrderItemId": 999999,
                            "actualUnitCost": 10000000,
                            "productSerials": [
                                {"serialNumber": "SN-INVALID-1", "status": "GOOD_CONDITION"}
                            ]
                        },
                        {
                            "purchaseOrderItemId": 888888,
                            "actualUnitCost": 10000000,
                            "productSerials": [
                                {"serialNumber": "SN-INVALID-2", "status": "GOOD_CONDITION"}
                            ]
                        }
                    ]
                }
                """.formatted(approvedPoId);

        mvc.perform(post("/api/goods-receipts").with(user("wh1").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("không tồn tại trong đơn đặt hàng")))
                .andExpect(jsonPath("$.detail").value(containsString("999999")))
                .andExpect(jsonPath("$.detail").value(containsString("888888")));
    }

    @Test
    void createReceiptWithBlankAndDuplicateSerials_returnsBatchErrors() throws Exception {
        String body = """
                {
                    "code": "GR-BATCH-ERR",
                    "purchaseOrderId": %d,
                    "status": "PENDING",
                    "items": [
                        {
                            "purchaseOrderItemId": %d,
                            "actualUnitCost": 10000000,
                            "productSerials": [
                                {"serialNumber": "", "status": "GOOD_CONDITION"},
                                {"serialNumber": "   ", "status": "GOOD_CONDITION"},
                                {"serialNumber": "DUP-BATCH-01", "status": "GOOD_CONDITION"},
                                {"serialNumber": "DUP-BATCH-01", "status": "DEFECTIVE"}
                            ]
                        }
                    ]
                }
                """.formatted(approvedPoId, poiId1);

        mvc.perform(post("/api/goods-receipts").with(user("wh1").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Phát hiện lỗi trong danh sách serial sản phẩm")))
                .andExpect(jsonPath("$.detail").value(containsString("Các serial bị bỏ trống")))
                .andExpect(jsonPath("$.detail").value(containsString("DUP-BATCH-01 (xuất hiện 2 lần)")));
    }

    @Test
    void unauthenticatedRequest_returnsUnauthorized401() throws Exception {
        mvc.perform(get("/api/goods-receipts").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forbiddenRole_returnsForbidden403() throws Exception {
        mvc.perform(get("/api/goods-receipts").with(user("sale").roles("STAFF_SALE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotCreateReceiptForNonApprovedPurchaseOrder() throws Exception {
        // Tạo một PO mới ở trạng thái DRAFT
        var draftPo = purchaseOrderService.createOrder(PurchaseOrderCreateRequest.builder()
                .code("PO-DRAFT-FOR-GR")
                .supplierId(supplier.getId())
                .orderDate(Instant.now())
                .items(List.of(PurchaseOrderItemRequest.builder().productId(product1.getId()).quantityOrder(2).unitCost(10000000L).build()))
                .build());

        String body = """
                {
                    "code": "GR-ON-DRAFT-PO",
                    "purchaseOrderId": %d,
                    "status": "DRAFT",
                    "items": []
                }
                """.formatted(draftPo.getId());

        mvc.perform(post("/api/goods-receipts").with(user("wh1").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("Chỉ được tạo phiếu nhập cho đơn đặt hàng đã duyệt (APPROVED)")));
    }

    @Test
    void cannotConfirmReceiptIfQuantityReceivedExceedsOrder() throws Exception {
        // product2 chỉ đặt 2 cái trong PO (poiId2)
        // Tạo phiếu nhập khai báo 3 serial -> vượt quá 2
        String body = """
                {
                    "code": "GR-EXCEED-QTY",
                    "purchaseOrderId": %d,
                    "status": "PENDING",
                    "items": [
                        {
                            "purchaseOrderItemId": %d,
                            "actualUnitCost": 40000000,
                            "productSerials": [
                                {"serialNumber": "VGA-SN-01", "status": "GOOD_CONDITION"},
                                {"serialNumber": "VGA-SN-02", "status": "GOOD_CONDITION"},
                                {"serialNumber": "VGA-SN-03", "status": "GOOD_CONDITION"}
                            ]
                        }
                    ]
                }
                """.formatted(approvedPoId, poiId2);

        String res = mvc.perform(post("/api/goods-receipts").with(user("wh1").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long grId = ((Number) com.jayway.jsonpath.JsonPath.read(res, "$.id")).longValue();

        // Manager cố tình duyệt CONFIRMED -> Bị chặn với lỗi BadRequest vì vượt quá quantityOrder
        mvc.perform(patch("/api/goods-receipts/" + grId + "/status")
                        .with(user("mgr").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("vượt quá số lượng đặt mua")));
    }

    @Test
    void cannotEditOrDeleteAnotherStaffDraftReceipt() throws Exception {
        // wh1 tạo 1 phiếu DRAFT
        var draft = createDraftReceiptForUser("GR-WH1-DRAFT", "wh1");

        // wh2 cố sửa phiếu DRAFT của wh1 -> 403 Forbidden
        String updateBody = """
                {
                    "code": "GR-WH1-DRAFT",
                    "name": "wh2 hack",
                    "items": []
                }
                """;
        mvc.perform(put("/api/goods-receipts/" + draft.getId()).with(user("wh2").roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isForbidden());

        // wh2 cố xóa phiếu DRAFT của wh1 -> 403 Forbidden
        mvc.perform(delete("/api/goods-receipts/" + draft.getId()).with(user("wh2").roles("STAFF_WAREHOUSE")))
                .andExpect(status().isForbidden());

        // wh1 tự xóa phiếu của mình -> 204 No Content
        mvc.perform(delete("/api/goods-receipts/" + draft.getId()).with(user("wh1").roles("STAFF_WAREHOUSE")))
                .andExpect(status().isNoContent());
    }

    private GoodsReceiptResponse createReceiptForUser(String code, String username) throws Exception {
        String body = """
                {
                    "code": "%s",
                    "purchaseOrderId": %d,
                    "status": "PENDING",
                    "items": [
                        {
                            "purchaseOrderItemId": %d,
                            "actualUnitCost": 10000000,
                            "productSerials": [
                                {"serialNumber": "SN-%s", "status": "GOOD_CONDITION"}
                            ]
                        }
                    ]
                }
                """.formatted(code, approvedPoId, poiId1, code);

        String res = mvc.perform(post("/api/goods-receipts").with(user(username).roles("STAFF_WAREHOUSE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = ((Number) com.jayway.jsonpath.JsonPath.read(res, "$.id")).longValue();
        return goodsReceiptService.getReceiptById(id);
    }

    private GoodsReceiptResponse createDraftReceiptForUser(String code, String username) throws Exception {
        String body = """
                {
                    "code": "%s",
                    "purchaseOrderId": %d,
                    "status": "DRAFT",
                    "items": [
                        {
                            "purchaseOrderItemId": %d,
                            "actualUnitCost": 10000000,
                            "productSerials": [
                                {"serialNumber": "SN-%s", "status": "GOOD_CONDITION"}
                            ]
                        }
                    ]
                }
                """.formatted(code, approvedPoId, poiId1, code);

        String role = username.equals("mgr") ? "MANAGER" : "STAFF_WAREHOUSE";
        String res = mvc.perform(post("/api/goods-receipts").with(user(username).roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = ((Number) com.jayway.jsonpath.JsonPath.read(res, "$.id")).longValue();
        return goodsReceiptService.getReceiptById(id);
    }
}
