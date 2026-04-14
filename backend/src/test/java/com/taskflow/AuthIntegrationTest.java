package com.taskflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.dto.AuthDto;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest {

    @Autowired MockMvc       mvc;
    @Autowired ObjectMapper  mapper;

    private static String token;
    private static final String EMAIL    = "it-" + UUID.randomUUID() + "@example.com";
    private static final String PASSWORD = "securepassword1";

    /* ── Register ──────────────────────────────────────────────────── */

    @Test @Order(1)
    void register_withValidBody_returns201AndToken() throws Exception {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setName("Integration User");
        req.setEmail(EMAIL);
        req.setPassword(PASSWORD);

        MvcResult result = mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token",     notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.email", is(EMAIL)))
                .andReturn();

        token = mapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test @Order(2)
    void register_withDuplicateEmail_returns409() throws Exception {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setName("Dup");
        req.setEmail(EMAIL);
        req.setPassword(PASSWORD);

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("email")));
    }

    @Test @Order(3)
    void register_withMissingFields_returns400WithFieldErrors() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error",  is("validation failed")))
                .andExpect(jsonPath("$.fields", notNullValue()));
    }

    /* ── Login ──────────────────────────────────────────────────────── */

    @Test @Order(4)
    void login_withCorrectCredentials_returnsToken() throws Exception {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword(PASSWORD);

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test @Order(5)
    void login_withWrongPassword_returns400() throws Exception {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword("wrong-password");

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    /* ── Protected endpoint without token ───────────────────────────── */

    @Test @Order(6)
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mvc.perform(get("/projects"))
                .andExpect(status().isUnauthorized());
    }

    /* ── Expose token for sibling tests ─────────────────────────────── */
    public static String getToken() { return token; }
}
