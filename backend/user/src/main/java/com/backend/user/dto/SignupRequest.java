package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

    @Schema(description = "Profile picture file (multipart upload)")
    private org.springframework.web.multipart.MultipartFile profilePicture;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public org.springframework.web.multipart.MultipartFile getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(org.springframework.web.multipart.MultipartFile profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}