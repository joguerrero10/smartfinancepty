package com.smartfinancepty.finance.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import com.smartfinancepty.finance.domain.Role;
import com.smartfinancepty.finance.domain.User;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().email("joel@smartfinance.com").password("encodedPassword")
                .fullName("Joel Guerrero").username("joel@smartfinance.com").role(Role.USER)
                .enabled(true).build();

        userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Debe encontrar usuario por email")
    void shouldFindUserByEmail() {
        // Act
        Optional<User> result = userRepository.findByEmail("joel@smartfinance.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("joel@smartfinance.com");
        assertThat(result.get().getFullName()).isEqualTo("Joel Guerrero");
        assertThat(result.get().getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Debe retornar vacío para email inexistente")
    void shouldReturnEmptyForNonExistentEmail() {
        // Act
        Optional<User> result = userRepository.findByEmail("noexiste@test.com");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe confirmar que el email existe")
    void shouldReturnTrueWhenEmailExists() {
        // Act
        boolean exists = userRepository.existsByEmail("joel@smartfinance.com");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Debe retornar false para email que no existe")
    void shouldReturnFalseWhenEmailDoesNotExist() {
        // Act
        boolean exists = userRepository.existsByEmail("otro@test.com");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Debe guardar usuario con todos los campos")
    void shouldSaveUserWithAllFields() {
        // Arrange
        User newUser = User.builder().email("admin@smartfinance.com").password("encodedPass")
                .fullName("Admin User").username("admin@smartfinance.com").role(Role.ADMIN)
                .enabled(true).build();

        // Act
        User saved = userRepository.save(newUser);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Email debe ser único")
    void emailShouldBeUnique() {
        // Arrange
        User duplicate = User.builder().email("joel@smartfinance.com") // mismo email
                .password("otroPassword").fullName("Otro Usuario").username("joel@smartfinance.com")
                .role(Role.USER).enabled(true).build();

        // Act & Assert
        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(duplicate);
        }).isInstanceOf(Exception.class); // DataIntegrityViolationException
    }
}
