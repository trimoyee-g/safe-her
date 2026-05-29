package com.safeher.authservice.repository;

import com.safeher.authservice.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true, t.revokedAt = :revokedAt WHERE t.tokenHash = :hash")
    void revokeByTokenHash(@Param("hash") String hash, @Param("revokedAt") OffsetDateTime revokedAt);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true, t.revokedAt = :revokedAt WHERE t.authUser.id = :userId AND t.revoked = false")
    void revokeAllForUser(@Param("userId") UUID userId, @Param("revokedAt") OffsetDateTime revokedAt);

    /** Cleanup job – delete expired tokens older than a threshold */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :threshold")
    void deleteExpiredBefore(@Param("threshold") OffsetDateTime threshold);

    long countByAuthUserIdAndRevokedFalse(UUID authUserId);
}
