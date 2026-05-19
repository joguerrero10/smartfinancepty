package com.smartfinancepty.finance.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.smartfinancepty.finance.domain.Role;
import com.smartfinancepty.finance.domain.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    private static final String SECRET_KEY =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION = 86400000L; // 24 horas

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);

        testUser = User.builder().id(1L).email("joel@smartfinance.com")
                .username("joel@smartfinance.com").role(Role.USER).enabled(true).build();
    }

    @Test
    @DisplayName("Debe generar token válido")
    void shouldGenerateValidToken() {
        // Act
        String token = jwtService.generateToken(testUser);

        // Assert
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("Debe extraer username del token")
    void shouldExtractUsernameFromToken() {
        // Arrange
        String token = jwtService.generateToken(testUser);

        // Act
        String username = jwtService.extractUsername(token);

        // Assert
        assertThat(username).isEqualTo("joel@smartfinance.com");
    }

    @Test
    @DisplayName("Debe validar token correctamente")
    void shouldValidateTokenCorrectly() {
        // Arrange
        String token = jwtService.generateToken(testUser);

        // Act
        boolean isValid = jwtService.isTokenValid(token, testUser);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Debe invalidar token de otro usuario")
    void shouldInvalidateTokenForDifferentUser() {
        // Arrange
        String token = jwtService.generateToken(testUser);

        User otherUser = User.builder().id(2L).email("otro@smartfinance.com")
                .username("otro@smartfinance.com").role(Role.USER).enabled(true).build();

        // Act
        boolean isValid = jwtService.isTokenValid(token, otherUser);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Debe generar tokens diferentes en cada llamada")
    void shouldGenerateDifferentTokensEachTime() throws InterruptedException {
        // Act
        String token1 = jwtService.generateToken(testUser);
        Thread.sleep(1000); // diferencia de tiempo
        String token2 = jwtService.generateToken(testUser);

        // Assert
        assertThat(token1).isNotEqualTo(token2);
    }
}
