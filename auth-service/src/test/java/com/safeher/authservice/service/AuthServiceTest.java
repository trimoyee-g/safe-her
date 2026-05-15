package com.safeher.authservice.service;

import com.safeher.authservice.client.UserServiceClient;
import com.safeher.authservice.dto.request.LoginRequest;
import com.safeher.authservice.dto.request.RegisterRequest;
import com.safeher.authservice.dto.response.AuthResponse;
import com.safeher.authservice.entity.AuthUser;
import com.safeher.authservice.enums.Role;
import com.safeher.authservice.event.AuthEventPublisher;
import com.safeher.authservice.exception.AccountLockedException;
import com.safeher.authservice.exception.AuthException;
import com.safeher.authservice.exception.DuplicateResourceException;
import com.safeher.authservice.repository.AuthUserRepository;
import com.safeher.authservice.repository.RefreshTokenRepository;
import com.safeher.authservice.security.token.JwtService;
import com.safeher.authservice.security.token.TokenBlacklistService;
import com.safeher.authservice.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthUserRepository     authUserRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtService             jwtService;
    @Mock PasswordEncoder        passwordEncoder;
    @Mock TokenBlacklistService  blacklistService;
    @Mock AuthEventPublisher     eventPublisher;
    @Mock UserServiceClient      userServiceClient;

    @InjectMocks AuthServiceImpl authService;

    private AuthUser sampleUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "maxLoginAttempts", 5);
        ReflectionTestUtils.setField(authService, "lockoutMinutes", 15);
        ReflectionTestUtils.setField(authService, "resetExpiryMinutes", 30);

        sampleUser = AuthUser.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$12$hashed")
                .role(Role.USER)
                .active(true)
                .failedLoginAttempts(0)
                .build();
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register – success returns AuthResponse with tokens")
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setEmail("test@example.com");
        req.setPassword("Test@1234");

        when(authUserRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(authUserRepository.existsByUsername(req.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(req.getPassword())).thenReturn("$2a$12$hashed");
        when(authUserRepository.save(any())).thenReturn(sampleUser);
        when(jwtService.generateAccessToken(sampleUser)).thenReturn("access.token");
        when(jwtService.generateRefreshToken(sampleUser)).thenReturn("refresh.token");
        when(jwtService.getRefreshTokenExpiryMs()).thenReturn(604800000L);
        when(jwtService.getAccessTokenExpiryMs()).thenReturn(86400000L);

        AuthResponse result = authService.register(req);

        assertThat(result.getAccessToken()).isEqualTo("access.token");
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(eventPublisher).publishUserRegistered(any());
        verify(refreshTokenRepository).save(any());
    }

    @Test
    @DisplayName("register – duplicate email throws DuplicateResourceException")
    void register_duplicateEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("dup@example.com");
        req.setUsername("newuser");

        when(authUserRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already registered");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login – valid credentials returns tokens")
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setIdentifier("test@example.com");
        req.setPassword("Test@1234");

        when(authUserRepository.findByEmailOrUsername(req.getIdentifier(), req.getIdentifier()))
                .thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches(req.getPassword(), sampleUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(sampleUser)).thenReturn("access.token");
        when(jwtService.generateRefreshToken(sampleUser)).thenReturn("refresh.token");
        when(jwtService.getRefreshTokenExpiryMs()).thenReturn(604800000L);
        when(jwtService.getAccessTokenExpiryMs()).thenReturn(86400000L);

        AuthResponse result = authService.login(req, "127.0.0.1");

        assertThat(result.getAccessToken()).isEqualTo("access.token");
        verify(authUserRepository).resetFailedAttempts(eq(sampleUser.getId()), any());
    }

    @Test
    @DisplayName("login – wrong password increments failed attempts")
    void login_wrongPassword_incrementsAttempts() {
        LoginRequest req = new LoginRequest();
        req.setIdentifier("test@example.com");
        req.setPassword("WrongPass1!");

        when(authUserRepository.findByEmailOrUsername(any(), any()))
                .thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Invalid credentials");

        verify(authUserRepository).incrementFailedAttempts(eq(sampleUser.getId()), anyInt(), any());
    }

    @Test
    @DisplayName("login – locked account throws AccountLockedException")
    void login_lockedAccount_throws() {
        sampleUser.setLockedUntil(OffsetDateTime.now().plusMinutes(10));

        LoginRequest req = new LoginRequest();
        req.setIdentifier("test@example.com");
        req.setPassword("AnyPass1!");

        when(authUserRepository.findByEmailOrUsername(any(), any()))
                .thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    @DisplayName("login – unknown user throws AuthException")
    void login_unknownUser_throws() {
        LoginRequest req = new LoginRequest();
        req.setIdentifier("nobody@example.com");
        req.setPassword("Pass@123");

        when(authUserRepository.findByEmailOrUsername(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(AuthException.class);
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("logout – valid access token is blacklisted")
    void logout_blacklistsToken() {
        String access  = "valid.access.token";
        String refresh = "valid.refresh.token";

        when(jwtService.isValid(access)).thenReturn(true);
        when(jwtService.getAccessTokenExpiryMs()).thenReturn(86400000L);

        authService.logout(access, refresh);

        verify(blacklistService).blacklist(eq(access), eq(86400000L));
        verify(refreshTokenRepository).revokeByTokenHash(any());
    }
}
