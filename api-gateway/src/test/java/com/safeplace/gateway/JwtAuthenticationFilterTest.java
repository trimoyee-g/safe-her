package com.safeher.gateway;

import com.safeher.gateway.filter.JwtAuthenticationFilter;
import com.safeher.gateway.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtUtils jwtUtils;
    @Mock private GatewayFilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtUtils);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Missing Authorization header → 401")
    void missingHeader_returns401() {
        var request  = MockServerHttpRequest.get("/api/v1/users/me").build();
        var exchange = MockServerWebExchange.from(request);
        var gf       = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Invalid JWT → 401")
    void invalidToken_returns401() {
        when(jwtUtils.isValid("bad.token")).thenReturn(false);

        var request  = MockServerHttpRequest.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer bad.token")
                .build();
        var exchange = MockServerWebExchange.from(request);
        var gf       = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Valid JWT → downstream headers injected, chain called")
    void validToken_forwardsHeaders() {
        String token = "valid.jwt.token";
        when(jwtUtils.isValid(token)).thenReturn(true);
        when(jwtUtils.extractUserId(token)).thenReturn("user-uuid-123");
        when(jwtUtils.extractUsername(token)).thenReturn("testuser");
        when(jwtUtils.extractRole(token)).thenReturn("USER");
        when(jwtUtils.extractEmail(token)).thenReturn("test@example.com");

        var request  = MockServerHttpRequest.get("/api/v1/places")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        var exchange = MockServerWebExchange.from(request);
        var gf       = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain)).verifyComplete();

        // chain must have been called with mutated request
        verify(chain, times(1)).filter(argThat(ex -> {
            HttpHeaders headers = ex.getRequest().getHeaders();
            return "user-uuid-123".equals(headers.getFirst("X-Auth-UserId"))
                && "testuser".equals(headers.getFirst("X-Auth-Username"))
                && "USER".equals(headers.getFirst("X-Auth-Role"))
                && "test@example.com".equals(headers.getFirst("X-Auth-Email"));
        }));
    }

    @Test
    @DisplayName("Bearer prefix missing → 401")
    void noBearerPrefix_returns401() {
        var request  = MockServerHttpRequest.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                .build();
        var exchange = MockServerWebExchange.from(request);
        var gf       = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
