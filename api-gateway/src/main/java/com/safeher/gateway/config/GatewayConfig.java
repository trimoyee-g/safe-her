package com.safeher.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class GatewayConfig {

    /**
     * Default Redis rate limiter shared across routes.
     * Individual routes override replenishRate/burstCapacity in application.yml.
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // replenishRate=50 req/s, burstCapacity=100, requestedTokens=1
        return new RedisRateLimiter(50, 100, 1);
    }

    /**
     * Explicit CORS filter (belt-and-suspenders alongside the globalcors config).
     * Required for preflight OPTIONS requests to pass through correctly.
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
