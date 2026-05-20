package com.smartfinancepty.finance.security.filter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(1)
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.security.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${app.security.rate-limit.auth-requests-per-minute:10}")
    private int authRequestsPerMinute;

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean enabled;

    // key: IP o userId → bucket
    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        String key = resolveKey(request);
        int limit =
                isAuthEndpoint(request.getRequestURI()) ? authRequestsPerMinute : requestsPerMinute;

        RateBucket bucket = buckets.computeIfAbsent(key, k -> new RateBucket());

        if (!bucket.tryConsume(limit)) {
            log.warn("🚫 Rate limit excedido: {} → {}", key, request.getRequestURI());
            sendRateLimitResponse(response, key);
            return;
        }

        // Limpiar buckets expirados cada 1000 requests
        if (buckets.size() > 1000) {
            cleanExpiredBuckets();
        }

        chain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        // Prioridad: JWT userId > X-Forwarded-For > IP real
        String userId = (String) request.getAttribute("userId");
        if (userId != null)
            return "user:" + userId;

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private boolean isAuthEndpoint(String uri) {
        return uri.startsWith("/api/v1/auth/");
    }

    private void sendRateLimitResponse(HttpServletResponse response, String key)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.getWriter().write("""
                {
                  "status": 429,
                  "error": "Too Many Requests",
                  "message": "Has excedido el límite de requests. Intenta en 60 segundos.",
                  "retryAfter": 60
                }
                """);
    }

    private void cleanExpiredBuckets() {
        Instant now = Instant.now();
        buckets.entrySet().removeIf(e -> e.getValue().isExpired(now));
        log.debug("🧹 Limpieza de rate limit buckets: {} activos", buckets.size());
    }

    // ── Sliding window rate bucket ────────────────────────────────────────────

    private static class RateBucket {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();
        private static final long WINDOW_MS = 60_000; // 1 minuto

        synchronized boolean tryConsume(int limit) {
            long now = System.currentTimeMillis();
            if (now - windowStart >= WINDOW_MS) {
                // Nueva ventana
                count.set(0);
                windowStart = now;
            }
            if (count.get() >= limit)
                return false;
            count.incrementAndGet();
            return true;
        }

        boolean isExpired(Instant now) {
            return now.toEpochMilli() - windowStart > WINDOW_MS * 2;
        }
    }
}
