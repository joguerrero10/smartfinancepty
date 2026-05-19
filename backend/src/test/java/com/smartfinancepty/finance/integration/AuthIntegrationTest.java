package com.smartfinancepty.finance.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinancepty.finance.dto.AuthResponse;
import com.smartfinancepty.finance.dto.LoginRequest;
import com.smartfinancepty.finance.dto.RefreshTokenRequest;
import com.smartfinancepty.finance.dto.RegisterRequest;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Auth Integration Tests")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static String accessToken;
    private static String refreshToken;

    @Test
    @Order(1)
    @DisplayName("Flujo completo: Register → Login → Refresh → Logout")
    void shouldCompleteFullAuthFlow() throws Exception {

        // ── 1. Register ──────────────────────────────────────────────────────
        RegisterRequest registerRequest =
                RegisterRequest.builder().fullName("Test Integration User")
                        .email("integration@smartfinance.com").password("Password123!").build();

        MvcResult registerResult = mockMvc
                .perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.role").value("USER")).andReturn();

        AuthResponse registerResponse = objectMapper
                .readValue(registerResult.getResponse().getContentAsString(), AuthResponse.class);

        assertThat(registerResponse.getAccessToken()).isNotEmpty();
        accessToken = registerResponse.getAccessToken();
        refreshToken = registerResponse.getRefreshToken();

        // ── 2. Login ─────────────────────────────────────────────────────────
        LoginRequest loginRequest = LoginRequest.builder().email("integration@smartfinance.com")
                .password("Password123!").build();

        MvcResult loginResult = mockMvc
                .perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        AuthResponse loginResponse = objectMapper
                .readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);

        assertThat(loginResponse.getAccessToken()).isNotEmpty();
        refreshToken = loginResponse.getRefreshToken();

        // ── 3. Refresh Token ─────────────────────────────────────────────────
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

        MvcResult refreshResult = mockMvc
                .perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        AuthResponse refreshResponse = objectMapper
                .readValue(refreshResult.getResponse().getContentAsString(), AuthResponse.class);

        // El nuevo token debe ser diferente al anterior (rotación)
        assertThat(refreshResponse.getRefreshToken()).isNotEqualTo(refreshToken);
        accessToken = refreshResponse.getAccessToken();

        // ── 4. Logout ────────────────────────────────────────────────────────
        mockMvc.perform(
                post("/api/v1/auth/logout").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(2)
    @DisplayName("Debe rechazar email duplicado con 409")
    void shouldRejectDuplicateEmailWith409() throws Exception {
        // Arrange — misma petición que en el test anterior
        RegisterRequest duplicateRequest = RegisterRequest.builder().fullName("Duplicate User")
                .email("integration@smartfinance.com").password("Password123!").build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @Order(3)
    @DisplayName("Debe rechazar credenciales incorrectas con 401")
    void shouldRejectBadCredentialsWith401() throws Exception {
        // Arrange
        LoginRequest badLogin = LoginRequest.builder().email("integration@smartfinance.com")
                .password("WrongPassword!").build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(4)
    @DisplayName("Debe rechazar refresh token inválido con 401")
    void shouldRejectInvalidRefreshTokenWith401() throws Exception {
        // Arrange
        RefreshTokenRequest invalidRefresh = new RefreshTokenRequest("token-invalido-123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRefresh)))
                .andExpect(status().isUnauthorized());
    }
}
