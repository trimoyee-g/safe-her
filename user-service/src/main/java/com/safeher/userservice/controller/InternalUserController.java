package com.safeher.userservice.controller;

import com.safeher.userservice.dto.response.ApiResponse;
import com.safeher.userservice.dto.response.PublicUserResponse;
import com.safeher.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Internal endpoints consumed by other microservices via Feign.
 * These are NOT exposed on the public API Gateway — protected at the network/gateway layer.
 */
@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{id}/public")
    public PublicUserResponse getPublicProfile(@PathVariable UUID id) {
        return userService.getPublicProfile(id);
    }

    @GetMapping("/by-auth/{authUserId}")
    public ApiResponse<Boolean> existsByAuthUserId(@PathVariable UUID authUserId) {
        return ApiResponse.ok(userService.existsByAuthUserId(authUserId));
    }

    @PostMapping("/{id}/increment-reviews")
    public void incrementReviews(@PathVariable UUID id) {
        userService.incrementReviewCount(id);
    }

    @PostMapping("/{id}/increment-helpful-votes")
    public void incrementHelpfulVotes(@PathVariable UUID id) {
        userService.incrementHelpfulVotes(id);
    }
}
