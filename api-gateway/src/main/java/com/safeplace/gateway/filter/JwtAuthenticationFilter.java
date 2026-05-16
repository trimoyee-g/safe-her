package com.safeher.gateway.filter;

import com.safeher.gateway.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Custom Gateway filter factory registered as "JwtAuthentication" in route config.
 *
 * Responsibilities:
 *  1. Extract Bearer token from Authorization header
 *  2. Validate signature + expiry using shared JWT secret
 *  3. Reject with 401 if missing/invalid
 *  4. Forward identity as downstream headers (X-Auth-UserId, X-Auth-Username, X-Auth-Role)
 *  5. Strip the raw Authorization header so downstream services can trust the X-Auth-* headers
 *     (or keep it – both patterns work; we keep it so services can also validate themselves)
 */
@Component
@Slf4j
public class JwtAuthenticationFilter
        extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        super(Config.class);
        this.jwtUtils = jwtUtils;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String token = extractToken(request);

            if (!StringUtils.hasText(token)) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing Authorization header");
            }

            if (!jwtUtils.isValid(token)) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
            }

            // ── Token is valid – extract claims and forward as headers ──────────
            String userId   = jwtUtils.extractUserId(token);
            String username = jwtUtils.extractUsername(token);
            String role     = jwtUtils.extractRole(token);
            String email    = jwtUtils.extractEmail(token);

            log.debug("Authenticated [{}] role=[{}] → {}", username, role,
                    request.getPath().value());

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Auth-UserId",   userId   != null ? userId   : "")
                    .header("X-Auth-Username", username != null ? username : "")
                    .header("X-Auth-Role",     role     != null ? role     : "")
                    .header("X-Auth-Email",    email    != null ? email    : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"success":false,"message":"%s","timestamp":"%s"}
                """.formatted(message, java.time.OffsetDateTime.now()).strip();

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        var buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // No config properties needed – filter is stateless
    }
}
