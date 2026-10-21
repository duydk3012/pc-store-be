package com.datn.pc_store.modules.user;

import com.datn.pc_store.modules.auth.entity.RefreshToken;
import com.datn.pc_store.modules.auth.repository.RefreshTokenRepository;
import com.datn.pc_store.modules.auth.security.JwtService;
import com.datn.pc_store.modules.auth.service.RefreshTokenService;
import com.datn.pc_store.modules.user.entity.User;
import com.datn.pc_store.modules.user.enums.RoleType;
import com.datn.pc_store.modules.user.enums.UserStatus;
import com.datn.pc_store.modules.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-integration-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.admin.password=",
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
@Transactional
class UserIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private MockMvc mvc;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        adminToken = jwtService.generateToken(saveUser("testadmin", RoleType.ADMIN));
    }

    @Test
    void userManagementRequiresAdmin() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        String customerToken = jwtService.generateToken(saveUser("customer", RoleType.CUSTOMER));
        for (String method : new String[]{"GET", "POST", "PUT", "PATCH", "DELETE"}) {
            String path = switch (method) {
                case "PUT", "DELETE" -> "/api/admin/users/1";
                case "PATCH" -> "/api/admin/users/1/status";
                default -> "/api/admin/users";
            };
            mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), path)
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void createUserHashesPasswordAndReturnsSafeResponse() throws Exception {
        mvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createRequest("newuser")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.tokenVersion").doesNotExist());

        User created = userRepository.findByUsername("newuser").orElseThrow();
        assertThat(passwordEncoder.matches("Password123!", created.getPasswordHash())).isTrue();

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(""" 
                                {"username":"newuser","password":"Password123!"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());

        mvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createRequest("newuser")))
                .andExpect(status().isConflict());
    }

    @Test
    void searchFiltersAndSoftDeleteExcludeDeletedUsers() throws Exception {
        User user = saveUser("findme", RoleType.STAFF_TECH);
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken)
                        .param("keyword", "FINDME").param("role", "STAFF_TECH").param("status", "ACTIVE")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("findme"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.issue(user.getId());
        mvc.perform(delete("/api/admin/users/" + user.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        flushAndClear();

        assertThat(userRepository.findById(user.getId())).isEmpty();
        assertThat(refreshTokenService.findByRaw(refreshToken)).get().extracting(RefreshToken::isRevoked).isEqualTo(true);
        mvc.perform(get("/api/admin/users/" + user.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken).param("keyword", "findme"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void changingStatusRevokesTokensEvenAfterReactivation() throws Exception {
        User user = saveUser("blockeduser", RoleType.CUSTOMER);
        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.issue(user.getId());
        for (String status : new String[]{"BANNED", "ACTIVE"}) {
            mvc.perform(patch("/api/admin/users/" + user.getId() + "/status")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(status));
        }
        assertThat(user.getTokenVersion()).isEqualTo(2);
        flushAndClear();
        assertThat(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(refreshToken)))
                .get().extracting(RefreshToken::isRevoked).isEqualTo(true);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatePreservesPasswordUnlessProvidedAndRevokesSessionsOnRoleChange() throws Exception {
        User user = saveUser("updateuser", RoleType.CUSTOMER);
        String originalHash = user.getPasswordHash();
        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.issue(user.getId());
        String request = """
                {"username":"updateuser","fullName":"Updated User","email":"updated@example.com"}
                """;
        mvc.perform(put("/api/admin/users/" + user.getId()).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Updated User"));
        assertThat(user.getPasswordHash()).isEqualTo(originalHash);
        assertThat(user.getRole()).isEqualTo(RoleType.CUSTOMER);
        assertThat(user.getTokenVersion()).isZero();

        String roleUpdate = request.strip().replace("}", ",\"role\":\"MANAGER\",\"password\":\"ChangedPassword123!\"}");
        mvc.perform(put("/api/admin/users/" + user.getId()).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(roleUpdate))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("MANAGER"));
        assertThat(passwordEncoder.matches("ChangedPassword123!", user.getPasswordHash())).isTrue();
        assertThat(user.getTokenVersion()).isEqualTo(1);
        flushAndClear();
        assertThat(refreshTokenService.findByRaw(refreshToken)).get().extracting(RefreshToken::isRevoked).isEqualTo(true);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidInputAndDuplicateEmailAreRejected() throws Exception {
        mvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"username":"abc","password":"short","fullName":"","email":"invalid","phoneNumber":"123"}
                                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.username").exists());
        User user = saveUser("existing", RoleType.CUSTOMER);
        mvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("anotheruser").replace("anotheruser@example.com", user.getEmail())))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/admin/users/" + user.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidSortReturnsBadRequestInsteadOfServerError() throws Exception {
        for (String sort : new String[]{"[\"string\"]", "[]", "string", "unknownField,desc", "passwordHash,asc"}) {
            mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken)
                            .param("sort", sort))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Trường sắp xếp không hợp lệ")));
        }
    }

    @Test
    void supportsMultipleSortParametersAndPagination() throws Exception {
        saveUser("sorttestalpha", RoleType.CUSTOMER);
        saveUser("sorttestzulu", RoleType.CUSTOMER);
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken)
                        .param("keyword", "sorttest").param("sort", "role,asc", "username,desc")
                        .param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].username").value("sorttestzulu"));
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken)
                        .param("keyword", "sorttest").param("sort", "username,desc")
                        .param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("sorttestalpha"));
    }

    @Test
    void swaggerDocumentsSeparatePaginationParameters() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/admin/users'].get.parameters[?(@.name == 'page')].in")
                        .value(org.hamcrest.Matchers.contains("query")))
                .andExpect(jsonPath("$.paths['/api/admin/users'].get.parameters[?(@.name == 'size')].in")
                        .value(org.hamcrest.Matchers.contains("query")))
                .andExpect(jsonPath("$.paths['/api/admin/users'].get.parameters[?(@.name == 'sort')].in")
                        .value(org.hamcrest.Matchers.contains("query")))
                .andExpect(jsonPath("$.paths['/api/admin/users'].get.parameters[?(@.name == 'pageable')]").isEmpty());
    }

    @Test
    void publicRegistrationStillWorksAfterMovingUserModel() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(createRequest("registered")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
        assertThat(userRepository.findByUsername("registered")).isPresent();
    }

    private User saveUser(String username, RoleType role) {
        return userRepository.saveAndFlush(User.builder()
                .username(username).fullName("Test User").email(username + "@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(role).status(UserStatus.ACTIVE).build());
    }

    private void flushAndClear() {
        // Bulk JPQL token revocation bypasses entities cached in this test transaction.
        entityManager.flush();
        entityManager.clear();
    }

    private String createRequest(String username) {
        return """
                {"username":"%s","password":"Password123!","fullName":"Test User","email":"%s@example.com"}
                """.formatted(username, username);
    }
}
