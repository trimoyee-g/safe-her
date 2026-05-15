package com.safeher.authservice.service;

import com.safeher.authservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    /** Runs daily at 02:00 – deletes refresh tokens expired more than 7 days ago */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(7);
        refreshTokenRepository.deleteExpiredBefore(threshold);
        log.info("Purged expired refresh tokens older than {}", threshold);
    }
}
