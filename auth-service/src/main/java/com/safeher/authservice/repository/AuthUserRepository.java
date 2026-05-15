package com.safeher.authservice.repository;

import com.safeher.authservice.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {

    Optional<AuthUser> findByEmail(String email);
    Optional<AuthUser> findByUsername(String username);
    Optional<AuthUser> findByEmailOrUsername(String email, String username);
    Optional<AuthUser> findByResetToken(String resetToken);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    @Modifying
    @Query("""
        UPDATE AuthUser u SET
            u.failedLoginAttempts = u.failedLoginAttempts + 1,
            u.lockedUntil = CASE
                WHEN u.failedLoginAttempts + 1 >= :maxAttempts
                THEN :lockUntil ELSE u.lockedUntil END
        WHERE u.id = :id
        """)
    void incrementFailedAttempts(@Param("id") UUID id,
                                 @Param("maxAttempts") int maxAttempts,
                                 @Param("lockUntil") OffsetDateTime lockUntil);

    @Modifying
    @Query("UPDATE AuthUser u SET u.failedLoginAttempts = 0, u.lockedUntil = null, u.lastLoginAt = :now WHERE u.id = :id")
    void resetFailedAttempts(@Param("id") UUID id, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("UPDATE AuthUser u SET u.active = false WHERE u.id = :id")
    void deactivate(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE AuthUser u SET u.emailVerified = true WHERE u.id = :id")
    void markEmailVerified(@Param("id") UUID id);
}
