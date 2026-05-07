package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generic API response message")
public class ApiResponse {

    @Schema(description = "Response message", example = "Login successful")
    private String message;

    public ApiResponse() {
    }

    public ApiResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}