package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User profile response")
public class UserResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "User email address", example = "user@example.com")
    private String email;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Profile picture URL or base64 data")
    private String profilePicture;

    @Schema(description = "Hashed password (bcrypt)")
    private String password;

    public UserResponse(Long id, String email, String firstName, String lastName, String profilePicture, String password) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profilePicture = profilePicture;
        this.password = password;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getProfilePicture() { return profilePicture; }
    public String getPassword() { return password; }
}