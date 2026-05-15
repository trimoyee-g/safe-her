package com.safeher.userservice.service.impl;

import com.safeher.userservice.dto.request.CreateUserRequest;
import com.safeher.userservice.dto.request.UpdateAvatarRequest;
import com.safeher.userservice.dto.request.UpdateUserRequest;
import com.safeher.userservice.dto.response.PagedResponse;
import com.safeher.userservice.dto.response.PublicUserResponse;
import com.safeher.userservice.dto.response.UserResponse;
import com.safeher.userservice.entity.User;
import com.safeher.userservice.enums.Role;
import com.safeher.userservice.event.UserCreatedEvent;
import com.safeher.userservice.event.UserDeletedEvent;
import com.safeher.userservice.event.UserEventPublisher;
import com.safeher.userservice.event.UserUpdatedEvent;
import com.safeher.userservice.exception.DuplicateResourceException;
import com.safeher.userservice.exception.ForbiddenException;
import com.safeher.userservice.exception.UserNotFoundException;
import com.safeher.userservice.mapper.UserMapper;
import com.safeher.userservice.repository.UserRepository;
import com.safeher.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserEventPublisher eventPublisher;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByAuthUserId(request.getAuthUserId())) {
            throw new DuplicateResourceException("Profile already exists for authUserId: " + request.getAuthUserId());
        }

        User user = userMapper.toEntity(request);
        User saved = userRepository.save(user);
        log.info("Created user profile [id={}] for authUserId={}", saved.getId(), saved.getAuthUserId());

        eventPublisher.publishUserCreated(UserCreatedEvent.builder()
                .userId(saved.getId())
                .authUserId(saved.getAuthUserId())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .role(saved.getRole().name())
                .build());

        return userMapper.toResponse(saved);
    }

    @Override
    public UserResponse getUserById(UUID id) {
        return userMapper.toResponse(findActiveUserById(id));
    }

    @Override
    public UserResponse getUserByAuthUserId(UUID authUserId) {
        User user = userRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found for authUserId: " + authUserId));
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request,
                                   UUID callerAuthUserId, Role callerRole) {
        User user = findActiveUserById(id);
        assertOwnerOrAdmin(user.getAuthUserId(), callerAuthUserId, callerRole, "update this profile");

        userMapper.updateEntity(request, user);
        User saved = userRepository.save(user);
        log.info("Updated user profile [id={}]", saved.getId());

        eventPublisher.publishUserUpdated(UserUpdatedEvent.builder()
                .userId(saved.getId())
                .username(saved.getUsername())
                .displayName(saved.getDisplayName())
                .avatarUrl(saved.getAvatarUrl())
                .build());

        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateAvatar(UUID id, UpdateAvatarRequest request,
                                     UUID callerAuthUserId, Role callerRole) {
        User user = findActiveUserById(id);
        assertOwnerOrAdmin(user.getAuthUserId(), callerAuthUserId, callerRole, "update this avatar");

        user.setAvatarUrl(request.getAvatarUrl());
        User saved = userRepository.save(user);

        eventPublisher.publishUserUpdated(UserUpdatedEvent.builder()
                .userId(saved.getId())
                .username(saved.getUsername())
                .displayName(saved.getDisplayName())
                .avatarUrl(saved.getAvatarUrl())
                .build());

        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id, UUID callerAuthUserId, Role callerRole) {
        User user = findActiveUserById(id);
        assertOwnerOrAdmin(user.getAuthUserId(), callerAuthUserId, callerRole, "delete this profile");

        userRepository.deactivateUser(id);
        log.info("Deactivated (soft-deleted) user [id={}]", id);

        eventPublisher.publishUserDeleted(UserDeletedEvent.builder()
                .userId(user.getId())
                .authUserId(user.getAuthUserId())
                .build());
    }

    // ── Public / search ───────────────────────────────────────────────────────

    @Override
    public PublicUserResponse getPublicProfile(UUID id) {
        User user = userRepository.findById(id)
                .filter(User::isActive)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
        return userMapper.toPublicResponse(user);
    }

    @Override
    public PagedResponse<PublicUserResponse> searchUsers(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
        Page<User> result = userRepository.searchUsers(query, pageable);
        return toPagedResponse(result.map(userMapper::toPublicResponse));
    }

    @Override
    public PagedResponse<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> result = userRepository.findAllByActiveTrue(pageable);
        return toPagedResponse(result.map(userMapper::toResponse));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UserResponse changeRole(UUID id, Role newRole) {
        User user = findActiveUserById(id);
        user.setRole(newRole);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse deactivateUser(UUID id) {
        User user = findActiveUserById(id);
        user.setActive(false);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse reactivateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
        user.setActive(true);
        return userMapper.toResponse(userRepository.save(user));
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void incrementReviewCount(UUID userId) {
        userRepository.incrementTotalReviews(userId);
    }

    @Override
    @Transactional
    public void incrementHelpfulVotes(UUID userId) {
        userRepository.incrementHelpfulVotes(userId);
    }

    @Override
    public boolean existsByAuthUserId(UUID authUserId) {
        return userRepository.existsByAuthUserId(authUserId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User findActiveUserById(UUID id) {
        return userRepository.findById(id)
                .filter(User::isActive)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }

    private void assertOwnerOrAdmin(UUID resourceOwnerAuthId, UUID callerAuthId,
                                    Role callerRole, String action) {
        boolean isOwner = resourceOwnerAuthId.equals(callerAuthId);
        boolean isAdmin = Role.ADMIN.equals(callerRole) || Role.MODERATOR.equals(callerRole);
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You are not authorised to " + action);
        }
    }

    private <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
