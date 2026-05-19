package com.smartfinancepty.finance.controllers.swaggerdoc;

// En la clase:
// @Tag(name = "Auth", description = "Autenticación y gestión de tokens")

// En el método register:
/*
 * @Operation( summary = "Registrar nuevo usuario", description =
 * "Crea una nueva cuenta de usuario y retorna tokens JWT" )
 *
 * @ApiResponses({
 *
 * @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente", content
 * = @Content(schema = @Schema(implementation = AuthResponse.class), examples = @ExampleObject(value
 * = """ { "accessToken": "eyJhbGciOiJIUzI1NiJ9...", "refreshToken":
 * "550e8400-e29b-41d4-a716-446655440000", "tokenType": "Bearer", "email": "joel@smartfinance.com",
 * "fullName": "Joel Guerrero", "role": "USER" } """))),
 *
 * @ApiResponse(responseCode = "400", description =
 * "Datos inválidos (email mal formado, contraseña corta)"),
 *
 * @ApiResponse(responseCode = "409", description = "El email ya está registrado") })
 */

// En el método login:
/*
 * @Operation( summary = "Iniciar sesión", description =
 * "Autentica al usuario y retorna nuevos tokens JWT" )
 *
 * @ApiResponses({
 *
 * @ApiResponse(responseCode = "200", description = "Login exitoso"),
 *
 * @ApiResponse(responseCode = "401", description = "Credenciales incorrectas"),
 *
 * @ApiResponse(responseCode = "400", description = "Datos inválidos") })
 */

// En el método refreshToken:
/*
 * @Operation( summary = "Renovar token de acceso", description =
 * "Usa el refresh token para obtener un nuevo access token. El refresh token anterior queda invalidado (rotación)."
 * )
 *
 * @ApiResponses({
 *
 * @ApiResponse(responseCode = "200", description = "Token renovado exitosamente"),
 *
 * @ApiResponse(responseCode = "401", description = "Refresh token inválido, expirado o revocado")
 * })
 */

// En el método logout:
/*
 * @Operation( summary = "Cerrar sesión", description =
 * "Invalida todos los refresh tokens del usuario" )
 *
 * @ApiResponse(responseCode = "204", description = "Sesión cerrada exitosamente")
 */

class AuthControllerSwaggerDoc {
}
