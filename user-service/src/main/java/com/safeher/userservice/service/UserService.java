package com.safeher.userservice.service;

import com.safeher.userservice.dto.request.CreateUserRequest;
import com.safeher.userservice.dto.request.UpdateAvatarRequest;
import com.safeher.userservice.dto.request.UpdateUserRequest;
import com.safeher.userservice.dto.response.PagedResponse;
import com.safeher.userservice.dto.response.PublicUserResponse;
import com.safeher.userservice.dto.response.UserResponse;
import com.safeher.userservice.enums.Role;

import java.util.UUID;

public interface UserService {

    // ── CRUD ──────────────────────────────────────────────────────────────────

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(UUID id);

    UserResponse getUserByAuthUserId(UUID authUserId);

    UserResponse getUserByUsername(String username);

    UserResponse updateUser(UUID id, UpdateUserRequest request, UUID callerAuthUserId, Role callerRole);

    UserResponse updateAvatar(UUID id, UpdateAvatarRequest request, UUID callerAuthUserId, Role callerRole);

    void deleteUser(UUID id, UUID callerAuthUserId, Role callerRole);

    // ── Public / search ───────────────────────────────────────────────────────

    PublicUserResponse getPublicProfile(UUID id);

    PagedResponse<PublicUserResponse> searchUsers(String query, int page, int size);

    PagedResponse<UserResponse> getAllUsers(int page, int size);

    // ── Admin ─────────────────────────────────────────────────────────────────

    UserResponse changeRole(UUID id, Role newRole);

    UserResponse deactivateUser(UUID id);

    UserResponse reactivateUser(UUID id);

    // ── Internal (called by other services via Feign) ─────────────────────────

    void incrementReviewCount(UUID userId);

    void incrementHelpfulVotes(UUID userId);

    boolean existsByAuthUserId(UUID authUserId);
}
