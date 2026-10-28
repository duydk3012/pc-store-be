package com.datn.pc_store.modules.product;

import com.datn.pc_store.modules.brand.entity.Brand;
import com.datn.pc_store.modules.brand.repository.BrandRepository;
import com.datn.pc_store.modules.category.entity.Category;
import com.datn.pc_store.modules.category.repository.CategoryRepository;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.enums.*;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:product-integration-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.admin.password=",
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
@Transactional
@WithMockUser(roles = "ADMIN")
class ProductIntegrationTest {
    @Autowired private WebApplicationContext context;
    @Autowired private ProductRepository productRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void adminEndpointsRequireAdminButPublicReadIsAnonymous() throws Exception {
        mvc.perform(get("/api/admin/products").with(anonymous())).andExpect(status().isUnauthorized());
        for (HttpMethod method : new HttpMethod[]{HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE}) {
            String path = method == HttpMethod.PATCH ? "/api/admin/products/1/status"
                    : method == HttpMethod.PUT || method == HttpMethod.DELETE ? "/api/admin/products/1" : "/api/admin/products";
            mvc.perform(request(method, path).with(user("customer").roles("CUSTOMER"))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/products").with(anonymous())).andExpect(status().isOk());
    }

    @Test
    void createsProductWithDefaultsAndGeneratedVietnameseSlug() throws Exception {
        mvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Card màn hình ASUS", "code":"ASUS-001"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("card-man-hinh-asus"))
                .andExpect(jsonPath("$.productType").value("OTHER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.categoryIds").isEmpty());
        Product saved = productRepository.findBySlug("card-man-hinh-asus").orElseThrow();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM products WHERE id = ?", Integer.class, saved.getId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT product_type FROM products WHERE id = ?", Integer.class, saved.getId())).isZero();
        mvc.perform(get("/api/products/by-slug/card-man-hinh-asus").with(anonymous()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("ASUS-001"));
    }

    @Test
    void supportsBrandCategoriesPrimaryCategoryAndFilters() throws Exception {
        Brand brand = brandRepository.saveAndFlush(Brand.builder().name("ASUS").slug("asus").build());
        Category category = categoryRepository.saveAndFlush(Category.builder().name("Laptop").slug("laptop").build());
        String body = """
                {"name":"ASUS Laptop","code":"LAPTOP-001","productType":"PC","brandId":%d,"categoryIds":[%d],"primaryCategoryId":%d}
                """.formatted(brand.getId(), category.getId(), category.getId());
        mvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.brandId").value(brand.getId().intValue()))
                .andExpect(jsonPath("$.categoryIds[0]").value(category.getId().intValue()))
                .andExpect(jsonPath("$.primaryCategoryId").value(category.getId().intValue()));
        mvc.perform(get("/api/products").with(anonymous()).param("keyword", "ASUS")
                        .param("brandId", brand.getId().toString()).param("categoryId", category.getId().toString())
                        .param("productType", "PC").param("status", "ACTIVE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/admin/products").param("categoryId", "999999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void updateReplacesCategoriesWithoutDuplicateLinksAndPreservesOmittedEnums() throws Exception {
        Category first = categoryRepository.saveAndFlush(Category.builder().name("First").slug("first").build());
        Category second = categoryRepository.saveAndFlush(Category.builder().name("Second").slug("second").build());
        Product product = saveProduct("Laptop", "LAPTOP", ProductStatus.OUT_OF_STOCK, ProductType.PC);
        String body = """
                {"name":"Updated Laptop","code":"LAPTOP","categoryIds":[%d,%d],"primaryCategoryId":%d}
                """.formatted(first.getId(), second.getId(), first.getId());
        for (int i = 0; i < 2; i++) {
            mvc.perform(put("/api/admin/products/" + product.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.categoryIds", hasSize(2)))
                    .andExpect(jsonPath("$.status").value("OUT_OF_STOCK"))
                    .andExpect(jsonPath("$.productType").value("PC"));
        }
        mvc.perform(put("/api/admin/products/" + product.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Laptop\",\"code\":\"LAPTOP\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.categoryIds").isEmpty())
                .andExpect(jsonPath("$.primaryCategoryId").isEmpty());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM category_products WHERE product_id = ?", Long.class, product.getId())).isZero();
    }

    @Test
    void publicApiHidesInactiveProductsAndShowsOutOfStock() throws Exception {
        Product hidden = saveProduct("Hidden", "HIDDEN", ProductStatus.INACTIVE, ProductType.PC);
        saveProduct("Visible", "VISIBLE", ProductStatus.ACTIVE, ProductType.PC);
        saveProduct("Sold Out", "SOLD-OUT", ProductStatus.OUT_OF_STOCK, ProductType.PC);
        mvc.perform(get("/api/products").with(anonymous()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/admin/products"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/products").with(anonymous()).param("status", "INACTIVE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/products/" + hidden.getId()).with(anonymous())).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/by-slug/hidden").with(anonymous())).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/products/" + hidden.getId())).andExpect(status().isOk());
        mvc.perform(patch("/api/admin/products/" + hidden.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/products/" + hidden.getId()).with(anonymous())).andExpect(status().isOk());
    }

    @Test
    void softDeleteHidesProductAndReservesCodeAndSlug() throws Exception {
        Product product = saveProduct("Deleted", "DELETED", ProductStatus.ACTIVE, ProductType.OTHER);
        mvc.perform(delete("/api/admin/products/" + product.getId())).andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();
        assertThat(productRepository.findById(product.getId())).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT is_deleted FROM products WHERE id = ?", Boolean.class, product.getId())).isTrue();
        mvc.perform(get("/api/products/by-slug/deleted").with(anonymous())).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/products")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        for (String body : new String[]{"{\"name\":\"Other\",\"code\":\"DELETED\"}", "{\"name\":\"Deleted\",\"code\":\"OTHER\"}"}) {
            mvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isConflict());
        }
    }

    @Test
    void rejectsInvalidFieldsEnumsAndReferences() throws Exception {
        for (String body : new String[]{
                "{}", "{\"name\":\"Test\",\"code\":\"TEST\",\"status\":\"INVALID\"}",
                "{\"name\":\"Test\",\"code\":\"TEST\",\"productType\":\"INVALID\"}",
                "{\"name\":\"!!!\",\"code\":\"TEST\"}",
                "{\"name\":\"Test\",\"code\":\"TEST\",\"categoryIds\":[-1]}",
                "{\"name\":\"Test\",\"code\":\"TEST\",\"primaryCategoryId\":1}"}) {
            mvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        for (String field : new String[]{"\"brandId\":999999", "\"categoryIds\":[999999]"}) {
            mvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test\",\"code\":\"TEST\"," + field + "}"))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/admin/products/999999")).andExpect(status().isNotFound());
    }

    @Test
    void duplicateUpdatesDoNotChangeExistingProduct() throws Exception {
        saveProduct("First", "FIRST", ProductStatus.ACTIVE, ProductType.OTHER);
        Product second = saveProduct("Second", "SECOND", ProductStatus.ACTIVE, ProductType.OTHER);
        for (String body : new String[]{"{\"name\":\"Second\",\"code\":\"FIRST\"}", "{\"name\":\"First\",\"code\":\"SECOND\"}"}) {
            mvc.perform(put("/api/admin/products/" + second.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isConflict());
        }
        assertThat(second.getName()).isEqualTo("Second");
    }

    @Test
    void validatesSortAndSupportsMultipleSortsAndPagination() throws Exception {
        saveProduct("Alpha", "A", ProductStatus.ACTIVE, ProductType.OTHER);
        saveProduct("Zulu", "Z", ProductStatus.ACTIVE, ProductType.OTHER);
        for (String path : new String[]{"/api/products", "/api/admin/products"}) {
            for (String sort : new String[]{"[\"string\"]", "[]", "unknown,desc"}) {
                mvc.perform(get(path).param("sort", sort)).andExpect(status().isBadRequest());
            }
            mvc.perform(get(path).param("sort", "status,asc", "name,desc").param("size", "1").param("page", "1"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content[0].name").value("Alpha"));
        }
    }

    @Test
    void swaggerDocumentsPaginationAndPublicAccess() throws Exception {
        mvc.perform(get("/v3/api-docs").with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/products'].get.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/admin/products'].get.parameters[?(@.name == 'page')].in").value(contains("query")))
                .andExpect(jsonPath("$.paths['/api/products'].get.parameters[?(@.name == 'sort')].in").value(contains("query")))
                .andExpect(jsonPath("$.paths['/api/products'].get.parameters[?(@.name == 'pageable')]").isEmpty());
    }

    private Product saveProduct(String name, String code, ProductStatus status, ProductType type) {
        return productRepository.saveAndFlush(Product.builder().name(name).code(code)
                .slug(com.datn.pc_store.common.util.SlugUtils.toSlug(name)).status(status).productType(type).build());
    }
}
