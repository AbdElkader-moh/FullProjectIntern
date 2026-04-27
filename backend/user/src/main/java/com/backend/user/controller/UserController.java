package com.backend.user.controller;

import com.backend.user.dto.*;
import com.backend.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
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
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(userService.signup(request));
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
            @RequestBody UpdateProfilePictureRequest request,
            HttpSession session) {
        return ResponseEntity.ok(userService.updateProfilePicture(request, session));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpSession session) {
        userService.logout(session);
        return ResponseEntity.ok(new ApiResponse("Logged out successfully"));
    }
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpSession session) {
        userService.changePassword(request, session);
        return ResponseEntity.ok(new ApiResponse("Password changed successfully."));
    }
}