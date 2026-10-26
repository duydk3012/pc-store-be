package com.datn.pc_store.modules.productprice;

import com.datn.pc_store.modules.product.entity.Product;
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
    void priceChangesCreateHistoryAndOmittedPriceKeepsCurrentVersion() throws Exception {
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
        assertThat(third.getId()).isEqualTo(second.getId());
        assertThat(priceRepository.findById(second.getId()).orElseThrow().getEffectiveTo()).isNull();
        assertThat(jdbc.queryForObject("SELECT price FROM products WHERE id=?", Long.class, product.getId())).isEqualTo(900000);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_price WHERE product_id=?", Long.class, product.getId())).isEqualTo(2);
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
    void enforcesValidityWindowAndHandlesLegacyNullStart() throws Exception {
        Instant at = Instant.now();
        ProductPrice expired = priceRepository.saveAndFlush(ProductPrice.builder().productId(product.getId()).price(100)
                .effectiveFrom(at.minusSeconds(7200)).effectiveTo(at.minusSeconds(3600)).presentPrice(true).build());
        Instant expiredAt = expired.getEffectiveTo();
        mvc.perform(get("/api/products/" + product.getId()).with(anonymous())).andExpect(status().isOk()).andExpect(jsonPath("$.priceId").isEmpty());
        var current = create(200);
        assertThat(priceRepository.findById(expired.getId()).orElseThrow().getEffectiveTo()).isEqualTo(expiredAt);
        priceRepository.findById(current.getId()).orElseThrow().setPresentPrice(false);
        priceRepository.flush();
        priceRepository.saveAndFlush(ProductPrice.builder().productId(product.getId()).price(300).presentPrice(true).build());
        mvc.perform(get("/api/products/" + product.getId()).with(anonymous()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.priceId").isNumber());
    }

    @Test
    void duplicateLegacyCurrentPricesAreReportedAndProductUpdateNormalizesThem() throws Exception {
        create(1000);
        priceRepository.saveAndFlush(ProductPrice.builder().productId(product.getId()).price(2000).presentPrice(true).build());
        mvc.perform(get("/api/products/" + product.getId()).with(anonymous())).andExpect(status().isConflict());
        create(3000);
        mvc.perform(get("/api/products/" + product.getId()).with(anonymous()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(3000));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_price WHERE product_id=? AND is_present_price=true", Long.class, product.getId())).isEqualTo(1);
    }

    @Test
    void swaggerDescribesPriceDtosAndPagination() throws Exception {
        mvc.perform(get("/v3/api-docs").with(anonymous())).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/admin/product-prices'].get.parameters[?(@.name == 'page')].in").value(contains("query")))
                .andExpect(jsonPath("$.components.schemas.ProductPriceResponse.properties.isPresentPrice").exists())
                .andExpect(jsonPath("$.paths['/api/products/{productId}/price']").doesNotExist());
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

    @Test
    void productPriceEndpointsAreReadOnly() throws Exception {
        var first = create(1000000);
        mvc.perform(post("/api/admin/product-prices").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/api/admin/product-prices/" + first.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"price\":1}"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/admin/product-prices/" + first.getId()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/admin/product-prices/products/" + product.getId() + "/current"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/product-prices/" + first.getId())).andExpect(status().isOk());
        var second = create(900000);
        var old = priceRepository.findById(first.getId()).orElseThrow();
        assertThat(old.getPrice()).isEqualTo(first.getPrice());
        assertThat(old.getCreatedDate()).isEqualTo(first.getCreatedDate());
        assertThat(old.getCreatedBy()).isEqualTo(first.getCreatedBy());
        assertThat(old.isPresentPrice()).isFalse();
        assertThat(old.getEffectiveTo()).isEqualTo(second.getEffectiveFrom());
        mvc.perform(get("/api/products/" + product.getId()).with(anonymous()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(900000));
        mvc.perform(put("/api/admin/products/" + product.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"GPU Demo\",\"code\":\"PRICE-001\",\"price\":900000}"))
                .andExpect(status().isOk());
        assertThat(priceService.getCurrentPrice(product.getId(), false).getId()).isEqualTo(second.getId());
    }
    private ProductPriceResponse create(long amount) {
        try {
            mvc.perform(put("/api/admin/products/" + product.getId()).with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"GPU Demo\",\"code\":\"PRICE-001\",\"price\":" + amount + "}"))
                    .andExpect(status().isOk());
            return priceService.getCurrentPrice(product.getId(), false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
