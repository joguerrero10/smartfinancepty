package com.smartfinancepty.finance.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private static final String BASE_URL = "/api/v1";

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    private static final String[] PUBLIC_ENDPOINTS =
            {BASE_URL + "/auth/login", BASE_URL + "/auth/register", BASE_URL + "/auth/refresh",
                    "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs",
                    "/v3/api-docs.yaml", "/graphql", "/graphiql", "/actuator/health"};


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())

                // ← Agrega esto:
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // ← Agrega esto (reemplaza el headers existente si lo tienes):
                .headers(headers -> headers.frameOptions(frame -> frame.deny())
                        .xssProtection(xss -> xss.disable()) // lo maneja SecurityHeadersFilter
                        .contentTypeOptions(Customizer.withDefaults()))

                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/api-docs/**", "/v3/api-docs/**", "/actuator/health", "/files/**")
                        .permitAll().anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


}
