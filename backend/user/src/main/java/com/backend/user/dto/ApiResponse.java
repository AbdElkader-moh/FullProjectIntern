package com.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generic API response message")
public class ApiResponse {

    @Schema(description = "Response message", example = "Login successful")
    private String message;
}
