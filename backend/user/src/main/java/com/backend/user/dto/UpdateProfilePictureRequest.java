package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for updating user profile picture")
public class UpdateProfilePictureRequest {

    @NotNull(message = "Profile picture cannot be null")
    @Schema(description = "Profile picture file (multipart upload)")
    private org.springframework.web.multipart.MultipartFile profilePicture;

    public org.springframework.web.multipart.MultipartFile getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(org.springframework.web.multipart.MultipartFile profilePicture) {
        this.profilePicture = profilePicture;
    }
}
