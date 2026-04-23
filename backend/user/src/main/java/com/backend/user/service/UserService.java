package com.backend.user.service;

import com.backend.user.dto.LoginRequest;
import com.backend.user.dto.SignupRequest;
import com.backend.user.dto.UserResponse;
import jakarta.servlet.http.HttpSession;

public interface UserService {
    UserResponse signup(SignupRequest request);
    String login(LoginRequest request, HttpSession session);
    UserResponse getCurrentUser(HttpSession session);
    void logout(HttpSession session);
}