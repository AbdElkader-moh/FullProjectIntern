package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request body for user login")
public class LoginRequest {

    @Email
    @NotBlank
    @Schema(description = "User email address", example = "user@example.com")
    private String email;

    @NotBlank
    @Schema(description = "User password", example = "abc123")
    private String password;
}
