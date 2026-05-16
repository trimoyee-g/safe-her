package com.safeher.placeservice.repository.jpa;

import com.safeher.placeservice.entity.Place;
import com.safeher.placeservice.enums.PlaceCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaceRepository extends JpaRepository<Place, UUID> {

    Optional<Place> findByIdAndActiveTrue(UUID id);

    boolean existsByExternalIdAndSource(String externalId, String source);

    // ── Geo-search (PostGIS ST_DWithin, SRID 4326 uses degrees – convert radius to metres) ──

    @Query(value = """
            SELECT p.* FROM places p
            WHERE p.is_active = true
              AND ST_DWithin(
                    p.location::geography,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                    :radiusMeters
                  )
            ORDER BY ST_Distance(
                p.location::geography,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
            )
            """,
            countQuery = """
            SELECT COUNT(*) FROM places p
            WHERE p.is_active = true
              AND ST_DWithin(
                    p.location::geography,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                    :radiusMeters
                  )
            """,
            nativeQuery = true)
    Page<Place> findWithinRadius(@Param("lat") double lat,
                                 @Param("lng") double lng,
                                 @Param("radiusMeters") double radiusMeters,
                                 Pageable pageable);

    @Query(value = """
            SELECT p.* FROM places p
            WHERE p.is_active = true
              AND p.category = :category
              AND ST_DWithin(
                    p.location::geography,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                    :radiusMeters
                  )
            ORDER BY ST_Distance(
                p.location::geography,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
            )
            """,
            countQuery = """
            SELECT COUNT(*) FROM places p
            WHERE p.is_active = true AND p.category = :category
              AND ST_DWithin(
                    p.location::geography,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                    :radiusMeters
                  )
            """,
            nativeQuery = true)
    Page<Place> findWithinRadiusByCategory(@Param("lat") double lat,
                                           @Param("lng") double lng,
                                           @Param("radiusMeters") double radiusMeters,
                                           @Param("category") String category,
                                           Pageable pageable);

    // ── Safety score update (called on Kafka rating.created event) ────────────

    @Modifying
    @Query("""
            UPDATE Place p
            SET p.safetyScore  = :newScore,
                p.totalRatings = :totalRatings
            WHERE p.id = :id
            """)
    void updateSafetyScore(@Param("id") UUID id,
                           @Param("newScore") BigDecimal newScore,
                           @Param("totalRatings") int totalRatings);

    @Modifying
    @Query("UPDATE Place p SET p.description = :description WHERE p.id = :id")
    void updateDescription(@Param("id") UUID id, @Param("description") String description);

    @Modifying
    @Query("""
            UPDATE Place p
            SET p.aiSummary = :summary,
                p.aiSummaryGeneratedAt = :generatedAt
            WHERE p.id = :id
            """)
    void updateAiSummary(@Param("id") UUID id,
                         @Param("summary") String summary,
                         @Param("generatedAt") OffsetDateTime generatedAt);

    @Modifying
    @Query("UPDATE Place p SET p.reportedCount = p.reportedCount + 1 WHERE p.id = :id")
    void incrementReportCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Place p SET p.active = false WHERE p.id = :id")
    void deactivate(@Param("id") UUID id);

    Page<Place> findAllByActiveTrue(Pageable pageable);

    Page<Place> findAllByActiveTrueAndCategory(PlaceCategory category, Pageable pageable);

    Page<Place> findAllByCreatedByAndActiveTrue(UUID createdBy, Pageable pageable);

    List<Place> findTop10ByActiveTrueOrderBySafetyScoreDesc();

    @Query("SELECT p FROM Place p WHERE p.id = :id AND p.active = true")
    java.util.Optional<Place> findActiveById(@Param("id") UUID id);
}

