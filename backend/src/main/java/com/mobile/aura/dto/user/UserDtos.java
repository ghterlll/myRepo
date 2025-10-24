package com.mobile.aura.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

public class UserDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterReq {
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        private String nickname;

        // Optional phone number
        private String phone;

        // Optional location info from client (will fallback to IP lookup if not provided)
        private String regionCode;
        private String city;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterWithOtpReq {
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        private String nickname;

        // Optional phone number
        private String phone;

        // Optional location info from client (will fallback to IP lookup if not provided)
        private String regionCode;
        private String city;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginReq {
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        private String deviceId;
    }

    @Data
    @AllArgsConstructor
    public static class TokenPair {
        private String accessToken;
        private String refreshToken;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendRegistrationCodeReq {
        @NotBlank(message = "Email is required")
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyRegistrationReq {
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Verification code is required")
        private String code;

        private String deviceId;
    }
}