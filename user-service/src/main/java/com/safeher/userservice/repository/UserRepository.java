package com.safeher.userservice.repository;

import com.safeher.userservice.entity.User;
import com.safeher.userservice.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByAuthUserId(UUID authUserId);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByAuthUserId(UUID authUserId);

    Page<User> findAllByActiveTrue(Pageable pageable);

    Page<User> findAllByRole(Role role, Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE u.active = true
              AND (
                  LOWER(u.username)    LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(u.city)        LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<User> searchUsers(@Param("q") String query, Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.totalReviews = u.totalReviews + 1 WHERE u.id = :id")
    void incrementTotalReviews(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE User u SET u.helpfulVotes = u.helpfulVotes + 1 WHERE u.id = :id")
    void incrementHelpfulVotes(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE User u SET u.active = false WHERE u.id = :id")
    void deactivateUser(@Param("id") UUID id);
}
