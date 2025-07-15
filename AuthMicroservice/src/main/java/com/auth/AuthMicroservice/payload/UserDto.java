package com.auth.AuthMicroservice.payload;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {
    private String userId;
    private String userName;
    private String userEmail;
    private String userAbout;
    private String userGender;
}
