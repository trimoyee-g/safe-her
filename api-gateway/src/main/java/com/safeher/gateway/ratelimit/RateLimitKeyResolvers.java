package com.safeher.gateway.ratelimit;

import com.safeher.gateway.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RateLimitKeyResolvers {

    private final JwtUtils jwtUtils;

    /**
     * Rate-limits by client IP address.
     * Used for unauthenticated (public) routes.
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = extractIp(exchange);
            return Mono.just("ip:" + ip);
        };
    }

    /**
     * Rate-limits by authenticated userId extracted from JWT.
     * Falls back to IP if no valid token present.
     * Used for authenticated routes – prevents one account from hammering the API.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String token = extractToken(exchange);
            if (StringUtils.hasText(token) && jwtUtils.isValid(token)) {
                String userId = jwtUtils.extractUserId(token);
                if (StringUtils.hasText(userId)) {
                    return Mono.just("user:" + userId);
                }
            }
            // Fallback to IP
            return Mono.just("ip:" + extractIp(exchange));
        };
    }

    /**
     * Rate-limits by email extracted from JWT.
     * Specifically for /auth endpoints to prevent credential-stuffing per email.
     */
    @Bean
    public KeyResolver emailKeyResolver() {
        return exchange -> {
            String token = extractToken(exchange);
            if (StringUtils.hasText(token)) {
                try {
                    String email = jwtUtils.extractEmail(token);
                    if (StringUtils.hasText(email)) {
                        return Mono.just("email:" + email);
                    }
                } catch (Exception ignored) {}
            }
            // For /auth endpoints the token may not exist yet – fall back to IP
            return Mono.just("ip:" + extractIp(exchange));
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractIp(ServerWebExchange exchange) {
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    private String extractToken(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}
