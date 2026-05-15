package com.safeher.authservice.service.impl;

import com.safeher.authservice.client.UserServiceClient;
import com.safeher.authservice.dto.request.*;
import com.safeher.authservice.dto.response.AuthResponse;
import com.safeher.authservice.entity.AuthUser;
import com.safeher.authservice.entity.RefreshToken;
import com.safeher.authservice.enums.Role;
import com.safeher.authservice.event.*;
import com.safeher.authservice.exception.*;
import com.safeher.authservice.repository.AuthUserRepository;
import com.safeher.authservice.repository.RefreshTokenRepository;
import com.safeher.authservice.security.token.JwtService;
import com.safeher.authservice.security.token.TokenBlacklistService;
import com.safeher.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final AuthUserRepository      authUserRepository;
    private final RefreshTokenRepository  refreshTokenRepository;
    private final JwtService              jwtService;
    private final PasswordEncoder         passwordEncoder;
    private final TokenBlacklistService   blacklistService;
    private final AuthEventPublisher      eventPublisher;
    private final UserServiceClient       userServiceClient;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.lockout-duration-minutes:15}")
    private int lockoutMinutes;

    @Value("${app.security.password-reset-expiry-minutes:30}")
    private int resetExpiryMinutes;

    // ── Register ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (authUserRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }
        if (authUserRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken");
        }

        AuthUser user = AuthUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        AuthUser saved = authUserRepository.save(user);
        log.info("Registered new user [id={}] email=[{}]", saved.getId(), saved.getEmail());

        // ── Publish Kafka event (User Service picks this up to create a profile) ──
        eventPublisher.publishUserRegistered(AuthUserRegisteredEvent.builder()
                .authUserId(saved.getId())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .role(saved.getRole().name())
                .displayName(request.getDisplayName())
                .city(request.getCity())
                .country(request.getCountry())
                .build());

        // ── Also call User Service directly via Feign (dual write for immediacy) ──
        // If Feign fails the fallback logs a warning; Kafka event ensures eventual consistency
        try {
            userServiceClient.createUserProfile(
                    UserServiceClient.CreateProfileRequest.builder()
                            .authUserId(saved.getId())
                            .username(saved.getUsername())
                            .email(saved.getEmail())
                            .displayName(request.getDisplayName())
                            .city(request.getCity())
                            .country(request.getCountry())
                            .build());
        } catch (Exception ex) {
            log.warn("Direct Feign call to User Service failed during registration (Kafka fallback active): {}", ex.getMessage());
        }

        String accessToken  = jwtService.generateAccessToken(saved);
        String refreshToken = jwtService.generateRefreshToken(saved);
        storeRefreshToken(saved, refreshToken, null, null);

        return buildAuthResponse(saved, accessToken, refreshToken);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String clientIp) {
        AuthUser user = authUserRepository
                .findByEmailOrUsername(request.getIdentifier(), request.getIdentifier())
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        if (!user.isActive()) {
            throw new AuthException("Account is deactivated");
        }

        if (user.isLocked()) {
            throw new AccountLockedException(
                    "Account locked until " + user.getLockedUntil() + ". Too many failed attempts.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            authUserRepository.incrementFailedAttempts(
                    user.getId(),
                    maxLoginAttempts,
                    OffsetDateTime.now().plusMinutes(lockoutMinutes));

            int remaining = Math.max(0, maxLoginAttempts - user.getFailedLoginAttempts() - 1);
            if (remaining == 0) {
                throw new AccountLockedException("Too many failed attempts. Account locked for " + lockoutMinutes + " minutes.");
            }
            throw new AuthException("Invalid credentials. " + remaining + " attempt(s) remaining.");
        }

        // Credentials valid – reset counter
        authUserRepository.resetFailedAttempts(user.getId(), OffsetDateTime.now());

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        storeRefreshToken(user, refreshToken, request.getDeviceInfo(), clientIp);

        log.info("User [{}] logged in from ip=[{}]", user.getUsername(), clientIp);
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, String clientIp) {
        String rawToken = request.getRefreshToken();

        if (!jwtService.isValid(rawToken) || !jwtService.isRefreshToken(rawToken)) {
            throw new TokenException("Invalid refresh token");
        }

        String hash = sha256(rawToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new TokenException("Refresh token not found"));

        if (!stored.isValid()) {
            throw new TokenException(stored.isRevoked() ? "Refresh token has been revoked" : "Refresh token has expired");
        }

        AuthUser user = stored.getAuthUser();

        // Rotate: revoke old, issue new
        refreshTokenRepository.revokeByTokenHash(hash);

        String newAccess  = jwtService.generateAccessToken(user);
        String newRefresh = jwtService.generateRefreshToken(user);
        storeRefreshToken(user, newRefresh, stored.getDeviceInfo(), clientIp);

        log.debug("Rotated refresh token for user [{}]", user.getUsername());
        return buildAuthResponse(user, newAccess, newRefresh);
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        // Blacklist access token for its remaining TTL
        if (StringUtils.hasText(accessToken) && jwtService.isValid(accessToken)) {
            long expiry = jwtService.getAccessTokenExpiryMs();
            blacklistService.blacklist(accessToken, expiry);
        }
        // Revoke refresh token
        if (StringUtils.hasText(refreshToken)) {
            refreshTokenRepository.revokeByTokenHash(sha256(refreshToken));
        }
    }

    @Override
    @Transactional
    public void logoutAllDevices(UUID authUserId) {
        refreshTokenRepository.revokeAllForUser(authUserId);
        log.info("Revoked all refresh tokens for user [{}]", authUserId);
    }

    // ── Password management ───────────────────────────────────────────────────

    @Override
    @Transactional
    public void changePassword(UUID authUserId, ChangePasswordRequest request) {
        AuthUser user = authUserRepository.findById(authUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AuthException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        authUserRepository.save(user);

        // Revoke all refresh tokens – forces re-login on all devices
        refreshTokenRepository.revokeAllForUser(authUserId);

        eventPublisher.publishPasswordChanged(
                AuthPasswordChangedEvent.builder().authUserId(authUserId).build());

        log.info("Password changed for user [{}]", authUserId);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always return success to prevent email enumeration
        authUserRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token  = UUID.randomUUID().toString();
            user.setResetToken(passwordEncoder.encode(token));
            user.setResetTokenExpiry(OffsetDateTime.now().plusMinutes(resetExpiryMinutes));
            authUserRepository.save(user);

            // TODO: send email via notification service
            // For now log it (remove in production!)
            log.info("[DEV ONLY] Password reset token for [{}]: {}", user.getEmail(), token);
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Find by matching reset token (raw token matches hashed stored value)
        AuthUser user = authUserRepository.findAll().stream()
                .filter(u -> u.getResetToken() != null
                          && u.getResetTokenExpiry() != null
                          && u.getResetTokenExpiry().isAfter(OffsetDateTime.now())
                          && passwordEncoder.matches(request.getToken(), u.getResetToken()))
                .findFirst()
                .orElseThrow(() -> new TokenException("Invalid or expired reset token"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        authUserRepository.save(user);

        refreshTokenRepository.revokeAllForUser(user.getId());
        log.info("Password reset completed for user [{}]", user.getEmail());
    }

    // ── Account deletion ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteAccount(UUID authUserId) {
        authUserRepository.deactivate(authUserId);
        refreshTokenRepository.revokeAllForUser(authUserId);
        eventPublisher.publishUserDeleted(
                AuthUserDeletedEvent.builder().authUserId(authUserId).build());
        log.info("Account deactivated for authUserId=[{}]", authUserId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void storeRefreshToken(AuthUser user, String rawToken,
                                   String deviceInfo, String ipAddress) {
        RefreshToken rt = RefreshToken.builder()
                .authUser(user)
                .tokenHash(sha256(rawToken))
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .expiresAt(OffsetDateTime.now().plusSeconds(
                        jwtService.getRefreshTokenExpiryMs() / 1000))
                .build();
        refreshTokenRepository.save(rt);
    }

    private AuthResponse buildAuthResponse(AuthUser user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(jwtService.getAccessTokenExpiryMs() / 1000)
                .refreshTokenExpiresIn(jwtService.getRefreshTokenExpiryMs() / 1000)
                .build();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
