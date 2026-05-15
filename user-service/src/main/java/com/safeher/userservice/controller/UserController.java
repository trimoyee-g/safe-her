package com.safeher.userservice.controller;

import com.safeher.userservice.dto.request.CreateUserRequest;
import com.safeher.userservice.dto.request.UpdateAvatarRequest;
import com.safeher.userservice.dto.request.UpdateUserRequest;
import com.safeher.userservice.dto.response.ApiResponse;
import com.safeher.userservice.dto.response.PagedResponse;
import com.safeher.userservice.dto.response.PublicUserResponse;
import com.safeher.userservice.dto.response.UserResponse;
import com.safeher.userservice.enums.Role;
import com.safeher.userservice.service.UserService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Called by Auth Service (via Feign) immediately after registration.
     * Also directly callable from the API Gateway when a new user signs up.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok("User profile created", userService.createUser(request));
    }

    // ── Get self ──────────────────────────────────────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @RateLimiter(name = "user-api")
    public ApiResponse<UserResponse> getMe(Authentication auth) {
        UUID authUserId = (UUID) auth.getCredentials();
        return ApiResponse.ok(userService.getUserByAuthUserId(authUserId));
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @RateLimiter(name = "user-api")
    public ApiResponse<UserResponse> getById(@PathVariable UUID id) {
        return ApiResponse.ok(userService.getUserById(id));
    }

    @GetMapping("/by-username/{username}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserResponse> getByUsername(@PathVariable String username) {
        return ApiResponse.ok(userService.getUserByUsername(username));
    }

    // ── Public profiles (no auth required) ───────────────────────────────────

    @GetMapping("/public/{id}")
    @RateLimiter(name = "user-api")
    public ApiResponse<PublicUserResponse> getPublicProfile(@PathVariable UUID id) {
        return ApiResponse.ok(userService.getPublicProfile(id));
    }

    @GetMapping("/search")
    @RateLimiter(name = "user-api")
    public ApiResponse<PagedResponse<PublicUserResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(userService.searchUsers(q, page, size));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication auth) {
        UUID callerAuthId = (UUID) auth.getCredentials();
        Role callerRole   = extractRole(auth);
        return ApiResponse.ok("Profile updated", userService.updateUser(id, request, callerAuthId, callerRole));
    }

    @PatchMapping("/{id}/avatar")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserResponse> updateAvatar(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAvatarRequest request,
            Authentication auth) {
        UUID callerAuthId = (UUID) auth.getCredentials();
        Role callerRole   = extractRole(auth);
        return ApiResponse.ok("Avatar updated", userService.updateAvatar(id, request, callerAuthId, callerRole));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable UUID id,
            Authentication auth) {
        UUID callerAuthId = (UUID) auth.getCredentials();
        Role callerRole   = extractRole(auth);
        userService.deleteUser(id, callerAuthId, callerRole);
        return ResponseEntity.ok(ApiResponse.ok("User deleted", null));
    }

    // ── Admin endpoints ───────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PagedResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(userService.getAllUsers(page, size));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> changeRole(
            @PathVariable UUID id,
            @RequestParam Role role) {
        return ApiResponse.ok("Role updated", userService.changeRole(id, role));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> deactivate(@PathVariable UUID id) {
        return ApiResponse.ok("User deactivated", userService.deactivateUser(id));
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> reactivate(@PathVariable UUID id) {
        return ApiResponse.ok("User reactivated", userService.reactivateUser(id));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Role extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .map(Role::valueOf)
                .findFirst()
                .orElse(Role.USER);
    }
}
