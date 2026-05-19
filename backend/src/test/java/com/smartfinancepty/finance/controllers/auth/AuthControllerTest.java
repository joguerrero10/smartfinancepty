package com.smartfinancepty.finance.controllers.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinancepty.finance.dto.AuthResponse;
import com.smartfinancepty.finance.dto.LoginRequest;
import com.smartfinancepty.finance.dto.RegisterRequest;
import com.smartfinancepty.finance.exception.EmailAlreadyExistsException;
import com.smartfinancepty.finance.service.auth.AuthService;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private AuthService authService;

    private AuthResponse mockAuthResponse;

    @BeforeEach
    void setUp() {
        mockAuthResponse = AuthResponse.builder().accessToken("eyJhbGci...")
                .refreshToken("refresh-uuid").tokenType("Bearer").email("joel@smartfinance.com")
                .fullName("Joel Guerrero").role("USER").build();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterEndpointTests {

        @Test
        @DisplayName("Debe retornar 201 con token al registrarse")
        void shouldReturn201OnSuccessfulRegister() throws Exception {
            // Arrange
            RegisterRequest request = RegisterRequest.builder().fullName("Joel Guerrero")
                    .email("joel@smartfinance.com").password("Password123!").build();

            when(authService.register(any())).thenReturn(mockAuthResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").value("eyJhbGci..."))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.email").value("joel@smartfinance.com"))
                    .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @DisplayName("Debe retornar 400 si faltan campos requeridos")
        void shouldReturn400WhenMissingRequiredFields() throws Exception {
            // Arrange — email vacío
            RegisterRequest invalidRequest = RegisterRequest.builder().fullName("Joel").email("")
                    .password("Password123!").build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("Debe retornar 400 si la contraseña es muy corta")
        void shouldReturn400WhenPasswordTooShort() throws Exception {
            // Arrange
            RegisterRequest invalidRequest = RegisterRequest.builder().fullName("Joel")
                    .email("joel@test.com").password("123").build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 409 si el email ya existe")
        void shouldReturn409WhenEmailAlreadyExists() throws Exception {
            // Arrange
            RegisterRequest request = RegisterRequest.builder().fullName("Joel")
                    .email("joel@smartfinance.com").password("Password123!").build();

            when(authService.register(any()))
                    .thenThrow(new EmailAlreadyExistsException("El email ya está registrado"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginEndpointTests {

        @Test
        @DisplayName("Debe retornar 200 con token al hacer login")
        void shouldReturn200OnSuccessfulLogin() throws Exception {
            // Arrange
            LoginRequest request = LoginRequest.builder().email("joel@smartfinance.com")
                    .password("Password123!").build();

            when(authService.login(any())).thenReturn(mockAuthResponse);

            // Act & Assert
            mockMvc.perform(
                    post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("Debe retornar 400 si el email tiene formato inválido")
        void shouldReturn400WhenInvalidEmailFormat() throws Exception {
            // Arrange
            LoginRequest invalidRequest =
                    LoginRequest.builder().email("not-an-email").password("Password123!").build();

            // Act & Assert
            mockMvc.perform(
                    post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutEndpointTests {

        @Test
        @DisplayName("Debe retornar 204 al hacer logout")
        @WithMockUser(username = "joel@smartfinance.com")
        void shouldReturn204OnLogout() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/logout").with(csrf()))
                    .andExpect(status().isNoContent());

            verify(authService).logout("joel@smartfinance.com");
        }
    }
}
