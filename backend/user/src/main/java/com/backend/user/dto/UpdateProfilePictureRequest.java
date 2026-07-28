package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Schema(description = "Request body for updating user profile picture")
public class UpdateProfilePictureRequest {

    @NotNull(message = "Profile picture cannot be null")
    @Schema(description = "Profile picture file (multipart upload)")
    private MultipartFile profilePicture;
}
