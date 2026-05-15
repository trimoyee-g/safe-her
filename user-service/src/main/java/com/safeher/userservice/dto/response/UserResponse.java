package com.safeher.userservice.dto.response;

import com.safeher.userservice.enums.Gender;
import com.safeher.userservice.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {

    private UUID id;
    private UUID authUserId;
    private String username;
    private String email;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String phoneNumber;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String city;
    private String country;
    private Double latitude;
    private Double longitude;
    private Role role;
    private boolean anonymous;
    private boolean active;
    private boolean verified;
    private int totalReviews;
    private int helpfulVotes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime lastSeenAt;
}
