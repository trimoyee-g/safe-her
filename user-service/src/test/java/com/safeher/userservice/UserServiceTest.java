package com.safeher.userservice;

import com.safeher.userservice.dto.request.CreateUserRequest;
import com.safeher.userservice.dto.request.UpdateUserRequest;
import com.safeher.userservice.dto.response.UserResponse;
import com.safeher.userservice.entity.User;
import com.safeher.userservice.enums.Role;
import com.safeher.userservice.event.UserEventPublisher;
import com.safeher.userservice.exception.DuplicateResourceException;
import com.safeher.userservice.exception.ForbiddenException;
import com.safeher.userservice.exception.UserNotFoundException;
import com.safeher.userservice.mapper.UserMapper;
import com.safeher.userservice.repository.UserRepository;
import com.safeher.userservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private UserEventPublisher eventPublisher;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private UUID authUserId;
    private User sampleUser;
    private UserResponse sampleResponse;

    @BeforeEach
    void setUp() {
        userId     = UUID.randomUUID();
        authUserId = UUID.randomUUID();

        sampleUser = User.builder()
                .id(userId)
                .authUserId(authUserId)
                .username("testuser")
                .email("test@example.com")
                .role(Role.USER)
                .active(true)
                .build();

        sampleResponse = UserResponse.builder()
                .id(userId)
                .authUserId(authUserId)
                .username("testuser")
                .email("test@example.com")
                .role(Role.USER)
                .build();
    }

    // ── createUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createUser – success path publishes event and returns response")
    void createUser_success() {
        CreateUserRequest req = new CreateUserRequest();
        req.setAuthUserId(authUserId);
        req.setUsername("testuser");
        req.setEmail("test@example.com");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(req.getUsername())).thenReturn(false);
        when(userRepository.existsByAuthUserId(req.getAuthUserId())).thenReturn(false);
        when(userMapper.toEntity(req)).thenReturn(sampleUser);
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);
        when(userMapper.toResponse(sampleUser)).thenReturn(sampleResponse);

        UserResponse result = userService.createUser(req);

        assertThat(result.getId()).isEqualTo(userId);
        verify(eventPublisher).publishUserCreated(any());
    }

    @Test
    @DisplayName("createUser – duplicate email throws DuplicateResourceException")
    void createUser_duplicateEmail_throws() {
        CreateUserRequest req = new CreateUserRequest();
        req.setAuthUserId(authUserId);
        req.setUsername("testuser");
        req.setEmail("duplicate@example.com");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already registered");
    }

    // ── getUserById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserById – found active user returns response")
    void getUserById_found() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userMapper.toResponse(sampleUser)).thenReturn(sampleResponse);

        UserResponse result = userService.getUserById(userId);

        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("getUserById – deactivated user throws UserNotFoundException")
    void getUserById_inactive_throws() {
        sampleUser.setActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("getUserById – missing user throws UserNotFoundException")
    void getUserById_missing_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ── updateUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateUser – owner can update own profile")
    void updateUser_ownerCanUpdate() {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setDisplayName("New Name");

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);
        when(userMapper.toResponse(sampleUser)).thenReturn(sampleResponse);

        // callerAuthId matches the user's authUserId
        assertThatCode(() -> userService.updateUser(userId, req, authUserId, Role.USER))
                .doesNotThrowAnyException();
        verify(eventPublisher).publishUserUpdated(any());
    }

    @Test
    @DisplayName("updateUser – admin can update any profile")
    void updateUser_adminCanUpdate() {
        UUID adminAuthId = UUID.randomUUID();
        UpdateUserRequest req = new UpdateUserRequest();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);
        when(userMapper.toResponse(sampleUser)).thenReturn(sampleResponse);

        assertThatCode(() -> userService.updateUser(userId, req, adminAuthId, Role.ADMIN))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("updateUser – stranger throws ForbiddenException")
    void updateUser_strangerForbidden() {
        UUID strangerId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> userService.updateUser(userId, new UpdateUserRequest(), strangerId, Role.USER))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteUser – owner deletes own account and event published")
    void deleteUser_ownerDeletes() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        userService.deleteUser(userId, authUserId, Role.USER);

        verify(userRepository).deactivateUser(userId);
        verify(eventPublisher).publishUserDeleted(any());
    }
}
