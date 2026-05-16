package com.safeher.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Runs on EVERY request (GlobalFilter).
 * - Injects a unique X-Trace-Id header for distributed tracing
 * - Logs method + path + status + latency
 * - Highest precedence (runs before route filters)
 */
@Component
@Slf4j
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();

        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String clientIp = getClientIp(exchange.getRequest());

        // Inject trace ID so downstream services can log it
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header("X-Trace-Id", traceId)
                .header("X-Forwarded-For", clientIp)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutated).build();

        log.info("→ [{} ] {} {} ip=[{}]",
                traceId,
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath().value(),
                clientIp);

        return chain.filter(mutatedExchange)
                .doFinally(signal -> {
                    ServerHttpResponse response = mutatedExchange.getResponse();
                    long latency = System.currentTimeMillis() - start;
                    log.info("← [{}] {} {} status={} latency={}ms",
                            traceId,
                            exchange.getRequest().getMethod(),
                            exchange.getRequest().getPath().value(),
                            response.getStatusCode(),
                            latency);
                });
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }
}
