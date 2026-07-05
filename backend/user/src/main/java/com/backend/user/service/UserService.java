package com.backend.user.service;

import com.backend.user.dto.ChangePasswordRequest;
import com.backend.user.dto.LoginRequest;
import com.backend.user.dto.SettingsDTO;
import com.backend.user.dto.SignupRequest;
import com.backend.user.dto.UpdateProfilePictureRequest;
import com.backend.user.dto.UserResponse;

import jakarta.servlet.http.HttpSession;

public interface UserService {
    UserResponse signup(SignupRequest request);
    String login(LoginRequest request, HttpSession session);
    String generateTokenForUser(String email);
    UserResponse getCurrentUser(HttpSession session);
    UserResponse updateProfilePicture(UpdateProfilePictureRequest request, HttpSession session);
    void logout(HttpSession session);
    void changePassword(ChangePasswordRequest request, HttpSession session);
    java.util.List<com.backend.user.dto.SettingsDTO> getSettings(HttpSession session);

    SettingsDTO addSetting(SettingsDTO req, HttpSession session);

    java.util.List<com.backend.user.dto.SettingsDTO> updateSettings(java.util.List<com.backend.user.dto.SettingsDTO> requests, HttpSession session);
    com.backend.user.dto.SettingsDTO updateSetting(String id, com.backend.user.dto.SettingsDTO request, HttpSession session);
    void deleteSetting(String id, HttpSession session);
    java.util.List<com.backend.user.dto.NotificationDTO> getNotifications(HttpSession session);
    void markNotificationAsRead(String id, HttpSession session);
    void markAllNotificationsAsRead(HttpSession session);
    void deleteNotification(String id, HttpSession session);

    String validateSettingsRequest(SettingsDTO request);
}