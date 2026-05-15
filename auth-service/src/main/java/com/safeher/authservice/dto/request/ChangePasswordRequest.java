package com.safeher.authservice.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class ChangePasswordRequest {
    @NotBlank private String currentPassword;
    @NotBlank
    @Size(min = 8, max = 128)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=]).+$",
             message = "Password must contain uppercase, lowercase, digit and special character")
    private String newPassword;
}
