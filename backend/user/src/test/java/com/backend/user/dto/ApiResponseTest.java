package com.backend.user.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void noArgConstructor_messageIsNull() {
        ApiResponse response = new ApiResponse();

        assertThat(response.getMessage()).isNull();
    }

    @Test
    void constructorWithMessage_setsMessage() {
        ApiResponse response = new ApiResponse("Login successful");

        assertThat(response.getMessage()).isEqualTo("Login successful");
    }

    @Test
    void constructorWithNullMessage_getterReturnsNull() {
        ApiResponse response = new ApiResponse(null);

        assertThat(response.getMessage()).isNull();
    }

    @Test
    void constructorWithEmptyMessage_getterReturnsEmptyString() {
        ApiResponse response = new ApiResponse("");

        assertThat(response.getMessage()).isEmpty();
    }
}
