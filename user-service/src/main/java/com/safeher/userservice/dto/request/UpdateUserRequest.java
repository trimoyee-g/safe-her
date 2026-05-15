package com.safeher.userservice.dto.request;

import com.safeher.userservice.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateUserRequest {

    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    @Pattern(regexp = "^\\+?[0-9\\-\\s]{7,20}$", message = "Invalid phone number format")
    private String phoneNumber;

    private Gender gender;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String country;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;

    private Boolean anonymous;
}
