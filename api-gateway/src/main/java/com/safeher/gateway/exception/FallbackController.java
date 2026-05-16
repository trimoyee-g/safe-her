package com.safeher.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Fallback endpoints invoked by the CircuitBreaker filter when a downstream
 * service is unavailable or the circuit is open.
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    @GetMapping("/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authFallback(ServerWebExchange exchange) {
        log.warn("Circuit open – Auth Service unavailable");
        return fallback("Auth Service", exchange);
    }

    @GetMapping("/user")
    public Mono<ResponseEntity<Map<String, Object>>> userFallback(ServerWebExchange exchange) {
        log.warn("Circuit open – User Service unavailable");
        return fallback("User Service", exchange);
    }

    @GetMapping("/place")
    public Mono<ResponseEntity<Map<String, Object>>> placeFallback(ServerWebExchange exchange) {
        log.warn("Circuit open – Place Service unavailable");
        return fallback("Place Service", exchange);
    }

    @GetMapping("/rating")
    public Mono<ResponseEntity<Map<String, Object>>> ratingFallback(ServerWebExchange exchange) {
        log.warn("Circuit open – Rating Service unavailable");
        return fallback("Rating Service", exchange);
    }

    @GetMapping("/ai")
    public Mono<ResponseEntity<Map<String, Object>>> aiFallback(ServerWebExchange exchange) {
        log.warn("Circuit open – AI Service unavailable");
        return fallback("AI Service", exchange);
    }

    private Mono<ResponseEntity<Map<String, Object>>> fallback(String service,
                                                                ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders()
                .getFirst("X-Trace-Id");

        Map<String, Object> body = Map.of(
                "success",   false,
                "message",   service + " is temporarily unavailable. Please try again shortly.",
                "traceId",   traceId != null ? traceId : "n/a",
                "timestamp", OffsetDateTime.now().toString()
        );

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(body));
    }
}
