package com.smartfinancepty.finance.exception;

import static org.assertj.core.api.Assertions.*;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.List;
import java.util.Map;
import static org.mockito.Mockito.*;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("Email Already Exists")
    class EmailExceptionTests {

        @Test
        @DisplayName("Debe retornar 409 CONFLICT para EmailAlreadyExistsException")
        void shouldReturn409ForEmailAlreadyExists() {
            EmailAlreadyExistsException ex = new EmailAlreadyExistsException("El email ya existe");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleEmailExists(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(409);
            assertThat(response.getBody().message()).isEqualTo("El email ya existe");
        }
    }

    @Nested
    @DisplayName("Invalid Token")
    class InvalidTokenTests {

        @Test
        @DisplayName("Debe retornar 401 UNAUTHORIZED para InvalidTokenException")
        void shouldReturn401ForInvalidToken() {
            InvalidTokenException ex = new InvalidTokenException("Token inválido");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleInvalidToken(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().status()).isEqualTo(401);
            assertThat(response.getBody().message()).isEqualTo("Token inválido");
        }
    }

    @Nested
    @DisplayName("Bad Credentials")
    class BadCredentialsTests {

        @Test
        @DisplayName("Debe retornar 401 UNAUTHORIZED para BadCredentialsException")
        void shouldReturn401ForBadCredentials() {
            BadCredentialsException ex = new BadCredentialsException("Bad credentials");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleBadCredentials(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().message()).contains("incorrectos");
        }
    }

    @Nested
    @DisplayName("Resource Not Found")
    class ResourceNotFoundTests {

        @Test
        @DisplayName("Debe retornar 404 NOT_FOUND para ResourceNotFoundException")
        void shouldReturn404ForResourceNotFound() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Recurso no encontrado");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleResourceNotFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().status()).isEqualTo(404);
            assertThat(response.getBody().message()).isEqualTo("Recurso no encontrado");
        }
    }

    @Nested
    @DisplayName("Illegal State")
    class IllegalStateTests {

        @Test
        @DisplayName("Debe retornar 409 CONFLICT para IllegalStateException")
        void shouldReturn409ForIllegalState() {
            IllegalStateException ex = new IllegalStateException("Estado inválido");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleIllegalState(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().status()).isEqualTo(409);
            assertThat(response.getBody().message()).isEqualTo("Estado inválido");
        }
    }

    @Nested
    @DisplayName("Illegal Argument")
    class IllegalArgumentTests {

        @Test
        @DisplayName("Debe retornar 400 BAD_REQUEST para IllegalArgumentException")
        void shouldReturn400ForIllegalArgument() {
            IllegalArgumentException ex = new IllegalArgumentException("Argumento inválido");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleIllegalArgument(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().message()).isEqualTo("Argumento inválido");
        }
    }

    @Nested
    @DisplayName("Method Argument Not Valid")
    class ValidationTests {

        @Test
        @DisplayName("Debe retornar 400 con mapa de errores de validación")
        void shouldReturn400WithValidationErrors() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);

            FieldError fieldError = new FieldError("request", "email", "El email es requerido");

            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

            ResponseEntity<Map<String, String>> response = handler.handleValidationErrors(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).containsEntry("email", "El email es requerido");
        }
    }

    @Nested
    @DisplayName("Method Argument Type Mismatch")
    class TypeMismatchTests {

        @Test
        @DisplayName("Debe retornar mensaje específico para tipo LocalDate")
        void shouldReturnDateFormatMessageForLocalDate() {
            MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
            when(ex.getRequiredType()).thenReturn((Class) LocalDate.class);

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleTypeMismatch(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().message()).contains("yyyy-MM-dd");
        }

        @Test
        @DisplayName("Debe retornar mensaje genérico para otros tipos")
        void shouldReturnGenericMessageForOtherTypes() {
            MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
            when(ex.getRequiredType()).thenReturn((Class) Long.class);

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleTypeMismatch(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().message()).isEqualTo("Parámetro inválido");
        }

        @Test
        @DisplayName("Debe retornar mensaje genérico cuando requiredType es null")
        void shouldReturnGenericMessageWhenRequiredTypeIsNull() {
            MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
            when(ex.getRequiredType()).thenReturn(null);

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleTypeMismatch(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().message()).isEqualTo("Parámetro inválido");
        }
    }

    @Nested
    @DisplayName("General Exception")
    class GeneralExceptionTests {

        @Test
        @DisplayName("Debe retornar 500 para excepción no manejada")
        void shouldReturn500ForUnhandledException() {
            Exception ex = new RuntimeException("Error inesperado");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleGeneral(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().status()).isEqualTo(500);
            assertThat(response.getBody().message()).isEqualTo("Error interno del servidor");
        }

        @Test
        @DisplayName("Debe relanzar excepciones de Spring")
        void shouldRethrowSpringExceptions() {
            // NoSuchBeanDefinitionException is a concrete Spring class starting with "org.springframework"
            Exception springEx = new org.springframework.beans.factory.NoSuchBeanDefinitionException("SomeBean");

            assertThatThrownBy(() -> handler.handleGeneral(springEx))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("ErrorResponse record")
    class ErrorResponseTests {

        @Test
        @DisplayName("ErrorResponse debe tener timestamp")
        void shouldHaveTimestamp() {
            GlobalExceptionHandler.ErrorResponse errorResponse =
                    new GlobalExceptionHandler.ErrorResponse(400, "Error");

            assertThat(errorResponse.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("ErrorResponse debe tener status y message")
        void shouldHaveStatusAndMessage() {
            GlobalExceptionHandler.ErrorResponse errorResponse =
                    new GlobalExceptionHandler.ErrorResponse(404, "No encontrado");

            assertThat(errorResponse.status()).isEqualTo(404);
            assertThat(errorResponse.message()).isEqualTo("No encontrado");
        }
    }
}
