package com.safeher.ratingservice.mapper;

import com.safeher.ratingservice.dto.request.CreateRatingRequest;
import com.safeher.ratingservice.dto.request.UpdateRatingRequest;
import com.safeher.ratingservice.dto.response.RatingResponse;
import com.safeher.ratingservice.entity.document.RatingDocument;
import com.safeher.ratingservice.entity.jpa.Rating;
import org.mapstruct.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface RatingMapper {

    // ── Instant → OffsetDateTime (UTC) for response DTOs ──────────────────────

    default OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    // ── Entity from request ────────────────────────────────────────────────────

    @Mapping(target = "id",              ignore = true)
    @Mapping(target = "userId",          ignore = true)
    @Mapping(target = "authorDisplayName", ignore = true)
    @Mapping(target = "authorAvatarUrl", ignore = true)
    @Mapping(target = "helpfulCount",    ignore = true)
    @Mapping(target = "helpfulVoters",   ignore = true)
    @Mapping(target = "reportedCount",   ignore = true)
    @Mapping(target = "reportedBy",      ignore = true)
    @Mapping(target = "active",          ignore = true)
    @Mapping(target = "suppressed",      ignore = true)
    @Mapping(target = "flagged",         ignore = true)
    @Mapping(target = "flagReason",      ignore = true)
    @Mapping(target = "flagConfidence",  ignore = true)
    @Mapping(target = "createdAt",       ignore = true)
    @Mapping(target = "updatedAt",       ignore = true)
    Rating toEntity(CreateRatingRequest request);

    // ── Partial update ─────────────────────────────────────────────────────────

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",              ignore = true)
    @Mapping(target = "placeId",         ignore = true)
    @Mapping(target = "userId",          ignore = true)
    @Mapping(target = "authorDisplayName", ignore = true)
    @Mapping(target = "authorAvatarUrl", ignore = true)
    @Mapping(target = "helpfulCount",    ignore = true)
    @Mapping(target = "helpfulVoters",   ignore = true)
    @Mapping(target = "reportedCount",   ignore = true)
    @Mapping(target = "reportedBy",      ignore = true)
    @Mapping(target = "active",          ignore = true)
    @Mapping(target = "suppressed",      ignore = true)
    @Mapping(target = "flagged",         ignore = true)
    @Mapping(target = "flagReason",      ignore = true)
    @Mapping(target = "flagConfidence",  ignore = true)
    @Mapping(target = "createdAt",       ignore = true)
    @Mapping(target = "updatedAt",       ignore = true)
    void updateEntity(UpdateRatingRequest request, @MappingTarget Rating rating);

    // ── Entity → response (caller may or may not be the author/admin) ──────────

    /**
     * Full response (admin or author viewing their own review).
     * UserId and reportedCount included.
     */
    @Mapping(target = "markedHelpfulByMe", ignore = true)
    @Mapping(target = "createdAt", expression = "java(toOffsetDateTime(rating.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toOffsetDateTime(rating.getUpdatedAt()))")
    @Mapping(target = "authorDisplayName",
             expression = "java(rating.isAnonymous() ? \"Anonymous\" : rating.getAuthorDisplayName())")
    @Mapping(target = "authorAvatarUrl",
             expression = "java(rating.isAnonymous() ? null : rating.getAuthorAvatarUrl())")
    RatingResponse toResponse(Rating rating);

    /**
     * Public response (non-author/non-admin).
     * Masks userId and reportedCount.
     */
    @Mapping(target = "userId",           ignore = true)
    @Mapping(target = "reportedCount",    ignore = true)
    @Mapping(target = "suppressed",       ignore = true)
    @Mapping(target = "flagged",          ignore = true)
    @Mapping(target = "flagReason",       ignore = true)
    @Mapping(target = "flagConfidence",   ignore = true)
    @Mapping(target = "markedHelpfulByMe", ignore = true)
    @Mapping(target = "createdAt", expression = "java(toOffsetDateTime(rating.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toOffsetDateTime(rating.getUpdatedAt()))")
    @Mapping(target = "authorDisplayName",
             expression = "java(rating.isAnonymous() ? \"Anonymous\" : rating.getAuthorDisplayName())")
    @Mapping(target = "authorAvatarUrl",
             expression = "java(rating.isAnonymous() ? null : rating.getAuthorAvatarUrl())")
    RatingResponse toPublicResponse(Rating rating);

    // ── Entity → ES document ──────────────────────────────────────────────────

    @Mapping(target = "placeId", expression = "java(rating.getPlaceId().toString())")
    @Mapping(target = "userId",  expression = "java(rating.getUserId().toString())")
    RatingDocument toDocument(Rating rating);
}
