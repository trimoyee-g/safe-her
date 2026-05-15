package com.safeher.placeservice.entity;

import com.safeher.placeservice.enums.PlaceCategory;
import com.safeher.placeservice.enums.PlaceSource;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "places")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_id")
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PlaceSource source = PlaceSource.USER;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PlaceCategory category;

    @Column(name = "sub_category", length = 50)
    private String subCategory;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /**
     * PostGIS Point – stored as geometry(Point, 4326).
     * Access latitude via location.getY(), longitude via location.getX().
     */
    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point location;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(length = 500)
    private String website;

    @Column(name = "google_maps_url", length = 500)
    private String googleMapsUrl;

    @Type(JsonType.class)
    @Column(name = "opening_hours", columnDefinition = "jsonb")
    private Map<String, String> openingHours;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> photos;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> amenities;

    // ── Aggregated safety data (materialised via Kafka from Rating Service) ──

    @Column(name = "safety_score", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal safetyScore = BigDecimal.ZERO;

    @Column(name = "total_ratings", nullable = false)
    @Builder.Default
    private int totalRatings = 0;

    // ── Moderation ────────────────────────────────────────────────────────────

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(name = "reported_count", nullable = false)
    @Builder.Default
    private int reportedCount = 0;

    // ── Ownership ─────────────────────────────────────────────────────────────

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ── Convenience accessors for lat/lng ─────────────────────────────────────

    public double getLatitude()  { return location != null ? location.getY() : 0; }
    public double getLongitude() { return location != null ? location.getX() : 0; }
}
