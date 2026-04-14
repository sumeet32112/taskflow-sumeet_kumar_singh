package com.taskflow.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AuthDto {

    /* ── Register ──────────────────────────────────────────────────── */

    @Getter
    @Setter
    public static class RegisterRequest {

        @NotBlank(message = "is required")
        private String name;

        @NotBlank(message = "is required")
        @Email(message = "must be a valid email")
        private String email;

        @NotBlank(message = "is required")
        @Size(min = 8, message = "must be at least 8 characters")
        private String password;
    }

    /* ── Login ─────────────────────────────────────────────────────── */

    @Getter
    @Setter
    public static class LoginRequest {

        @NotBlank(message = "is required")
        @Email(message = "must be a valid email")
        private String email;

        @NotBlank(message = "is required")
        private String password;
    }

    /* ── Responses ─────────────────────────────────────────────────── */

    @Builder
    @Getter
    public static class TokenResponse {
        private String      token;
        private String      tokenType;
        private long        expiresIn;
        private UserSummary user;
    }

    @Builder
    @Getter
    public static class UserSummary {
        private UUID            id;
        private String          name;
        private String          email;
        private OffsetDateTime  createdAt;
    }
}
