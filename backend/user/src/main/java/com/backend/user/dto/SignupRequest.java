package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Schema(description = "Request body for user registration")
public class SignupRequest {

    @Email
    @NotBlank
    @Schema(description = "User email address", example = "user@example.com")
    private String email;

    @NotBlank
    @Schema(description = "User first name", example = "John")
    private String firstName;

    @NotBlank
    @Schema(description = "User last name", example = "Doe")
    private String lastName;

    @NotNull(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "Password (min 6 characters)", example = "abc123")
    private String password;

    @NotNull(message = "Profile picture is required")
    @Schema(description = "Profile picture file (multipart upload)", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile profilePicture;
}
