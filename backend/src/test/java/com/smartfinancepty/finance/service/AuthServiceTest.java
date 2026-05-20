package com.smartfinancepty.finance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import com.smartfinancepty.finance.domain.RefreshToken;
import com.smartfinancepty.finance.domain.Role;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.AuthResponse;
import com.smartfinancepty.finance.dto.LoginRequest;
import com.smartfinancepty.finance.dto.RefreshTokenRequest;
import com.smartfinancepty.finance.dto.RegisterRequest;
import com.smartfinancepty.finance.exception.EmailAlreadyExistsException;
import com.smartfinancepty.finance.exception.InvalidTokenException;
import com.smartfinancepty.finance.repository.RefreshTokenRepository;
import com.smartfinancepty.finance.repository.UserRepository;
import com.smartfinancepty.finance.security.JwtService;
import com.smartfinancepty.finance.service.auth.AuthService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RefreshToken validToken;
    private RefreshToken expiredToken;
    private RefreshToken revokedToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 86400000L);

        testUser = User.builder().id(1L).email("joel@smartfinance.com")
                .username("joel@smartfinance.com").fullName("Joel Guerrero")
                .password("encoded_password").role(Role.USER).enabled(true).build();

        validToken = RefreshToken.builder().id(1L).token("valid-refresh-token").user(testUser)
                .expiryDate(Instant.now().plusSeconds(3600)).revoked(false).build();

        expiredToken = RefreshToken.builder().id(2L).token("expired-token").user(testUser)
                .expiryDate(Instant.now().minusSeconds(3600)).revoked(false).build();

        revokedToken = RefreshToken.builder().id(3L).token("revoked-token").user(testUser)
                .expiryDate(Instant.now().plusSeconds(3600)).revoked(true).build();
    }

    @Nested
    @DisplayName("Register")
    class RegisterTests {

        @Test
        @DisplayName("Debe registrar usuario exitosamente")
        void shouldRegisterUserSuccessfully() {
            RegisterRequest request = RegisterRequest.builder().fullName("Joel Guerrero")
                    .email("joel@smartfinance.com").password("pass123").build();

            when(userRepository.existsByEmail("joel@smartfinance.com")).thenReturn(false);
            when(passwordEncoder.encode("pass123")).thenReturn("encoded_password");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtService.generateToken(any())).thenReturn("access_token");
            when(refreshTokenRepository.save(any())).thenReturn(validToken);

            AuthResponse response = authService.register(request);

            assertThat(response.getAccessToken()).isEqualTo("access_token");
            assertThat(response.getEmail()).isEqualTo("joel@smartfinance.com");
            assertThat(response.getRole()).isEqualTo("USER");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción si el email ya existe")
        void shouldThrowWhenEmailAlreadyExists() {
            RegisterRequest request = RegisterRequest.builder().fullName("Joel")
                    .email("joel@smartfinance.com").password("pass123").build();

            when(userRepository.existsByEmail("joel@smartfinance.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining("El email ya está registrado");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("Debe autenticar correctamente y retornar tokens")
        void shouldLoginSuccessfully() {
            LoginRequest request = LoginRequest.builder().email("joel@smartfinance.com")
                    .password("pass123").build();

            when(userRepository.findByEmail("joel@smartfinance.com"))
                    .thenReturn(Optional.of(testUser));
            when(jwtService.generateToken(testUser)).thenReturn("access_token");
            when(refreshTokenRepository.save(any())).thenReturn(validToken);

            AuthResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo("access_token");
            assertThat(response.getEmail()).isEqualTo("joel@smartfinance.com");
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(refreshTokenRepository).revokeAllUserTokens(testUser);
        }

        @Test
        @DisplayName("Debe lanzar excepción si credenciales son incorrectas")
        void shouldThrowWhenBadCredentials() {
            LoginRequest request = LoginRequest.builder().email("joel@smartfinance.com")
                    .password("wrongpass").build();

            doThrow(new BadCredentialsException("Bad credentials")).when(authenticationManager)
                    .authenticate(any());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Debe lanzar RuntimeException si usuario no encontrado tras autenticación")
        void shouldThrowWhenUserNotFoundAfterAuth() {
            LoginRequest request = LoginRequest.builder().email("notfound@test.com")
                    .password("pass").build();

            when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }
    }

    @Nested
    @DisplayName("Refresh Token")
    class RefreshTokenTests {

        @Test
        @DisplayName("Debe rotar refresh token y retornar nuevos tokens")
        void shouldRefreshTokenSuccessfully() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("valid-refresh-token").build();

            when(refreshTokenRepository.findByToken("valid-refresh-token"))
                    .thenReturn(Optional.of(validToken));
            when(jwtService.generateToken(testUser)).thenReturn("new_access_token");
            when(refreshTokenRepository.save(any())).thenReturn(validToken);

            AuthResponse response = authService.refreshToken(request);

            assertThat(response.getAccessToken()).isEqualTo("new_access_token");
            assertThat(validToken.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("Debe lanzar excepción si refresh token no existe")
        void shouldThrowWhenRefreshTokenNotFound() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("nonexistent").build();

            when(refreshTokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Refresh token no encontrado");
        }

        @Test
        @DisplayName("Debe lanzar excepción si refresh token está revocado")
        void shouldThrowWhenRefreshTokenIsRevoked() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("revoked-token").build();

            when(refreshTokenRepository.findByToken("revoked-token"))
                    .thenReturn(Optional.of(revokedToken));

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Refresh token revocado");
        }

        @Test
        @DisplayName("Debe lanzar excepción y eliminar token si está expirado")
        void shouldThrowAndDeleteWhenRefreshTokenIsExpired() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("expired-token").build();

            when(refreshTokenRepository.findByToken("expired-token"))
                    .thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("expirado");

            verify(refreshTokenRepository).delete(expiredToken);
        }
    }

    @Nested
    @DisplayName("Logout")
    class LogoutTests {

        @Test
        @DisplayName("Debe revocar todos los tokens del usuario")
        void shouldLogoutSuccessfully() {
            when(userRepository.findByEmail("joel@smartfinance.com"))
                    .thenReturn(Optional.of(testUser));

            authService.logout("joel@smartfinance.com");

            verify(refreshTokenRepository).revokeAllUserTokens(testUser);
        }

        @Test
        @DisplayName("Debe lanzar excepción si usuario no existe en logout")
        void shouldThrowWhenUserNotFoundOnLogout() {
            when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.logout("notfound@test.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }
    }
}
