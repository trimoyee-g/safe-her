package com.safeher.authservice.service;

import com.safeher.authservice.dto.request.*;
import com.safeher.authservice.dto.response.AuthResponse;

import java.util.UUID;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request, String clientIp);

    AuthResponse refreshToken(RefreshTokenRequest request, String clientIp);

    void logout(String accessToken, String refreshToken);

    void logoutAllDevices(UUID authUserId);

    void changePassword(UUID authUserId, ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void deleteAccount(UUID authUserId);
}
