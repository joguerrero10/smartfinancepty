package com.smartfinancepty.finance.config;

import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Configuration
@OpenAPIDefinition(info = @Info(title = "SmartFinance PTY API", version = "1.0",
        description = "Plataforma inteligente de gestión financiera personal para Panamá"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer",
        bearerFormat = "JWT")

public class SwaggerConfig {
}
