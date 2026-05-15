package com.safeher.authservice.controller;

import com.safeher.authservice.dto.request.*;
import com.safeher.authservice.dto.response.ApiResponse;
import com.safeher.authservice.dto.response.AuthResponse;
import com.safeher.authservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ── Registration ──────────────────────────────────────────────────────────

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Registration successful", authService.register(request));
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletRequest httpRequest) {
        String ip = extractClientIp(httpRequest);
        return ApiResponse.ok("Login successful", authService.login(request, ip));
    }

    // ── Token refresh ─────────────────────────────────────────────────────────

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                             HttpServletRequest httpRequest) {
        String ip = extractClientIp(httpRequest);
        return ApiResponse.ok("Token refreshed", authService.refreshToken(request, ip));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logout(@RequestBody(required = false) RefreshTokenRequest body,
                                    HttpServletRequest httpRequest) {
        String accessToken  = extractRawToken(httpRequest);
        String refreshToken = (body != null) ? body.getRefreshToken() : null;
        authService.logout(accessToken, refreshToken);
        return ApiResponse.ok("Logged out successfully", null);
    }

    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logoutAll(Authentication auth) {
        UUID authUserId = (UUID) auth.getCredentials();
        authService.logoutAllDevices(authUserId);
        return ApiResponse.ok("Logged out from all devices", null);
    }

    // ── Password management ───────────────────────────────────────────────────

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                            Authentication auth) {
        UUID authUserId = (UUID) auth.getCredentials();
        authService.changePassword(authUserId, request);
        return ApiResponse.ok("Password changed. Please log in again on all devices.", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        // Always return 200 to prevent email enumeration
        return ApiResponse.ok("If that email is registered, a reset link has been sent.", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("Password reset successfully. Please log in.", null);
    }

    // ── Account deletion ──────────────────────────────────────────────────────

    @DeleteMapping("/account")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteAccount(Authentication auth) {
        UUID authUserId = (UUID) auth.getCredentials();
        authService.deleteAccount(authUserId);
        return ApiResponse.ok("Account deleted", null);
    }

    // ── Health / validation ───────────────────────────────────────────────────

    /** Lets the gateway or other services verify a JWT is still valid */
    @GetMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> validate() {
        return ApiResponse.ok("Token is valid", null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private String extractRawToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}
