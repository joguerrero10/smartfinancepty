package com.smartfinancepty.finance.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Value("${app.security.cors.allowed-origins:http://localhost:4200,http://localhost:8100,capacitor://localhost,ionic://localhost}")
    private String allowedOrigins;

    @Value("${app.security.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${app.security.cors.max-age:3600}")
    private long maxAge;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Orígenes permitidos — separados por coma en application.yml
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);

        // Métodos HTTP permitidos
        config.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));

        // Headers permitidos en el request
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept",
                "X-Requested-With", "Cache-Control", "X-Forwarded-For"));

        // Headers expuestos al cliente
        config.setExposedHeaders(List.of("Authorization", "X-RateLimit-Limit",
                "X-RateLimit-Remaining", "Retry-After"));

        config.setAllowCredentials(true);
        config.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
