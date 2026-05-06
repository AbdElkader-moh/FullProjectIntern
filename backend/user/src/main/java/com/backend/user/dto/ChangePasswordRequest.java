package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for changing user password")
public class ChangePasswordRequest {

    @NotBlank
    @Schema(description = "Current password", example = "oldpass123")
    private String oldPassword;

    @NotBlank
    @Schema(description = "New password", example = "newpass456")
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}