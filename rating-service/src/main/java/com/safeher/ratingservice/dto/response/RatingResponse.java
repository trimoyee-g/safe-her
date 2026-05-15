package com.safeher.ratingservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RatingResponse {

    private String id;
    private UUID placeId;

    /**
     * Null when anonymous=true and caller is not the author or an admin.
     * The field is omitted from JSON entirely via @JsonInclude.
     */
    private UUID userId;

    private String authorDisplayName;   // "Anonymous" when anonymous=true
    private String authorAvatarUrl;     // null when anonymous=true
    private int score;
    private String title;
    private String body;
    private List<String> photos;
    private List<String> tags;
    private boolean anonymous;
    private int helpfulCount;
    private boolean markedHelpfulByMe;  // populated when caller is authenticated
    private int reportedCount;          // admin-only
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
