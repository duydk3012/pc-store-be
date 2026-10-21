package com.datn.pc_store.modules.purchaseorder;

import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.enums.ProductStatus;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.purchaseorder.dto.*;
import com.datn.pc_store.modules.purchaseorder.service.PurchaseOrderService;
import com.datn.pc_store.modules.suppliers.entity.Supplier;
import com.datn.pc_store.modules.suppliers.enums.SupplierStatus;
import com.datn.pc_store.modules.suppliers.repository.SupplierRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:purchase-order-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.admin.password=",
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
@Transactional
@WithMockUser(roles = "MANAGER")
@Sql("/purchaseorder/receipt-schema.sql")
class PurchaseOrderIntegrationTest {
    @Autowired private WebApplicationContext context;
    @Autowired private PurchaseOrderService purchaseOrderService;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    private MockMvc mvc;
    private Supplier supplier;
    private Product product;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        supplier = supplierRepository.saveAndFlush(Supplier.builder().code("NCC-01").name("Nhà cung cấp")
                .email("supplier@example.com").build());
        product = productRepository.saveAndFlush(Product.builder().name("GPU").code("GPU-01").slug("gpu").build());
    }

    @Test
    void createsDraftAndCalculatesAmountsIgnoringClientControlledValues() throws Exception {
        String body = """
                {"code":" PO-01 ","supplierId":%d,"status":"RECEIVED","totalAmount":1,
                 "items":[{"productId":%d,"quantityOrder":3,"unitCost":5000000,"quantityReceived":99,"subtotal":1}]}
                """.formatted(supplier.getId(), product.getId());
        mvc.perform(post("/api/purchase-orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("PO-01"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmount").value(15000000))
                .andExpect(jsonPath("$.items[0].subtotal").value(15000000))
                .andExpect(jsonPath("$.items[0].quantityReceived").value(0));
        assertThat(jdbc.queryForObject("SELECT status FROM purchase_orders WHERE code='PO-01'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM goods_receipts", Long.class)).isZero();
    }

    @Test
    void draftMayBeEmptyButCannotBeSubmitted() throws Exception {
        var order = createOrder("EMPTY", List.of());
        assertThat(order.getTotalAmount()).isZero();
        transition(order.getId(), "PENDING", 400);
        assertThat(purchaseOrderService.getOrderById(order.getId()).getStatus().name()).isEqualTo("DRAFT");
    }

    @Test
    void replacesDraftItemsAndRecalculatesTotal() throws Exception {
        var order = createOrder("UPDATE", items(2, 100));
        String body = """
                {"code":"UPDATED","name":"Đơn đã sửa","supplierId":%d,
                 "items":[{"productId":%d,"quantityOrder":4,"unitCost":300}]}
                """.formatted(supplier.getId(), product.getId());
        mvc.perform(put("/api/purchase-orders/" + order.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(1200))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.orderDate").value(order.getOrderDate().toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM purchase_order_items WHERE purchase_order_id=?", Long.class, order.getId())).isEqualTo(1);
        mvc.perform(get("/api/purchase-orders/by-code/UPDATED")).andExpect(status().isOk());
    }

    @Test
    void enforcesLifecycleAndPreventsEditingSubmittedOrders() throws Exception {
        var order = createOrder("FLOW", items(2, 100));
        transition(order.getId(), "APPROVED", 409);
        transition(order.getId(), "PENDING", 200);
        transition(order.getId(), "DRAFT", 409);
        mvc.perform(delete("/api/purchase-orders/" + order.getId())).andExpect(status().isConflict());
        mvc.perform(put("/api/purchase-orders/" + order.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"FLOW\",\"supplierId\":" + supplier.getId() + "}"))
                .andExpect(status().isConflict());
        transition(order.getId(), "APPROVED", 200);
        transition(order.getId(), "RECEIVED", 409);
        transition(order.getId(), "CANCELLED", 200);
        transition(order.getId(), "CANCELLED", 200);
        transition(order.getId(), "PENDING", 409);
    }

    @Test
    void deletionPreservesItemsAndReservesCode() throws Exception {
        var order = createOrder("DELETE", items(1, 100));
        mvc.perform(delete("/api/purchase-orders/" + order.getId())).andExpect(status().isNoContent());
        entityManager.clear();
        mvc.perform(get("/api/purchase-orders/" + order.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/purchase-orders/by-code/DELETE")).andExpect(status().isNotFound());
        mvc.perform(get("/api/purchase-orders")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM purchase_order_items WHERE purchase_order_id=?", Long.class, order.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM purchase_orders WHERE id=?", Boolean.class, order.getId())).isTrue();
        mvc.perform(post("/api/purchase-orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DELETE\",\"supplierId\":" + supplier.getId() + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    void warehouseCanReadButOnlyManagersAndAdminsCanWrite() throws Exception {
        var order = createOrder("ACCESS", items(1, 100));
        mvc.perform(get("/api/purchase-orders").with(anonymous())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/purchase-orders").with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/purchase-orders").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        for (String path : new String[]{"/api/purchase-orders", "/api/purchase-orders/" + order.getId(), "/api/purchase-orders/by-code/ACCESS"}) {
            mvc.perform(get(path).with(user("warehouse").roles("STAFF_WAREHOUSE"))).andExpect(status().isOk());
        }
        for (HttpMethod method : new HttpMethod[]{HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE}) {
            String path = method == HttpMethod.POST ? "/api/purchase-orders"
                    : "/api/purchase-orders/" + order.getId() + (method == HttpMethod.PATCH ? "/status" : "");
            mvc.perform(request(method, path).with(user("warehouse").roles("STAFF_WAREHOUSE"))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void receiptReferencesPreventDraftEditAndDelete() throws Exception {
        var order = createOrder("LINKED", items(1, 100));
        addReceipt(order.getId(), "REJECTED-RECEIPT", 2);
        mvc.perform(delete("/api/purchase-orders/" + order.getId())).andExpect(status().isConflict());
        mvc.perform(put("/api/purchase-orders/" + order.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"LINKED\",\"supplierId\":" + supplier.getId() + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cannotCancelOrderWithPendingConfirmedOrReceivedGoods() throws Exception {
        var order = createOrder("CANCEL", items(5, 100));
        transition(order.getId(), "PENDING", 200);
        transition(order.getId(), "APPROVED", 200);
        long receiptId = addReceipt(order.getId(), "RECEIPT-CANCEL", 0);
        transition(order.getId(), "CANCELLED", 409);
        jdbc.update("UPDATE goods_receipts SET status=1 WHERE id=?", receiptId);
        transition(order.getId(), "CANCELLED", 409);
        jdbc.update("UPDATE goods_receipts SET status=3 WHERE id=?", receiptId);
        jdbc.update("UPDATE purchase_order_items SET quantity_received=1 WHERE id=?", order.getItems().getFirst().getId());
        entityManager.clear();
        transition(order.getId(), "CANCELLED", 409);
    }

    @Test
    void completesOnlyAfterAllConfirmedReceiptQuantitiesMatchOrderedAndReceived() throws Exception {
        var order = createOrder("RECEIVE", items(5, 100));
        transition(order.getId(), "PENDING", 200);
        transition(order.getId(), "APPROVED", 200);
        long receiptId = addReceipt(order.getId(), "RECEIPT-OK", 1);
        long itemId = order.getItems().getFirst().getId();
        jdbc.update("INSERT INTO receipt_items (goods_receipt_id,purchase_order_item_id,quantity_received) VALUES (?,?,?)", receiptId, itemId, 2);
        jdbc.update("UPDATE purchase_order_items SET quantity_received=2 WHERE id=?", itemId);
        entityManager.clear();
        transition(order.getId(), "RECEIVED", 409);
        jdbc.update("UPDATE purchase_order_items SET quantity_received=5 WHERE id=?", itemId);
        entityManager.clear();
        transition(order.getId(), "RECEIVED", 409);
        jdbc.update("UPDATE receipt_items SET quantity_received=5 WHERE purchase_order_item_id=?", itemId);
        long pending = addReceipt(order.getId(), "STILL-PENDING", 0);
        transition(order.getId(), "RECEIVED", 409);
        jdbc.update("UPDATE goods_receipts SET status=3 WHERE id=?", pending);
        transition(order.getId(), "RECEIVED", 200);
        transition(order.getId(), "CANCELLED", 409);
        assertThat(jdbc.queryForObject("SELECT status FROM purchase_orders WHERE id=?", Integer.class, order.getId())).isEqualTo(3);
    }

    @Test
    void rejectsInvalidQuantitiesDatesDuplicateProductsAndOverflow() throws Exception {
        String prefix = "{\"code\":\"INVALID\",\"supplierId\":" + supplier.getId();
        for (String extra : new String[]{
                ",\"items\":[null]}",
                ",\"items\":[{\"productId\":" + product.getId() + ",\"quantityOrder\":0,\"unitCost\":100}]}",
                ",\"items\":[{\"productId\":" + product.getId() + ",\"quantityOrder\":1,\"unitCost\":-1}]}",
                ",\"orderDate\":\"2026-10-21T00:00:00Z\",\"expectedDate\":\"2026-10-20T00:00:00Z\"}",
                ",\"items\":[{\"productId\":" + product.getId() + ",\"quantityOrder\":2,\"unitCost\":9223372036854775807}]}"
        }) {
            mvc.perform(post("/api/purchase-orders").contentType(MediaType.APPLICATION_JSON).content(prefix + extra))
                    .andExpect(status().isBadRequest());
        }
        String repeated = "{\"productId\":" + product.getId() + ",\"quantityOrder\":1,\"unitCost\":100}";
        mvc.perform(post("/api/purchase-orders").contentType(MediaType.APPLICATION_JSON)
                        .content(prefix + ",\"items\":[" + repeated + "," + repeated + "]}"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM purchase_orders", Long.class)).isZero();
    }

    @Test
    void detectsOverflowWhenSummingOtherwiseValidItems() throws Exception {
        Product second = productRepository.saveAndFlush(Product.builder().name("CPU").code("CPU").slug("cpu").build());
        String body = """
                {"code":"OVERFLOW","supplierId":%d,"items":[
                 {"productId":%d,"quantityOrder":1,"unitCost":9223372036854775807},
                 {"productId":%d,"quantityOrder":1,"unitCost":1}]}
                """.formatted(supplier.getId(), product.getId(), second.getId());
        mvc.perform(post("/api/purchase-orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void revalidatesSupplierAndProductBeforeSubmittingAndApproving() throws Exception {
        var order = createOrder("VALIDATE", items(1, 100));
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.saveAndFlush(product);
        transition(order.getId(), "PENDING", 400);
        product.setStatus(ProductStatus.OUT_OF_STOCK);
        productRepository.saveAndFlush(product);
        transition(order.getId(), "PENDING", 200);
        supplier.setStatus(SupplierStatus.INACTIVE);
        supplierRepository.saveAndFlush(supplier);
        transition(order.getId(), "APPROVED", 400);
    }

    @Test
    void rejectsMissingAndDeletedReferences() throws Exception {
        String missingSupplier = "{\"code\":\"BAD-SUPPLIER\",\"supplierId\":999999}";
        mvc.perform(post("/api/purchase-orders").contentType(MediaType.APPLICATION_JSON).content(missingSupplier))
                .andExpect(status().isNotFound());
        product.setDeleted(true);
        productRepository.saveAndFlush(product);
        entityManager.clear();
        mvc.perform(post("/api/purchase-orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"BAD-PRODUCT\",\"supplierId\":" + supplier.getId()
                                + ",\"items\":[{\"productId\":" + product.getId() + ",\"quantityOrder\":1,\"unitCost\":100}]}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/purchase-orders/999999")).andExpect(status().isNotFound());
    }

    @Test
    void filtersByKeywordStatusSupplierAndDateAndValidatesSort() throws Exception {
        var first = createOrder("FILTER-A", items(1, 100));
        var second = createOrder("FILTER-Z", items(2, 100));
        transition(first.getId(), "PENDING", 200);
        transition(second.getId(), "CANCELLED", 200);
        mvc.perform(get("/api/purchase-orders").param("keyword", "filter").param("supplierId", supplier.getId().toString())
                        .param("sort", "code,desc").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].code").value("FILTER-Z"));
        mvc.perform(get("/api/purchase-orders").param("status", "PENDING")
                        .param("fromDate", "2026-01-01T00:00:00Z").param("toDate", "2026-12-31T23:59:59Z"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/purchase-orders").param("fromDate", "2026-10-22T00:00:00Z").param("toDate", "2026-10-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
        for (String sort : new String[]{"[]", "[\"string\"]", "unknown,desc"}) {
            mvc.perform(get("/api/purchase-orders").param("sort", sort)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void swaggerDescribesDtosAndIndividualPaginationParameters() throws Exception {
        mvc.perform(get("/v3/api-docs").with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/purchase-orders'].get.parameters[?(@.name == 'page')].in").value(contains("query")))
                .andExpect(jsonPath("$.paths['/api/purchase-orders'].get.parameters[?(@.name == 'sort')].in").value(contains("query")))
                .andExpect(jsonPath("$.components.schemas.PurchaseOrderCreateRequest.description").isNotEmpty())
                .andExpect(jsonPath("$.components.schemas.PurchaseOrderStatusUpdateRequest.properties.status").exists());
    }

    private PurchaseOrderResponse createOrder(String code, List<PurchaseOrderItemRequest> items) {
        return purchaseOrderService.createOrder(PurchaseOrderCreateRequest.builder().code(code).supplierId(supplier.getId())
                .orderDate(java.time.Instant.parse("2026-10-21T08:00:00Z")).items(items).build());
    }

    private List<PurchaseOrderItemRequest> items(int quantity, long cost) {
        return List.of(PurchaseOrderItemRequest.builder().productId(product.getId()).quantityOrder(quantity).unitCost(cost).build());
    }

    private void transition(Long id, String target, int expectedStatus) throws Exception {
        mvc.perform(patch("/api/purchase-orders/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + target + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    private long addReceipt(Long orderId, String code, int status) {
        jdbc.update("INSERT INTO goods_receipts (code,purchase_order_id,status) VALUES (?,?,?)", code, orderId, status);
        return jdbc.queryForObject("SELECT id FROM goods_receipts WHERE code=?", Long.class, code);
    }
}
