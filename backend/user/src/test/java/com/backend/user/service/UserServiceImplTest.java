package com.backend.user.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Captor;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.backend.user.dto.ChangePasswordRequest;
import com.backend.user.dto.LoginRequest;
import com.backend.user.dto.SettingsDTO;
import com.backend.user.dto.SignupRequest;
import com.backend.user.dto.UpdateProfilePictureRequest;
import com.backend.user.dto.UserResponse;
import com.backend.user.entity.Settings;
import com.backend.user.entity.User;
import com.backend.user.exception.ConflictException;
import com.backend.user.exception.ExternalServiceException;
import com.backend.user.exception.NotFoundException;
import com.backend.user.exception.UnauthorizedException;
import com.backend.user.repository.UserRepository;
import com.backend.user.util.JwtUtil;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.backend.user.repository.SettingsRepository settingsRepository;

    @Mock
    private com.backend.user.repository.NotificationRepository notificationRepository;
    @Mock
    private Cloudinary cloudinary;
    @Mock
    private HttpSession session;

    @Mock
    private JwtUtil jwtUtil;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private UserServiceImpl userService;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, settingsRepository, notificationRepository, cloudinary, jwtUtil);
        passwordEncoder = new BCryptPasswordEncoder();
    }

    // ---------- SIGNUP TESTS ----------
    @Test
    void signup_validRequest_shouldCreateUser() {
        SignupRequest request = new SignupRequest();
        request.setEmail("test@test.com");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setPassword("123456");
        request.setProfilePicture(new org.springframework.mock.web.MockMultipartFile("file", new byte[0]));

        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail("test@test.com");
        savedUser.setFirstName("Test");
        savedUser.setLastName("User");
        savedUser.setProfilePicture("");
        savedUser.setPassword("hashed-password");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.signup(request);

        assertEquals(1L, response.getId());
        assertEquals("test@test.com", response.getEmail());
        assertEquals("Test", response.getFirstName());
        assertEquals("User", response.getLastName());

        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertEquals("test@test.com", capturedUser.getEmail());
        assertTrue(passwordEncoder.matches("123456", capturedUser.getPassword()));
    }

    @Test
    void signup_existingEmail_shouldThrowConflictException() {
        SignupRequest request = new SignupRequest();
        request.setEmail("test@test.com");

        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.signup(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void signup_cloudinaryNotConfigured_shouldThrowExternalServiceException() {
        UserServiceImpl serviceWithNoCloudinary = new UserServiceImpl(
                userRepository, settingsRepository, notificationRepository, null, jwtUtil);

        SignupRequest request = new SignupRequest();
        request.setEmail("test@test.com");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setPassword("123456");
        request.setProfilePicture(new org.springframework.mock.web.MockMultipartFile("file", new byte[]{1, 2, 3}));

        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);

        assertThrows(ExternalServiceException.class, () -> serviceWithNoCloudinary.signup(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void signup_cloudinaryUploadFails_shouldThrowExternalServiceException() throws IOException {
        SignupRequest request = new SignupRequest();
        request.setEmail("test@test.com");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setPassword("123456");
        request.setProfilePicture(new org.springframework.mock.web.MockMultipartFile("file", new byte[]{1, 2, 3}));

        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);

        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any())).thenThrow(new IOException("network error"));

        assertThrows(ExternalServiceException.class, () -> userService.signup(request));

        verify(userRepository, never()).save(any(User.class));
    }

    // ---------- LOGIN TESTS ----------
    @Test
    void login_validCredentials_shouldSetSessionAndReturnMessage() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("123456");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setPassword(passwordEncoder.encode("123456"));

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        String result = userService.login(request, session);

        assertEquals("Login successful", result);
        verify(session).setAttribute("userId", 1L);
    }

    @Test
    void login_emailNotFound_shouldThrowUnauthorizedException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@test.com");
        request.setPassword("123456");

        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> userService.login(request, session));

        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    void login_wrongPassword_shouldThrowUnauthorizedException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("wrong-password");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setPassword(passwordEncoder.encode("123456"));

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> userService.login(request, session));

        verify(session, never()).setAttribute(anyString(), any());
    }

    // ---------- GET CURRENT USER TESTS ----------
    @Test
    void getCurrentUser_loggedIn_shouldReturnUserResponse() {
        when(session.getAttribute("userId")).thenReturn(1L);

        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setProfilePicture("image-data");
        user.setPassword("hashed-password");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getCurrentUser(session);

        assertEquals(1L, response.getId());
        assertEquals("test@test.com", response.getEmail());
        assertEquals("Test", response.getFirstName());
        assertEquals("User", response.getLastName());
    }

    @Test
    void getCurrentUser_notLoggedIn_shouldThrowUnauthorizedException() {
        when(session.getAttribute("userId")).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> userService.getCurrentUser(session));

        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void getCurrentUser_userNotFound_shouldThrowNotFoundException() {
        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getCurrentUser(session));
    }

    // ---------- UPDATE PROFILE PICTURE TESTS ----------
    @Test
    void updateProfilePicture_loggedIn_shouldUpdatePicture() {
        UpdateProfilePictureRequest request = new UpdateProfilePictureRequest();
        request.setProfilePicture(new org.springframework.mock.web.MockMultipartFile("file", new byte[0]));

        when(session.getAttribute("userId")).thenReturn(1L);

        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setProfilePicture("old-image-data");
        user.setPassword("hashed-password");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateProfilePicture(request, session);

        assertEquals("old-image-data", response.getProfilePicture());

        verify(userRepository).save(userCaptor.capture());
        assertEquals("old-image-data", userCaptor.getValue().getProfilePicture());
    }

    @Test
    void updateProfilePicture_notLoggedIn_shouldThrowUnauthorizedException() {
        UpdateProfilePictureRequest request = new UpdateProfilePictureRequest();
        request.setProfilePicture(new org.springframework.mock.web.MockMultipartFile("file", new byte[0]));

        when(session.getAttribute("userId")).thenReturn(null);

        assertThrows(UnauthorizedException.class,
                () -> userService.updateProfilePicture(request, session));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfilePicture_userNotFound_shouldThrowNotFoundException() {
        UpdateProfilePictureRequest request = new UpdateProfilePictureRequest();
        request.setProfilePicture(new org.springframework.mock.web.MockMultipartFile("file", new byte[0]));

        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.updateProfilePicture(request, session));
    }

    @Test
    void updateProfilePicture_cloudinaryNotConfigured_shouldThrowExternalServiceException() {
        UserServiceImpl serviceWithNoCloudinary = new UserServiceImpl(
                userRepository, settingsRepository, notificationRepository, null, jwtUtil);

        UpdateProfilePictureRequest request = new UpdateProfilePictureRequest();
        request.setProfilePicture(new org.springframework.mock.web.MockMultipartFile("file", new byte[]{1, 2, 3}));

        when(session.getAttribute("userId")).thenReturn(1L);

        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(ExternalServiceException.class,
                () -> serviceWithNoCloudinary.updateProfilePicture(request, session));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfilePicture_cloudinaryUploadFails_shouldThrowExternalServiceException() throws IOException {
        UpdateProfilePictureRequest request = new UpdateProfilePictureRequest();
        request.setProfilePicture(new org.springframework.mock.web.MockMultipartFile("file", new byte[]{1, 2, 3}));

        when(session.getAttribute("userId")).thenReturn(1L);

        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any())).thenThrow(new IOException("network error"));

        assertThrows(ExternalServiceException.class,
                () -> userService.updateProfilePicture(request, session));

        verify(userRepository, never()).save(any(User.class));
    }

    // ---------- CHANGE PASSWORD TESTS ----------
    @Test
    void changePassword_validOldPassword_shouldUpdatePassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("old123");
        request.setNewPassword("new123");

        when(session.getAttribute("userId")).thenReturn(1L);

        User user = new User();
        user.setId(1L);
        user.setPassword(passwordEncoder.encode("old123"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changePassword(request, session);

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertTrue(passwordEncoder.matches("new123", savedUser.getPassword()));
        assertFalse(passwordEncoder.matches("old123", savedUser.getPassword()));
    }

    @Test
    void changePassword_notLoggedIn_shouldThrowUnauthorizedException() {
        ChangePasswordRequest request = new ChangePasswordRequest();

        when(session.getAttribute("userId")).thenReturn(null);

        assertThrows(UnauthorizedException.class,
                () -> userService.changePassword(request, session));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_userNotFound_shouldThrowNotFoundException() {
        ChangePasswordRequest request = new ChangePasswordRequest();

        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.changePassword(request, session));
    }

    @Test
    void generateTokenForUser_validEmail_shouldReturnToken() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("1", "test@test.com")).thenReturn("mocked-jwt-token");

        String token = userService.generateTokenForUser("test@test.com");

        assertEquals("mocked-jwt-token", token);
    }

    @Test
    void generateTokenForUser_emailNotFound_shouldThrowUnauthorizedException() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> userService.generateTokenForUser("missing@test.com"));
    }

    @Test
    void changePassword_wrongOldPassword_shouldThrowUnauthorizedException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrong-old");
        request.setNewPassword("new123");

        when(session.getAttribute("userId")).thenReturn(1L);

        User user = new User();
        user.setId(1L);
        user.setPassword(passwordEncoder.encode("correct-old"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class,
                () -> userService.changePassword(request, session));

        verify(userRepository, never()).save(any(User.class));
    }

    // ---------- LOGOUT TEST ----------
    @Test
    void logout_shouldInvalidateSession() {
        userService.logout(session);

        verify(session).invalidate();
    }

    // ---------- UPDATE SETTINGS (BULK UPSERT) TESTS ----------
    @Test
    void updateSettings_notLoggedIn_shouldThrowUnauthorizedException() {
        when(session.getAttribute("userId")).thenReturn(null);

        List<SettingsDTO> requests = List.of(
                new SettingsDTO(null, "Traffic", "Traffic Density", 100.0f, "above"));

        assertThrows(UnauthorizedException.class,
                () -> userService.updateSettings(requests, session));

        verify(settingsRepository, never()).save(any(Settings.class));
    }

    @Test
    void updateSettings_foundById_shouldUpdateInPlace() {
        when(session.getAttribute("userId")).thenReturn(1L);

        Settings existing = new Settings(1L, "Traffic", "Traffic Density", 80.0f, "above");
        existing.setId("setting-1");

        SettingsDTO req = new SettingsDTO("setting-1", "Traffic", "Traffic Density", 120.0f, "above");

        when(settingsRepository.findById("setting-1")).thenReturn(Optional.of(existing));
        when(settingsRepository.findByUserId(1L)).thenReturn(List.of(existing));

        userService.updateSettings(List.of(req), session);

        assertEquals(120.0f, existing.getThresholdValue(), 0.001f);
        verify(settingsRepository).save(existing);
        verify(settingsRepository, never()).findByUserIdAndTypeAndMetric(anyLong(), anyString(), anyString());
    }

    @Test
    void updateSettings_idNotFoundButMatchesByTypeAndMetric_shouldUpdateInPlace() {
        when(session.getAttribute("userId")).thenReturn(1L);

        Settings existing = new Settings(1L, "Air", "Carbon Monoxide", 50.0f, "above");
        existing.setId("setting-2");

        // Stale/unknown id supplied, but type+metric matches an existing row.
        SettingsDTO req = new SettingsDTO("stale-id", "Air", "Carbon Monoxide", 75.0f, "above");

        when(settingsRepository.findById("stale-id")).thenReturn(Optional.empty());
        when(settingsRepository.findByUserIdAndTypeAndMetric(1L, "Air", "Carbon Monoxide"))
                .thenReturn(Optional.of(existing));
        when(settingsRepository.findByUserId(1L)).thenReturn(List.of(existing));

        userService.updateSettings(List.of(req), session);

        assertEquals(75.0f, existing.getThresholdValue(), 0.001f);
        verify(settingsRepository).save(existing);
    }

    @Test
    void updateSettings_noMatchFound_shouldInsertNew() {
        when(session.getAttribute("userId")).thenReturn(1L);

        SettingsDTO req = new SettingsDTO(null, "Light", "Brightness Level", 60.0f, "below");

        when(settingsRepository.findByUserIdAndTypeAndMetric(1L, "Light", "Brightness Level"))
                .thenReturn(Optional.empty());
        when(settingsRepository.findByUserId(1L)).thenReturn(List.of());

        userService.updateSettings(List.of(req), session);

        ArgumentCaptor<Settings> captor = ArgumentCaptor.forClass(Settings.class);
        verify(settingsRepository).save(captor.capture());
        assertEquals("Light", captor.getValue().getType());
        assertEquals(60.0f, captor.getValue().getThresholdValue(), 0.001f);
    }

    @Test
    void updateSettings_mixedBatch_shouldUpdateOneAndInsertOne() {
        when(session.getAttribute("userId")).thenReturn(1L);

        Settings existing = new Settings(1L, "Traffic", "Average Speed", 40.0f, "below");
        existing.setId("setting-3");

        SettingsDTO updateReq = new SettingsDTO("setting-3", "Traffic", "Average Speed", 55.0f, "below");
        SettingsDTO newReq = new SettingsDTO(null, "Air", "Ozone", 30.0f, "above");

        when(settingsRepository.findById("setting-3")).thenReturn(Optional.of(existing));
        when(settingsRepository.findByUserIdAndTypeAndMetric(1L, "Air", "Ozone"))
                .thenReturn(Optional.empty());
        when(settingsRepository.findByUserId(1L)).thenReturn(List.of(existing));

        userService.updateSettings(List.of(updateReq, newReq), session);

        assertEquals(55.0f, existing.getThresholdValue(), 0.001f);
        verify(settingsRepository, times(2)).save(any(Settings.class));
    }
}
