package com.smartfinancepty.finance.security.filter;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(2)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        // ── Clickjacking protection ───────────────────────────────────────────
        response.setHeader("X-Frame-Options", "DENY");

        // ── XSS Protection ────────────────────────────────────────────────────
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // ── Content type sniffing ─────────────────────────────────────────────
        response.setHeader("X-Content-Type-Options", "nosniff");

        // ── HSTS (solo en producción con HTTPS) ───────────────────────────────
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // ── Referrer Policy ───────────────────────────────────────────────────
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // ── Content Security Policy ───────────────────────────────────────────
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; " + "script-src 'self' 'unsafe-inline'; "
                        + "style-src 'self' 'unsafe-inline'; " + "img-src 'self' data: blob:; "
                        + "connect-src 'self' https://api.smartfinance.com; "
                        + "frame-ancestors 'none'");

        // ── Permissions Policy ────────────────────────────────────────────────
        response.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()");

        // ── Cache control para endpoints sensibles ────────────────────────────
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/v1/auth/") || uri.startsWith("/api/v1/files/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
        }

        chain.doFilter(request, response);
    }
}
