package com.safeher.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateAvatarRequest {

    @NotBlank(message = "Avatar URL is required")
    private String avatarUrl;
}
