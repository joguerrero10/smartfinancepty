package com.smartfinancepty.finance.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Bean
    public OpenAPI smartFinanceOpenAPI() {
        return new OpenAPI().info(buildInfo()).servers(buildServers()).components(buildComponents())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth")).tags(buildTags());
    }

    private Info buildInfo() {
        return new Info().title("SmartFinance PTY API").description("""
                ## API de Control Financiero Personal para Panamá

                SmartFinance PTY es una aplicación móvil de gestión financiera personal \
                diseñada para el mercado panameño. Permite a los usuarios controlar \
                ingresos, gastos, presupuestos y metas de ahorro.

                ### Características principales
                - 🔐 **Autenticación** JWT con refresh token
                - 💰 **Ingresos** con deducciones (CSS, SENNIAF, préstamos)
                - 💸 **Gastos** fijos y variables por categoría
                - 📊 **Dashboard** con balance en tiempo real (GraphQL)
                - 🎯 **Presupuestos** globales y por categoría con alertas
                - 💡 **Metas de ahorro** por monto fijo o porcentaje
                - 🔔 **Notificaciones** y recordatorios automáticos
                - 📈 **Analytics** con predicciones y recomendaciones IA
                - 📎 **Upload** de recibos y facturas

                ### Autenticación
                Usa el endpoint `/api/v1/auth/login` para obtener un token JWT. \
                Luego haz clic en **Authorize** y pega el token con el prefijo `Bearer `.

                ### GraphQL
                Los endpoints de Dashboard y Analytics también están disponibles via GraphQL \
                en `/graphql`. Puedes explorarlos en `/graphiql` (solo en desarrollo).
                """).version("1.0.0")
                .contact(new Contact().name("SmartFinance PTY Team").email("dev@smartfinance.com")
                        .url("https://smartfinance.com"))
                .license(new License().name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private List<Server> buildServers() {
        return List.of(
                new Server().url("http://localhost:8080").description("Servidor de Desarrollo"),
                new Server().url("https://api.smartfinance.com")
                        .description("Servidor de Producción"));
    }

    private Components buildComponents() {
        return new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token JWT obtenido del endpoint /api/v1/auth/login"));
    }

    private List<Tag> buildTags() {
        return List.of(
                new Tag().name("Auth").description("Registro, login, refresh token y logout"),
                new Tag().name("Incomes")
                        .description("Gestión de ingresos y deducciones (CSS, SENNIAF, préstamos)"),
                new Tag().name("Expenses")
                        .description("Gestión de gastos fijos y variables por categoría"),
                new Tag().name("Budgets")
                        .description("Presupuestos globales y por categoría con alertas al 80%"),
                new Tag().name("Savings Goals")
                        .description("Metas de ahorro por monto fijo o porcentaje del ingreso"),
                new Tag().name("Notifications")
                        .description("Notificaciones automáticas del sistema"),
                new Tag().name("Reminders")
                        .description("Recordatorios manuales recurrentes y puntuales"),
                new Tag().name("Analytics").description(
                        "Análisis financiero, tendencias, predicciones y recomendaciones IA"),
                new Tag().name("Files")
                        .description("Upload de recibos y facturas (JPG, PNG, PDF, XML)"));
    }
}
