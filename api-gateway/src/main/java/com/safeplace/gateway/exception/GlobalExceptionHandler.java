package com.safeher.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@Component
@Order(-2)   // higher priority than DefaultErrorWebExceptionHandler
@Slf4j
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message    = "An unexpected gateway error occurred";

        if (ex instanceof ResponseStatusException rse) {
            status  = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : rse.getMessage();
        } else if (ex instanceof io.jsonwebtoken.JwtException) {
            status  = HttpStatus.UNAUTHORIZED;
            message = "Invalid token";
        } else if (ex instanceof java.net.ConnectException) {
            status  = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Upstream service unreachable";
        }

        log.error("Gateway error [{}]: {}", status, ex.getMessage());

        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");

        String body = """
                {"success":false,"message":"%s","traceId":"%s","timestamp":"%s"}
                """.formatted(
                message,
                traceId != null ? traceId : "n/a",
                OffsetDateTime.now()
        ).strip();

        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
