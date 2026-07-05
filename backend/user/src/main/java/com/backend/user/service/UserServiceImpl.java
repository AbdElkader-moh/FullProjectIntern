package com.backend.user.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.backend.user.dto.ChangePasswordRequest;
import com.backend.user.dto.LoginRequest;
import com.backend.user.dto.NotificationDTO;
import com.backend.user.dto.SettingsDTO;
import com.backend.user.dto.SignupRequest;
import com.backend.user.dto.UpdateProfilePictureRequest;
import com.backend.user.dto.UserResponse;
import com.backend.user.entity.Notification;
import com.backend.user.entity.Settings;
import com.backend.user.entity.User;
import com.backend.user.exception.ConflictException;
import com.backend.user.exception.NotFoundException;
import com.backend.user.exception.UnauthorizedException;
import com.backend.user.repository.NotificationRepository;
import com.backend.user.repository.SettingsRepository;
import com.backend.user.repository.UserRepository;
import com.backend.user.util.JwtUtil;
import com.cloudinary.Cloudinary;

import jakarta.servlet.http.HttpSession;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SettingsRepository settingsRepository;
    private final NotificationRepository notificationRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Cloudinary cloudinary;

    private final JwtUtil jwtUtil;

    public UserServiceImpl(
            UserRepository userRepository,
            SettingsRepository settingsRepository,
            NotificationRepository notificationRepository,
            Cloudinary cloudinary,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.cloudinary = cloudinary;
        this.jwtUtil = jwtUtil;
    }
    // Allowed metrics per sensor type (BUG-SET-001, BUG-SET-005)
    private static final Map<String, Set<String>> VALID_METRICS = Map.of(
            "Traffic", Set.of("Traffic Density", "Average Speed"),
            "Air", Set.of("Carbon Monoxide", "Ozone"),
            "Light", Set.of("Brightness Level", "Power Consumption")
    );
    private static final Set<String> VALID_ALERT_TYPES = Set.of("above", "below");

    @Override
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("An account with this email already exists.");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        try {
            if (request.getProfilePicture() != null && !request.getProfilePicture().isEmpty()) {
                if (cloudinary == null) {
                    throw new RuntimeException("Server configuration error: Cloudinary secrets missing. Image upload disabled.");
                }
                Map uploadResult = cloudinary.uploader().upload(
                        request.getProfilePicture().getBytes(),
                        com.cloudinary.utils.ObjectUtils.emptyMap()
                );
                user.setProfilePicture(uploadResult.get("url").toString());
            } else {
                user.setProfilePicture("");
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not upload profile picture to Cloudinary.");
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        return mapToResponse(savedUser);
    }

    @Override
    public String login(LoginRequest request, HttpSession session) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        session.setAttribute("userId", user.getId());
        return "Login successful";
    }

    @Override
    public String generateTokenForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));
        return jwtUtil.generateToken(user.getId().toString(), user.getEmail());
    }

    @Override
    public UserResponse getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            throw new UnauthorizedException("You are not logged in.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));

        return mapToResponse(user);
    }

    @Override
    public UserResponse updateProfilePicture(UpdateProfilePictureRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            throw new UnauthorizedException("You are not logged in.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));

        try {
            if (request.getProfilePicture() != null && !request.getProfilePicture().isEmpty()) {
                if (cloudinary == null) {
                    throw new RuntimeException("Server configuration error: Cloudinary secrets missing. Image upload disabled.");
                }
                Map uploadResult = cloudinary.uploader().upload(
                        request.getProfilePicture().getBytes(),
                        com.cloudinary.utils.ObjectUtils.emptyMap()
                );
                user.setProfilePicture(uploadResult.get("url").toString());
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not upload profile picture to Cloudinary.");
        }

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    @Override
    public void logout(HttpSession session) {
        session.invalidate();
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getProfilePicture(),
                user.getPassword()
        );
    }

    @Override
    public void changePassword(ChangePasswordRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            throw new UnauthorizedException("You are not logged in.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new UnauthorizedException("Old password is incorrect.");
        }

        // ✅ NEW: Check new password length
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }

        // ✅ NEW: Check new password is different from old one
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the old password.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public List<SettingsDTO> getSettings(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("You are not logged in.");
        }
        return settingsRepository.findByUserId(userId).stream()
                .map(s -> new SettingsDTO(s.getId(), s.getType(), s.getMetric(), s.getThresholdValue(), s.getAlertType()))
                .collect(Collectors.toList());
    }

    @Override
    public SettingsDTO addSetting(SettingsDTO req, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("You are not logged in.");
        }
        req.validateThreshold();
        Settings newSettings = new Settings(userId, req.getType(), req.getMetric(), req.getThresholdValue(), req.getAlertType());
        Settings saved = settingsRepository.save(newSettings);
        return new SettingsDTO(saved.getId(), saved.getType(), saved.getMetric(), saved.getThresholdValue(), saved.getAlertType());
    }

    @Override
    public List<SettingsDTO> updateSettings(List<SettingsDTO> requests, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("You are not logged in.");
        }

        for (SettingsDTO req : requests) {

            Settings settings = null;

            // 1. Try to find by ID
            if (req.getId() != null) {
                settings = settingsRepository.findById(req.getId()).orElse(null);
            }

            // 2. Try to find by Type and Metric if ID search failed or wasn't provided
            if (settings == null) {
                settings = settingsRepository.findByUserIdAndTypeAndMetric(userId, req.getType(), req.getMetric()).orElse(null);
            }

            // Create new
            Settings newSettings = new Settings(userId, req.getType(), req.getMetric(), req.getThresholdValue(), req.getAlertType());
            settingsRepository.save(newSettings);

        }
        return getSettings(session);
    }

    @Override
    public SettingsDTO updateSetting(String id, SettingsDTO request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("You are not logged in.");
        }
        Settings settings = settingsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Setting not found."));
        if (!settings.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to update this setting.");
        }
        request.validateThreshold(settings.getType(), settings.getMetric()); // ← uses DB values
        settings.setThresholdValue(request.getThresholdValue());
        settings.setAlertType(request.getAlertType());
        Settings saved = settingsRepository.save(settings);
        return new SettingsDTO(saved.getId(), saved.getType(), saved.getMetric(), saved.getThresholdValue(), saved.getAlertType());
    }

    @Override
    public void deleteSetting(String id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("You are not logged in.");
        }
        Settings settings = settingsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Setting not found."));
        if (!settings.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to delete this setting.");
        }
        settingsRepository.deleteById(id);
    }

    @Override
    public List<NotificationDTO> getNotifications(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("You are not logged in.");
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> new NotificationDTO(
                n.getId(), n.getType(), n.getMetric(), n.getValue(),
                n.getThresholdValue(), n.getAlertType(), n.getLocation(),
                n.getIsRead(), n.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public void markNotificationAsRead(String id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("You are not logged in.");
        }
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found."));
        if (!notification.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to update this notification.");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void markAllNotificationsAsRead(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("You are not logged in.");
        }
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (Notification n : notifications) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(notifications);
    }

    @Override
    public void deleteNotification(String id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("You are not logged in.");
        }
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found."));
        if (!notification.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to delete this notification.");
        }
        notificationRepository.deleteById(id);
    }

    @Override
    public String validateSettingsRequest(SettingsDTO request) {
        // BUG-SET-005 / BUG-SET-008: type must not be null or empty
        if (request.getType() == null || request.getType().trim().isEmpty()) {
            return "type is required";
        }
        // BUG-SET-003 / BUG-SET-008: metric must not be null or empty
        if (request.getMetric() == null || request.getMetric().trim().isEmpty()) {
            return "metric is required";
        }
        // BUG-SET-004 / BUG-SET-008: alertType must not be null or empty
        if (request.getAlertType() == null || request.getAlertType().trim().isEmpty()) {
            return "alertType is required";
        }
        // BUG-SET-006: thresholdValue must not be null
        if (request.getThresholdValue() == null) {
            return "thresholdValue is required";
        }
        // BUG-SET-007: thresholdValue must be non-negative
        if (request.getThresholdValue() < 0) {
            return "thresholdValue must be a non-negative number";
        }
        // BUG-SET-005: type must be a supported sensor type
        if (!VALID_METRICS.containsKey(request.getType())) {
            return "Invalid type: must be one of " + VALID_METRICS.keySet();
        }
        // BUG-SET-001: metric must be valid for the given type
        Set<String> allowedMetrics = VALID_METRICS.get(request.getType());
        if (!allowedMetrics.contains(request.getMetric())) {
            return "Invalid metric '" + request.getMetric() + "' for type '" + request.getType()
                    + "'. Allowed values: " + allowedMetrics;
        }
        // BUG-SET-002: alertType must be "above" or "below"
        if (!VALID_ALERT_TYPES.contains(request.getAlertType())) {
            return "Invalid alertType '" + request.getAlertType()
                    + "'. Allowed values: " + VALID_ALERT_TYPES;
        }
        return null; // valid
    }

}
