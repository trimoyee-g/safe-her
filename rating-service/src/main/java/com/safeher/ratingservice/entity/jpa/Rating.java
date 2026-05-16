package com.safeher.ratingservice.entity.jpa;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core review/rating document stored in MongoDB.
 *
 * One document per user per place. When a user edits their review the
 * document is updated in-place (not versioned – history is out of scope).
 *
 * Anonymous posting: when anonymous=true, userId is still stored for
 * moderation/deduplication but is NEVER returned in any response DTO.
 */
@Document(collection = "ratings")
@CompoundIndexes({
    @CompoundIndex(name = "idx_place_user",   def = "{'placeId': 1, 'userId': 1}", unique = true),
    @CompoundIndex(name = "idx_place_active", def = "{'placeId': 1, 'active': 1}"),
    @CompoundIndex(name = "idx_user_active",  def = "{'userId': 1,  'active': 1}")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Rating {

    @Id
    private String id;               // MongoDB ObjectId string

    @Indexed
    @Field("place_id")
    private UUID placeId;

    @Field("user_id")
    private UUID userId;             // always stored; masked in responses when anonymous

    /** Display name snapshot at review time (denormalised to avoid User Service calls at read) */
    @Field("author_display_name")
    private String authorDisplayName;

    @Field("author_avatar_url")
    private String authorAvatarUrl;

    /** 1–5 safety score */
    private int score;

    private String title;

    private String body;

    @Builder.Default
    private List<String> photos = new ArrayList<>();

    @Builder.Default
    private List<String> tags = new ArrayList<>();   // ["well-lit","cctv","busy-street"]

    /** True = authorDisplayName is replaced with "Anonymous" in all responses */
    @Builder.Default
    private boolean anonymous = false;

    @Builder.Default
    private boolean active = true;

    @Field("helpful_count")
    @Builder.Default
    private int helpfulCount = 0;

    /** User IDs who marked this review helpful – capped at helpfulCount */
    @Field("helpful_voters")
    @Builder.Default
    private List<String> helpfulVoters = new ArrayList<>();

    @Field("reported_count")
    @Builder.Default
    private int reportedCount = 0;

    @Field("reported_by")
    @Builder.Default
    private List<String> reportedBy = new ArrayList<>();

    // ── AI Moderation ─────────────────────────────────────────────────────────

    /** True when auto-suppressed by the ReviewModerationAgent (active set to false). */
    @Field("suppressed")
    @Builder.Default
    private boolean suppressed = false;

    /** True when flagged for human review by an AI agent. */
    @Field("flagged")
    @Builder.Default
    private boolean flagged = false;

    @Field("flag_reason")
    private String flagReason;

    @Field("flag_confidence")
    @Builder.Default
    private double flagConfidence = 0.0;

    @CreatedDate
    @Field("created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private OffsetDateTime updatedAt;
}
