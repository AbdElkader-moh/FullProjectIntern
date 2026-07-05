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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.backend.user.dto.ChangePasswordRequest;
import com.backend.user.dto.LoginRequest;
import com.backend.user.dto.SignupRequest;
import com.backend.user.dto.UpdateProfilePictureRequest;
import com.backend.user.dto.UserResponse;
import com.backend.user.entity.User;
import com.backend.user.exception.ConflictException;
import com.backend.user.exception.NotFoundException;
import com.backend.user.exception.UnauthorizedException;
import com.backend.user.repository.UserRepository;
import com.backend.user.util.JwtUtil;
import com.cloudinary.Cloudinary;

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
}
