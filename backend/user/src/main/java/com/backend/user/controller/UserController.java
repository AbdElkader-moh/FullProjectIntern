package com.backend.user.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.user.dto.ApiResponse;
import com.backend.user.dto.ChangePasswordRequest;
import com.backend.user.dto.LoginRequest;
import com.backend.user.dto.NotificationDTO;
import com.backend.user.dto.SettingsDTO;
import com.backend.user.dto.SignupRequest;
import com.backend.user.dto.UpdateProfilePictureRequest;
import com.backend.user.dto.UserResponse;
import com.backend.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@Tag(name = "User Management", description = "Endpoints for user registration, authentication, profile, settings, and notifications")
public class UserController {

    private final UserService userService;

    // Allowed alert types (BUG-SET-002)
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Validates a SettingsDTO and returns an error message if invalid, or null
     * if valid.
     */
    @PostMapping("/signup")
    @Operation(summary = "Register a new user", description = "Creates a new user account and automatically logs them in. Accepts multipart form data for profile picture upload.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User created successfully",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already registered",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<?> signup(@Valid @ModelAttribute SignupRequest request, HttpSession session) {
        UserResponse response = userService.signup(request);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(request.getEmail());
        loginRequest.setPassword(request.getPassword());
        userService.login(loginRequest, session);

        String token = userService.generateTokenForUser(request.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Authorization", "Bearer " + token)
                .body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Logs in with email and password. Sets a JSESSIONID cookie and returns a JWT for cross-service authentication.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or invalid fields"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        String message = userService.login(request, session);
        String token = userService.generateTokenForUser(request.getEmail());

        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + token)
                .body(new ApiResponse(message));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns the profile of the currently authenticated user (requires active session).")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserResponse> getProfile(HttpSession session) {
        return ResponseEntity.ok(userService.getCurrentUser(session));
    }

    @PutMapping("/me")
    @Operation(summary = "Update profile picture", description = "Uploads a new profile picture for the authenticated user. Accepts multipart form data.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile picture updated",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserResponse> updateProfilePicture(
            @ModelAttribute UpdateProfilePictureRequest request,
            HttpSession session) {
        return ResponseEntity.ok(userService.updateProfilePicture(request, session));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidates the current session and clears the JSESSIONID cookie.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out successfully")
    })
    public ResponseEntity<?> logout(HttpSession session) {
        userService.logout(session);
        return ResponseEntity.ok().body(Map.of("message", "Logged out successfully"));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change password", description = "Changes the password for the authenticated user. Requires current and new password.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password changed successfully",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated or wrong current password")
    })
    public ResponseEntity<ApiResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpSession session) {
        userService.changePassword(request, session);
        return ResponseEntity.ok(new ApiResponse("Password changed successfully."));
    }

    @GetMapping("/settings")
    @Operation(summary = "Get user alert settings", description = "Returns all threshold alert settings configured by the authenticated user.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Settings retrieved",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = SettingsDTO.class)))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<?> getSettings(HttpSession session) {
        return ResponseEntity.ok(userService.getSettings(session));
    }

    @PostMapping("/settings")
    @Operation(summary = "Create or bulk-update alert settings", description = "Creates or replaces alert threshold settings for the authenticated user.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Settings saved",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = SettingsDTO.class)))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<?> updateSettings(@RequestBody SettingsDTO request, HttpSession session) {
        String validationError = userService.validateSettingsRequest(request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("message", validationError));
        }
        return ResponseEntity.ok(userService.addSetting(request, session));
    }

    @PutMapping("/settings/{id}")
    @Operation(summary = "Update a single alert setting", description = "Updates a specific alert threshold setting by its ID.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Setting updated",
                content = @Content(schema = @Schema(implementation = SettingsDTO.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Setting not found")
    })
    public ResponseEntity<com.backend.user.dto.SettingsDTO> updateSetting(
            @Parameter(description = "Setting ID") @PathVariable String id,
            @RequestBody com.backend.user.dto.SettingsDTO request,
            HttpSession session) {

        return ResponseEntity.ok(userService.updateSetting(id, request, session));
    }

    @DeleteMapping("/settings/{id}")
    @Operation(summary = "Delete an alert setting", description = "Removes a specific alert threshold setting by its ID.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Setting deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Setting not found")
    })
    public ResponseEntity<?> deleteSetting(
            @Parameter(description = "Setting ID") @PathVariable String id,
            HttpSession session) {
        userService.deleteSetting(id, session);
        return ResponseEntity.ok(Map.of("message", "Setting deleted successfully"));
    }

    @GetMapping("/notifications")
    @Operation(summary = "Get user notifications", description = "Returns all threshold-breach notifications for the authenticated user.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifications retrieved",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = NotificationDTO.class)))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<?> getNotifications(HttpSession session) {
        return ResponseEntity.ok(userService.getNotifications(session));
    }

    @PutMapping("/notifications/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Marks a single notification as read by its ID.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification marked as read"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<?> markNotificationAsRead(
            @Parameter(description = "Notification ID") @PathVariable String id,
            HttpSession session) {
        userService.markNotificationAsRead(id, session);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @PutMapping("/notifications/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Marks all notifications for the authenticated user as read.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All notifications marked as read"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<?> markAllNotificationsAsRead(HttpSession session) {
        userService.markAllNotificationsAsRead(session);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @DeleteMapping("/notifications/{id}")
    @Operation(summary = "Delete a notification", description = "Permanently removes a notification by its ID.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<?> deleteNotification(
            @Parameter(description = "Notification ID") @PathVariable String id,
            HttpSession session) {
        userService.deleteNotification(id, session);
        return ResponseEntity.ok(Map.of("message", "Notification deleted successfully"));
    }

}
