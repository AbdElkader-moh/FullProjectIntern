package com.backend.user.service;

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
import com.backend.user.exception.ExternalServiceException;
import com.backend.user.exception.NotFoundException;
import com.backend.user.exception.UnauthorizedException;
import com.backend.user.repository.NotificationRepository;
import com.backend.user.repository.SettingsRepository;
import com.backend.user.repository.UserRepository;
import com.backend.user.util.JwtUtil;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Full-coverage unit tests for UserServiceImpl.
 *
 * NOTE: passwordEncoder inside UserServiceImpl is instantiated internally
 * (`new BCryptPasswordEncoder()`), not injected — it can't be mocked. Tests
 * that depend on password matching use a real BCryptPasswordEncoder to
 * produce the stored hash, so `matches(...)` behaves correctly against
 * real bcrypt rather than a stub.
 *
 * UserRepository / SettingsRepository / NotificationRepository / JwtUtil
 * aren't in hand as source, but every method called on them here is taken
 * directly from the exact call sites in UserServiceImpl, so the mocked
 * signatures match the real interfaces.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private SettingsRepository settingsRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private Cloudinary cloudinary;
    @Mock private JwtUtil jwtUtil;
    @Mock private HttpSession session;

    private UserServiceImpl userService;
    private final BCryptPasswordEncoder realEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, settingsRepository, notificationRepository, cloudinary, jwtUtil);
    }

    private User buildUser(Long id, String email, String rawPassword) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setProfilePicture("http://old-pic");
        user.setPassword(realEncoder.encode(rawPassword));
        return user;
    }

    // ==================== signup ====================

    @Test
    void signup_emailAlreadyExists_throwsConflictException() {
        SignupRequest req = new SignupRequest();
        req.setEmail("taken@example.com");
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.signup(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_noProfilePicture_setsEmptyStringAndSucceeds() {
        SignupRequest req = new SignupRequest();
        req.setEmail("new@example.com");
        req.setFirstName("Jane");
        req.setLastName("Smith");
        req.setPassword("password1");
        req.setProfilePicture(null);

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse response = userService.signup(req);

        assertEquals("new@example.com", response.getEmail());
        assertEquals("", response.getProfilePicture());
    }

    @Test
    void signup_pictureProvided_cloudinaryNull_throwsExternalServiceException() {
        SignupRequest req = new SignupRequest();
        req.setEmail("new@example.com");
        req.setFirstName("Jane");
        req.setLastName("Smith");
        req.setPassword("password1");
        req.setProfilePicture(new MockMultipartFile("profilePicture", "pic.png", "image/png", "bytes".getBytes()));

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        UserServiceImpl serviceWithNullCloudinary =
                new UserServiceImpl(userRepository, settingsRepository, notificationRepository, null, jwtUtil);

        assertThrows(ExternalServiceException.class, () -> serviceWithNullCloudinary.signup(req));
    }

    @Test
    void signup_pictureProvided_uploadSucceeds() throws IOException {
        SignupRequest req = new SignupRequest();
        req.setEmail("new@example.com");
        req.setFirstName("Jane");
        req.setLastName("Smith");
        req.setPassword("password1");
        req.setProfilePicture(new MockMultipartFile("profilePicture", "pic.png", "image/png", "bytes".getBytes()));

        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("url", "http://cdn/pic.png"));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        UserResponse response = userService.signup(req);

        assertEquals("http://cdn/pic.png", response.getProfilePicture());
    }

    @Test
    void signup_pictureUploadThrowsIOException_throwsExternalServiceException() throws IOException {
        SignupRequest req = new SignupRequest();
        req.setEmail("new@example.com");
        req.setFirstName("Jane");
        req.setLastName("Smith");
        req.setPassword("password1");
        req.setProfilePicture(new MockMultipartFile("profilePicture", "pic.png", "image/png", "bytes".getBytes()));

        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("upload failed"));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        assertThrows(ExternalServiceException.class, () -> userService.signup(req));
    }

    @Test
    void signup_passwordTooShort_throwsIllegalArgumentException() {
        SignupRequest req = new SignupRequest();
        req.setEmail("new@example.com");
        req.setFirstName("Jane");
        req.setLastName("Smith");
        req.setPassword("abc");
        req.setProfilePicture(null);

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.signup(req));
    }

    // ==================== login ====================

    @Test
    void login_validCredentials_setsSessionAndReturnsSuccessMessage() {
        User user = buildUser(1L, "user@example.com", "correctPass");
        LoginRequest req = new LoginRequest();
        req.setEmail("user@example.com");
        req.setPassword("correctPass");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        String result = userService.login(req, session);

        assertEquals("Login successful", result);
        verify(session).setAttribute("userId", 1L);
    }

    @Test
    void login_userNotFound_throwsUnauthorized() {
        LoginRequest req = new LoginRequest();
        req.setEmail("nobody@example.com");
        req.setPassword("whatever");

        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> userService.login(req, session));
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        User user = buildUser(1L, "user@example.com", "correctPass");
        LoginRequest req = new LoginRequest();
        req.setEmail("user@example.com");
        req.setPassword("wrongPass");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> userService.login(req, session));
    }

    // ==================== generateTokenForUser ====================

    @Test
    void generateTokenForUser_validEmail_returnsToken() {
        User user = buildUser(1L, "user@example.com", "pass1234");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("1", "user@example.com")).thenReturn("signed-token");

        String token = userService.generateTokenForUser("user@example.com");

        assertEquals("signed-token", token);
    }

    @Test
    void generateTokenForUser_userNotFound_throwsUnauthorized() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> userService.generateTokenForUser("ghost@example.com"));
    }

    // ==================== getCurrentUser ====================

    @Test
    void getCurrentUser_notLoggedIn_throwsUnauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> userService.getCurrentUser(session));
    }

    @Test
    void getCurrentUser_userNotFound_throwsNotFound() {
        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getCurrentUser(session));
    }

    @Test
    void getCurrentUser_validSession_returnsUserResponse() {
        User user = buildUser(1L, "user@example.com", "pass1234");
        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getCurrentUser(session);

        assertEquals("user@example.com", response.getEmail());
    }

    // ==================== updateProfilePicture ====================

    @Test
    void updateProfilePicture_notLoggedIn_throwsUnauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        UpdateProfilePictureRequest req = new UpdateProfilePictureRequest();

        assertThrows(UnauthorizedException.class, () -> userService.updateProfilePicture(req, session));
    }

    @Test
    void updateProfilePicture_userNotFound_throwsNotFound() {
        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        UpdateProfilePictureRequest req = new UpdateProfilePictureRequest();

        assertThrows(NotFoundException.class, () -> userService.updateProfilePicture(req, session));
    }

    @Test
    void updateProfilePicture_noNewPicture_keepsExistingAndSaves() {
        User user = buildUser(1L, "user@example.com", "pass1234");
        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateProfilePictureRequest req = new UpdateProfilePictureRequest();
        req.setProfilePicture(null);

        UserResponse response = userService.updateProfilePicture(req, session);

        assertEquals("http://old-pic", response.getProfilePicture());
    }

    @Test
    void updateProfilePicture_cloudinaryNull_throwsExternalServiceException() {
        User user = buildUser(1L, "user@example.com", "pass1234");
        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UpdateProfilePictureRequest req = new UpdateProfilePictureRequest();
        req.setProfilePicture(new MockMultipartFile("profilePicture", "pic.png", "image/png", "bytes".getBytes()));

        UserServiceImpl serviceWithNullCloudinary =
                new UserServiceImpl(userRepository, settingsRepository, notificationRepository, null, jwtUtil);

        assertThrows(ExternalServiceException.class, () -> serviceWithNullCloudinary.updateProfilePicture(req, session));
    }

    @Test
    void updateProfilePicture_uploadFails_throwsExternalServiceException() throws IOException {
        User user = buildUser(1L, "user@example.com", "pass1234");
        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("fail"));
        UpdateProfilePictureRequest req = new UpdateProfilePictureRequest();
        req.setProfilePicture(new MockMultipartFile("profilePicture", "pic.png", "image/png", "bytes".getBytes()));

        assertThrows(ExternalServiceException.class, () -> userService.updateProfilePicture(req, session));
    }

    @Test
    void updateProfilePicture_uploadSucceeds_updatesPicture() throws IOException {
        User user = buildUser(1L, "user@example.com", "pass1234");
        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("url", "http://cdn/new.png"));
        UpdateProfilePictureRequest req = new UpdateProfilePictureRequest();
        req.setProfilePicture(new MockMultipartFile("profilePicture", "pic.png", "image/png", "bytes".getBytes()));

        UserResponse response = userService.updateProfilePicture(req, session);

        assertEquals("http://cdn/new.png", response.getProfilePicture());
    }

    // ==================== logout ====================

    @Test
    void logout_invalidatesSession() {
        userService.logout(session);
        verify(session).invalidate();
    }

    // ==================== changePassword ====================

    @Test
    void changePassword_notLoggedIn_throwsUnauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ChangePasswordRequest req = new ChangePasswordRequest();

        assertThrows(UnauthorizedException.class, () -> userService.changePassword(req, session));
    }

    @Test
    void changePassword_userNotFound_throwsNotFound() {
        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        ChangePasswordRequest req = new ChangePasswordRequest();

        assertThrows(NotFoundException.class, () -> userService.changePassword(req, session));
    }

    @Test
    void changePassword_wrongOldPassword_throwsUnauthorized() {
        User user = buildUser(1L, "user@example.com", "correctOld");
        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("wrongOld");
        req.setNewPassword("newpass1");

        assertThrows(UnauthorizedException.class, () -> userService.changePassword(req, session));
    }

    @Test
    void changePassword_newPasswordTooShort_throwsIllegalArgument() {
        User user = buildUser(1L, "user@example.com", "correctOld");
        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("correctOld");
        req.setNewPassword("abc");

        assertThrows(IllegalArgumentException.class, () -> userService.changePassword(req, session));
    }

    @Test
    void changePassword_newPasswordSameAsOld_throwsIllegalArgument() {
        User user = buildUser(1L, "user@example.com", "correctOld");
        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("correctOld");
        req.setNewPassword("correctOld");

        assertThrows(IllegalArgumentException.class, () -> userService.changePassword(req, session));
    }

    @Test
    void changePassword_validRequest_encodesAndSaves() {
        User user = buildUser(1L, "user@example.com", "correctOld");
        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("correctOld");
        req.setNewPassword("brandNewPass");

        userService.changePassword(req, session);

        verify(userRepository).save(user);
    }

    // ==================== getSettings ====================

    @Test
    void getSettings_notLoggedIn_throwsUnauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> userService.getSettings(session));
    }

    @Test
    void getSettings_returnsMappedList() {
        when(session.getAttribute("userId")).thenReturn(1L);
        Settings s = new Settings(1L, "Traffic", "Traffic Density", 80f, "above");
        when(settingsRepository.findByUserId(1L)).thenReturn(List.of(s));

        List<SettingsDTO> result = userService.getSettings(session);

        assertEquals(1, result.size());
        assertEquals("Traffic", result.get(0).getType());
    }

    // ==================== addSetting ====================

    @Test
    void addSetting_notLoggedIn_throwsUnauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        SettingsDTO req = new SettingsDTO(null, "Traffic", "Traffic Density", 80f, "above");

        assertThrows(UnauthorizedException.class, () -> userService.addSetting(req, session));
    }

    @Test
    void addSetting_invalidThreshold_propagatesIllegalArgumentException() {
        when(session.getAttribute("userId")).thenReturn(1L);
        SettingsDTO req = new SettingsDTO(null, "Traffic", "Traffic Density", 9999f, "above");

        assertThrows(IllegalArgumentException.class, () -> userService.addSetting(req, session));
        verify(settingsRepository, never()).save(any());
    }

    @Test
    void addSetting_validRequest_savesAndReturnsDto() {
        when(session.getAttribute("userId")).thenReturn(1L);
        SettingsDTO req = new SettingsDTO(null, "Traffic", "Traffic Density", 80f, "above");
        when(settingsRepository.save(any(Settings.class))).thenAnswer(inv -> inv.getArgument(0));

        SettingsDTO result = userService.addSetting(req, session);

        assertEquals("Traffic", result.getType());
        assertEquals(80f, result.getThresholdValue());
    }

    // ==================== updateSettings (bulk) ====================

    @Test
    void updateSettings_notLoggedIn_throwsUnauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> userService.updateSettings(List.of(), session));
    }

    @Test
    void updateSettings_foundById_updatesInPlace() {
        when(session.getAttribute("userId")).thenReturn(1L);
        Settings existing = new Settings(1L, "Air", "Ozone", 100f, "above");
        SettingsDTO req = new SettingsDTO(existing.getId(), "Air", "Ozone", 200f, "below");
        when(settingsRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(settingsRepository.findByUserId(1L)).thenReturn(List.of(existing));

        userService.updateSettings(List.of(req), session);

        verify(settingsRepository).save(existing);
        assertEquals(200f, existing.getThresholdValue());
        assertEquals("below", existing.getAlertType());
    }

    @Test
    void updateSettings_foundByTypeAndMetricFallback_updatesInPlace() {
        when(session.getAttribute("userId")).thenReturn(1L);
        Settings existing = new Settings(1L, "Air", "Ozone", 100f, "above");
        SettingsDTO req = new SettingsDTO(null, "Air", "Ozone", 200f, "below");
        when(settingsRepository.findByUserIdAndTypeAndMetric(1L, "Air", "Ozone")).thenReturn(Optional.of(existing));
        when(settingsRepository.findByUserId(1L)).thenReturn(List.of(existing));

        userService.updateSettings(List.of(req), session);

        verify(settingsRepository).save(existing);
    }

    @Test
    void updateSettings_notFound_insertsNew() {
        when(session.getAttribute("userId")).thenReturn(1L);
        SettingsDTO req = new SettingsDTO(null, "Air", "Ozone", 200f, "below");
        when(settingsRepository.findByUserIdAndTypeAndMetric(1L, "Air", "Ozone")).thenReturn(Optional.empty());
        when(settingsRepository.findByUserId(1L)).thenReturn(List.of());

        userService.updateSettings(List.of(req), session);

        verify(settingsRepository).save(any(Settings.class));
    }

    // ==================== updateSetting (single) ====================

    @Test
    void updateSetting_notLoggedIn_throwsUnauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        SettingsDTO req = new SettingsDTO(null, "Air", "Ozone", 100f, "above");

        assertThrows(UnauthorizedException.class, () -> userService.updateSetting("id-1", req, session));
    }

    @Test
    void updateSetting_notFound_throwsNotFound() {
        when(session.getAttribute("userId")).thenReturn(1L);
        when(settingsRepository.findById("id-1")).thenReturn(Optional.empty());
        SettingsDTO req = new SettingsDTO(null, "Air", "Ozone", 100f, "above");

        assertThrows(NotFoundException.class, () -> userService.updateSetting("id-1", req, session));
    }

    @Test
    void updateSetting_wrongUser_throwsUnauthorized() {
        when(session.getAttribute("userId")).thenReturn(1L);
        Settings existing = new Settings(2L, "Air", "Ozone", 100f, "above");
        when(settingsRepository.findById("id-1")).thenReturn(Optional.of(existing));
        SettingsDTO req = new SettingsDTO(null, "Air", "Ozone", 150f, "above");

        assertThrows(UnauthorizedException.class, () -> userService.updateSetting("id-1", req, session));
    }

    @Test
    void updateSetting_validRequest_updatesUsingDbTypeAndMetric() {
        when(session.getAttribute("userId")).thenReturn(1L);
        Settings existing = new Settings(1L, "Air", "Ozone", 100f, "above");
        when(settingsRepository.findById("id-1")).thenReturn(Optional.of(existing));
        when(settingsRepository.save(any(Settings.class))).thenAnswer(inv -> inv.getArgument(0));
        SettingsDTO req = new SettingsDTO(null, null, null, 150f, "below");

        SettingsDTO result = userService.updateSetting("id-1", req, session);

        assertEquals(150f, result.getThresholdValue());
        assertEquals("below", result.getAlertType());
    }

    // ==================== deleteSetting ====================

    @Test
    void deleteSetting_notLoggedIn_throwsUnauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> userService.deleteSetting("id-1", session));
    }

    @Test
    void deleteSetting_notFound_throwsNotFound() {
        when(session.getAttribute("userId")).thenReturn(1L);
        when(settingsRepository.findById("id-1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.deleteSetting("id-1", session));
    }

    @Test
    void deleteSetting_wrongUser_throwsUnauthorized() {
        when(session.getAttribute("userId")).thenReturn(1L);
        Settings existing = new Settings(2L, "Air", "Ozone", 100f, "above");
        when(settingsRepository.findById("id-1")).thenReturn(Optional.of(existing));

        assertThrows(UnauthorizedException.class, () -> userService.deleteSetting("id-1", session));
    }

    @Test
    void deleteSetting_validRequest_deletes() {
        when(session.getAttribute("userId")).thenReturn(1L);
        Settings existing = new Settings(1L, "Air", "Ozone", 100f, "above");
        when(settingsRepository.findById("id-1")).thenReturn(Optional.of(existing));

        userService.deleteSetting("id-1", session);

        verify(settingsRepository).deleteById("id-1");
    }

    // ==================== getNotifications ====================

    @Test
    void getNotifications_notLoggedIn_throwsUnauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> userService.getNotifications(session));
    }

    @Test
    void getNotifications_returnsMappedList() {
        when(session.getAttribute("userId")).thenReturn(1L);
        Notification n = new Notification();
        n.setUserId(1L);
        n.setType("Traffic");
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(n));

        List<NotificationDTO> result = userService.getNotifications(session);

        assertEquals(1, result.size());
        assertEquals("Traffic", result.get(0).getType());
    }

    // ==================== markNotificationAsRead ====================

    @Test
    void markNotificationAsRead_notLoggedIn_throwsUnauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> userService.markNotificationAsRead("n-1", session));
    }

    @Test
    void markNotificationAsRead_notFound_throwsNotFound() {
        when(session.getAttribute("userId")).thenReturn(1L);
        when(notificationRepository.findById("n-1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.markNotificationAsRead("n-1", session));
    }

    @Test
    void markNotificationAsRead_wrongUser_throwsUnauthorized() {
        when(session.getAttribute("userId")).thenReturn(1L);
        Notification n = new Notification();
        n.setUserId(2L);
        when(notificationRepository.findById("n-1")).thenReturn(Optional.of(n));

        assertThrows(UnauthorizedException.class, () -> userService.markNotificationAsRead("n-1", session));
    }

    @Test
    void markNotificationAsRead_validRequest_marksAndSaves() {
        when(session.getAttribute("userId")).thenReturn(1L);
        Notification n = new Notification();
        n.setUserId(1L);
        when(notificationRepository.findById("n-1")).thenReturn(Optional.of(n));

        userService.markNotificationAsRead("n-1", session);

        assertEquals(Boolean.TRUE, n.getIsRead());
        verify(notificationRepository).save(n);
    }

    // ==================== markAllNotificationsAsRead ====================

    @Test
    void markAllNotificationsAsRead_notLoggedIn_throwsUnauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> userService.markAllNotificationsAsRead(session));
    }

    @Test
    void markAllNotificationsAsRead_marksEveryNotification() {
        when(session.getAttribute("userId")).thenReturn(1L);
        Notification n1 = new Notification();
        Notification n2 = new Notification();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(n1, n2));

        userService.markAllNotificationsAsRead(session);

        assertEquals(Boolean.TRUE, n1.getIsRead());
        assertEquals(Boolean.TRUE, n2.getIsRead());
        verify(notificationRepository).saveAll(anyList());
    }

    // ==================== deleteNotification ====================

    @Test
    void deleteNotification_notLoggedIn_throwsUnauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> userService.deleteNotification("n-1", session));
    }

    @Test
    void deleteNotification_notFound_throwsNotFound() {
        when(session.getAttribute("userId")).thenReturn(1L);
        when(notificationRepository.findById("n-1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.deleteNotification("n-1", session));
    }

    @Test
    void deleteNotification_wrongUser_throwsUnauthorized() {
        when(session.getAttribute("userId")).thenReturn(1L);
        Notification n = new Notification();
        n.setUserId(2L);
        when(notificationRepository.findById("n-1")).thenReturn(Optional.of(n));

        assertThrows(UnauthorizedException.class, () -> userService.deleteNotification("n-1", session));
    }

    @Test
    void deleteNotification_validRequest_deletes() {
        when(session.getAttribute("userId")).thenReturn(1L);
        Notification n = new Notification();
        n.setUserId(1L);
        when(notificationRepository.findById("n-1")).thenReturn(Optional.of(n));

        userService.deleteNotification("n-1", session);

        verify(notificationRepository).deleteById("n-1");
    }

    // ==================== validateSettingsRequest ====================

    private SettingsDTO settingsReq(String type, String metric, Float threshold, String alertType) {
        return new SettingsDTO(null, type, metric, threshold, alertType);
    }

    @Test
    void validateSettingsRequest_missingType_returnsMessage() {
        assertEquals("type is required", userService.validateSettingsRequest(settingsReq(null, "Ozone", 10f, "above")));
        assertEquals("type is required", userService.validateSettingsRequest(settingsReq("  ", "Ozone", 10f, "above")));
    }

    @Test
    void validateSettingsRequest_missingMetric_returnsMessage() {
        assertEquals("metric is required", userService.validateSettingsRequest(settingsReq("Air", null, 10f, "above")));
        assertEquals("metric is required", userService.validateSettingsRequest(settingsReq("Air", " ", 10f, "above")));
    }

    @Test
    void validateSettingsRequest_missingAlertType_returnsMessage() {
        assertEquals("alertType is required", userService.validateSettingsRequest(settingsReq("Air", "Ozone", 10f, null)));
        assertEquals("alertType is required", userService.validateSettingsRequest(settingsReq("Air", "Ozone", 10f, " ")));
    }

    @Test
    void validateSettingsRequest_missingThreshold_returnsMessage() {
        assertEquals("thresholdValue is required", userService.validateSettingsRequest(settingsReq("Air", "Ozone", null, "above")));
    }

    @Test
    void validateSettingsRequest_negativeThreshold_returnsMessage() {
        assertEquals("thresholdValue must be a non-negative number",
                userService.validateSettingsRequest(settingsReq("Air", "Ozone", -1f, "above")));
    }

    @Test
    void validateSettingsRequest_invalidType_returnsMessage() {
        String result = userService.validateSettingsRequest(settingsReq("Weather", "Humidity", 10f, "above"));
        assertTrue(result.startsWith("Invalid type: must be one of"));
    }

    @Test
    void validateSettingsRequest_invalidMetricForType_returnsMessage() {
        String result = userService.validateSettingsRequest(settingsReq("Air", "Smog", 10f, "above"));
        assertTrue(result.startsWith("Invalid metric 'Smog' for type 'Air'"));
    }

    @Test
    void validateSettingsRequest_invalidAlertType_returnsMessage() {
        String result = userService.validateSettingsRequest(settingsReq("Air", "Ozone", 10f, "sideways"));
        assertTrue(result.startsWith("Invalid alertType 'sideways'"));
    }

    @Test
    void validateSettingsRequest_validRequest_returnsNull() {
        assertNull(userService.validateSettingsRequest(settingsReq("Air", "Ozone", 10f, "above")));
    }
}
