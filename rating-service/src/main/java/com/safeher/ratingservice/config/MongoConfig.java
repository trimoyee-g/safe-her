package com.safeher.ratingservice.config;

import org.springframework.context.annotation.Configuration;

/**
 * MongoDB configuration.
 * No custom converters needed — Rating entity uses Instant, which is
 * natively supported by Spring Data MongoDB.
 */
@Configuration
public class MongoConfig {
}
