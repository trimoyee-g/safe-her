package com.safeher.authservice.security.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * When a user logs out, the access token is still cryptographically valid until
 * its expiry. We add it to a Redis blacklist so the gateway / downstream services
 * can quickly check revocation without re-parsing the JWT signature.
 *
 * Key format: "blacklist:<tokenHash>"  TTL = remaining token lifetime
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private static final String PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void blacklist(String token, long ttlMillis) {
        String key = PREFIX + tokenKey(token);
        Duration ttl = Duration.ofMillis(Math.max(ttlMillis, 1));
        redisTemplate.opsForValue().set(key, "revoked", ttl);
        log.debug("Blacklisted access token (ttl={}ms)", ttlMillis);
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + tokenKey(token)));
    }

    /** Use first 32 chars of raw token as Redis key component – collision risk negligible */
    private String tokenKey(String token) {
        return token.length() > 32 ? token.substring(0, 32) : token;
    }
}
