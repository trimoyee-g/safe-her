package com.safeher.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Belt-and-suspenders block for all /internal/** paths.
 * Even if a route somehow leaked through, this global filter stops it.
 */
@Component
@Slf4j
public class InternalRouteBlockingFilter implements GlobalFilter, Ordered {

    private static final String INTERNAL_PREFIX = "/api/v1/internal";

    @Override
    public int getOrder() {
        // Run just after logging filter but before route matching
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (path.startsWith(INTERNAL_PREFIX)) {
            log.warn("BLOCKED internal path attempt: {} from ip={}",
                    path,
                    exchange.getRequest().getRemoteAddress());

            var response = exchange.getResponse();
            response.setStatusCode(HttpStatus.FORBIDDEN);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            String body = """
                    {"success":false,"message":"Access to internal endpoints is forbidden"}
                    """.strip();

            var buffer = response.bufferFactory()
                    .wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }

        return chain.filter(exchange);
    }
}
