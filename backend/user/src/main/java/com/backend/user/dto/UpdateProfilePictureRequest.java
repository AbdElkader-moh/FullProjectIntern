package com.backend.user.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateProfilePictureRequest {

    @NotNull(message = "Profile picture cannot be null")
    private org.springframework.web.multipart.MultipartFile profilePicture;

    public org.springframework.web.multipart.MultipartFile getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(org.springframework.web.multipart.MultipartFile profilePicture) {
        this.profilePicture = profilePicture;
    }
}
