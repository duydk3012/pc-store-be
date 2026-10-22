package com.datn.pc_store.modules.productprice;

import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.enums.ProductStatus;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.productprice.dto.*;
import com.datn.pc_store.modules.productprice.entity.ProductPrice;
import com.datn.pc_store.modules.productprice.repository.ProductPriceRepository;
import com.datn.pc_store.modules.productprice.service.ProductPriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.*;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:product-price-test;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false", "app.admin.password=",
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
@Transactional
@WithMockUser(roles = "ADMIN")
class ProductPriceIntegrationTest {
    @Autowired private WebApplicationContext context;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductPriceRepository priceRepository;
    @Autowired private ProductPriceService priceService;
    @Autowired private JdbcTemplate jdbc;
    private MockMvc mvc;
    private Product product;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        product = productRepository.saveAndFlush(Product.builder().name("GPU Demo").code("PRICE-001").slug("price-gpu").build());
    }

    @Test
    void productCreateAutomaticallyPersistsPriceAndFirstHistoryRow() throws Exception {
        mvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"New Priced Product","code":"NEW-PRICED","price":2500000}
                        """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.price").value(2500000))
                .andExpect(jsonPath("$.priceId").isNumber());
        Product created = productRepository.findBySlug("new-priced-product").orElseThrow();
        assertThat(created.getPrice()).isEqualTo(2500000);
        assertThat(priceService.getCurrentPrice(created.getId(), false).getPrice()).isEqualTo(created.getPrice());
    }

    @Test
    void eachProductPutCreatesHistoryClosesOldPriceAndKeepsOmittedPrice() throws Exception {
        var first = create(1000000);
        String body = "{\"name\":\"GPU Demo\",\"code\":\"PRICE-001\",\"price\":900000}";
        mvc.perform(put("/api/admin/products/" + product.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(900000));
        var second = priceService.getCurrentPrice(product.getId(), false);
        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(priceRepository.findById(first.getId()).orElseThrow().getEffectiveTo()).isEqualTo(second.getEffectiveFrom());
        mvc.perform(put("/api/admin/products/" + product.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"GPU Renamed\",\"code\":\"PRICE-001\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(900000));
        var third = priceService.getCurrentPrice(product.getId(), false);
        assertThat(third.getId()).isNotEqualTo(second.getId());
        assertThat(priceRepository.findById(second.getId()).orElseThrow().getEffectiveTo()).isEqualTo(third.getEffectiveFrom());
        assertThat(jdbc.queryForObject("SELECT price FROM products WHERE id=?", Long.class, product.getId())).isEqualTo(900000);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_price WHERE product_id=?", Long.class, product.getId())).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_price WHERE product_id=? AND is_present_price=true", Long.class, product.getId())).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void productChangesRollbackIfHistoryCannotBeUpdated() throws Exception {
        try {
            create(1000000);
            priceRepository.saveAndFlush(ProductPrice.builder().productId(product.getId()).price(2000000)
                    .effectiveFrom(Instant.now().plusSeconds(3600)).presentPrice(true).build());
            mvc.perform(put("/api/admin/products/" + product.getId()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Must Roll Back\",\"code\":\"PRICE-001\",\"price\":3000000}"))
                    .andExpect(status().isConflict());
            assertThat(jdbc.queryForObject("SELECT price FROM products WHERE id=?", Long.class, product.getId())).isEqualTo(1000000);
            assertThat(jdbc.queryForObject("SELECT name FROM products WHERE id=?", String.class, product.getId())).isEqualTo("GPU Demo");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_price WHERE product_id=?", Long.class, product.getId())).isEqualTo(2);
        } finally {
            jdbc.update("DELETE FROM product_price WHERE product_id=?", product.getId());
            jdbc.update("DELETE FROM products WHERE id=?", product.getId());
        }
    }

    @Test
    void appliesCurrentPriceAndReturnsItInPublicProductResponses() throws Exception {
        mvc.perform(post("/api/admin/product-prices").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + product.getId() + ",\"price\":12990000}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.price").value(12990000))
                .andExpect(jsonPath("$.isPresentPrice").value(true)).andExpect(jsonPath("$.effectiveFrom").isNotEmpty());
        mvc.perform(get("/api/products/" + product.getId() + "/price").with(anonymous()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(12990000));
        mvc.perform(get("/api/products/" + product.getId()).with(anonymous()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(12990000)).andExpect(jsonPath("$.priceId").isNumber());
        mvc.perform(get("/api/products").with(anonymous())).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].price").value(12990000));
    }

    @Test
    void changingPriceCreatesNewIdAndPreservesOldAmountAndAudit() throws Exception {
        var first = create(1000000);
        mvc.perform(put("/api/admin/product-prices/" + first.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":900000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id", not(first.getId().intValue())))
                .andExpect(jsonPath("$.price").value(900000));
        var old = priceRepository.findById(first.getId()).orElseThrow();
        var current = priceService.getCurrentPrice(product.getId(), false);
        assertThat(old.getPrice()).isEqualTo(1000000);
        assertThat(old.getCreatedDate()).isEqualTo(first.getCreatedDate());
        assertThat(old.isPresentPrice()).isFalse();
        assertThat(old.getEffectiveTo()).isEqualTo(current.getEffectiveFrom());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_price WHERE product_id=?", Long.class, product.getId())).isEqualTo(2);
        mvc.perform(put("/api/admin/product-prices/" + first.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"price\":1}"))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/admin/product-prices/" + first.getId())).andExpect(status().isConflict());
    }

    @Test
    void subsequentPostClosesPreviousPriceAndDeleteRetainsHistory() throws Exception {
        var first = create(1000000);
        var second = create(2000000);
        assertThat(priceRepository.findById(first.getId()).orElseThrow().isPresentPrice()).isFalse();
        mvc.perform(delete("/api/admin/product-prices/" + second.getId())).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_price WHERE product_id=?", Long.class, product.getId())).isEqualTo(2);
        mvc.perform(get("/api/products/" + product.getId() + "/price").with(anonymous())).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/" + product.getId()).with(anonymous()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").isEmpty());
        mvc.perform(get("/api/admin/product-prices/" + first.getId())).andExpect(status().isOk());
    }

    @Test
    void adminOnlyWritesAndPublicCannotSeeHiddenOrDeletedProductPrice() throws Exception {
        var price = create(1000000);
        mvc.perform(get("/api/admin/product-prices").with(anonymous())).andExpect(status().isUnauthorized());
        for (HttpMethod method : new HttpMethod[]{HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE}) {
            String path = "/api/admin/product-prices" + (method == HttpMethod.PUT || method == HttpMethod.DELETE ? "/" + price.getId() : "");
            mvc.perform(request(method, path).with(user("customer").roles("CUSTOMER"))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.saveAndFlush(product);
        mvc.perform(get("/api/products/" + product.getId() + "/price").with(anonymous())).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/product-prices/products/" + product.getId() + "/current")).andExpect(status().isOk());
        product.setDeleted(true);
        productRepository.saveAndFlush(product);
        mvc.perform(get("/api/products/" + product.getId() + "/price").with(anonymous())).andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/product-prices").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + product.getId() + ",\"price\":1000}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void enforcesValidityWindowAndHandlesLegacyNullStart() throws Exception {
        Instant at = Instant.now();
        ProductPrice expired = priceRepository.saveAndFlush(ProductPrice.builder().productId(product.getId()).price(100)
                .effectiveFrom(at.minusSeconds(7200)).effectiveTo(at.minusSeconds(3600)).presentPrice(true).build());
        Instant expiredAt = expired.getEffectiveTo();
        mvc.perform(get("/api/products/" + product.getId() + "/price").with(anonymous())).andExpect(status().isNotFound());
        var current = create(200);
        assertThat(priceRepository.findById(expired.getId()).orElseThrow().getEffectiveTo()).isEqualTo(expiredAt);
        priceRepository.findById(current.getId()).orElseThrow().setPresentPrice(false);
        priceRepository.flush();
        priceRepository.saveAndFlush(ProductPrice.builder().productId(product.getId()).price(300).presentPrice(true).build());
        mvc.perform(get("/api/products/" + product.getId() + "/price").with(anonymous()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(300));
    }

    @Test
    void futureDatedLegacyPriceIsNotExposedAndCannotBeSilentlyRewritten() throws Exception {
        priceRepository.saveAndFlush(ProductPrice.builder().productId(product.getId()).price(500)
                .effectiveFrom(Instant.now().plusSeconds(3600)).presentPrice(true).build());
        mvc.perform(get("/api/products/" + product.getId() + "/price").with(anonymous())).andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/product-prices").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + product.getId() + ",\"price\":600}"))
                .andExpect(status().isConflict());
    }

    @Test
    void duplicateLegacyCurrentPricesAreReportedAndPostNormalizesThem() throws Exception {
        create(1000);
        priceRepository.saveAndFlush(ProductPrice.builder().productId(product.getId()).price(2000).presentPrice(true).build());
        mvc.perform(get("/api/products/" + product.getId() + "/price").with(anonymous())).andExpect(status().isConflict());
        create(3000);
        mvc.perform(get("/api/products/" + product.getId() + "/price").with(anonymous()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(3000));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_price WHERE product_id=? AND is_present_price=true", Long.class, product.getId())).isEqualTo(1);
    }

    @Test
    void validatesAmountReferencesAndSortAndFiltersHistory() throws Exception {
        for (String body : new String[]{"{}", "{\"productId\":1,\"price\":0}", "{\"productId\":1,\"price\":-1}",
                "{\"productId\":1,\"price\":1.5}", "{\"productId\":1,\"price\":9223372036854775808}"}) {
            mvc.perform(post("/api/admin/product-prices").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/admin/product-prices").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":999999,\"price\":1000}"))
                .andExpect(status().isNotFound());
        create(1000);
        create(2000);
        mvc.perform(get("/api/admin/product-prices").param("productId", product.getId().toString()).param("isPresentPrice", "false")
                        .param("sort", "price,desc").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].price").value(1000));
        for (String sort : new String[]{"[]", "[\"string\"]", "unknown,desc"}) {
            mvc.perform(get("/api/admin/product-prices").param("sort", sort)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void swaggerDescribesPriceDtosAndPagination() throws Exception {
        mvc.perform(get("/v3/api-docs").with(anonymous())).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/admin/product-prices'].get.parameters[?(@.name == 'page')].in").value(contains("query")))
                .andExpect(jsonPath("$.components.schemas.ProductPriceResponse.properties.isPresentPrice").exists())
                .andExpect(jsonPath("$.paths['/api/products/{productId}/price'].get.security").isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousChangesLeaveExactlyOneCurrentPrice() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var futures = List.of(1000L, 2000L).stream().map(amount -> executor.submit(() -> {
                start.await();
                return create(amount);
            })).toList();
            start.countDown();
            for (var future : futures) future.get(15, TimeUnit.SECONDS);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_price WHERE product_id=?", Long.class, product.getId())).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_price WHERE product_id=? AND is_present_price=true", Long.class, product.getId())).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            jdbc.update("DELETE FROM product_price WHERE product_id=?", product.getId());
            jdbc.update("DELETE FROM products WHERE id=?", product.getId());
        }
    }

    private ProductPriceResponse create(long amount) {
        return priceService.createPrice(ProductPriceCreateRequest.builder().productId(product.getId()).price(java.math.BigDecimal.valueOf(amount)).build());
    }
}
