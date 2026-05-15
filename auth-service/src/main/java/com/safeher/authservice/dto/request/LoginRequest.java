package com.safeher.authservice.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class LoginRequest {
    @NotBlank(message = "Email or username is required")
    private String identifier;   // accepts email OR username
    @NotBlank(message = "Password is required")
    private String password;
    private String deviceInfo;
}
