package com.backend.user.controller;

import com.backend.user.dto.*;
import com.backend.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @ModelAttribute SignupRequest request, HttpSession session) {
        UserResponse response = userService.signup(request);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(request.getEmail());
        loginRequest.setPassword(request.getPassword());
        userService.login(loginRequest, session);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        String message = userService.login(request, session);
        return ResponseEntity.ok(new ApiResponse(message));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(HttpSession session) {
        return ResponseEntity.ok(userService.getCurrentUser(session));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfilePicture(
            @ModelAttribute UpdateProfilePictureRequest request,
            HttpSession session) {
        return ResponseEntity.ok(userService.updateProfilePicture(request, session));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        userService.logout(session);
        return ResponseEntity.ok().body(Map.of("message", "Logged out successfully"));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpSession session) {
        userService.changePassword(request, session);
        return ResponseEntity.ok(new ApiResponse("Password changed successfully."));
    }

    @GetMapping("/settings")
    public ResponseEntity<?> getSettings(HttpSession session) {
        return ResponseEntity.ok(userService.getSettings(session));
    }

    @PostMapping("/settings")
    public ResponseEntity<?> updateSettings(@RequestBody java.util.List<com.backend.user.dto.SettingsDTO> requests,
            HttpSession session) {
        return ResponseEntity.ok(userService.updateSettings(requests, session));
    }

    @PutMapping("/settings/{id}")
    public ResponseEntity<com.backend.user.dto.SettingsDTO> updateSetting(
            @PathVariable String id,
            @RequestBody com.backend.user.dto.SettingsDTO request,
            HttpSession session) {
        return ResponseEntity.ok(userService.updateSetting(id, request, session));
    }

    @DeleteMapping("/settings/{id}")
    public ResponseEntity<?> deleteSetting(
            @PathVariable String id,
            HttpSession session) {
        userService.deleteSetting(id, session);
        return ResponseEntity.ok(Map.of("message", "Setting deleted successfully"));
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(HttpSession session) {
        return ResponseEntity.ok(userService.getNotifications(session));
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<?> markNotificationAsRead(
            @PathVariable String id,
            HttpSession session) {
        userService.markNotificationAsRead(id, session);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @PutMapping("/notifications/read-all")
    public ResponseEntity<?> markAllNotificationsAsRead(HttpSession session) {
        userService.markAllNotificationsAsRead(session);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @DeleteMapping("/notifications/{id}")
    public ResponseEntity<?> deleteNotification(
            @PathVariable String id,
            HttpSession session) {
        userService.deleteNotification(id, session);
        return ResponseEntity.ok(Map.of("message", "Notification deleted successfully"));
    }

}