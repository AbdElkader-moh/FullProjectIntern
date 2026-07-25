package com.backend.user.controller;

import com.backend.user.dto.NotificationDTO;
import com.backend.user.dto.SettingsDTO;
import com.backend.user.dto.UserResponse;
import com.backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpSession;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-layer tests for UserController. Service layer is mocked
 * (@MockitoBean UserService — Spring Boot 4 removed @MockBean, this is its
 * replacement from spring-test); GlobalExceptionHandler is picked up automatically
 * by @WebMvcTest since it's a @RestControllerAdvice in the app's component
 * scan, so validation-error branches (@Valid failures) are exercised
 * end-to-end through the real advice, not stubbed.
 *
 * NOTE: UserResponse in the currently uploaded source still includes the
 * password field (C4 in the execution plan calls for removing it). These
 * tests assert against the response shape as it exists today; if/when C4
 * is applied, the assertions referencing "password" in the JSON body
 * should be removed accordingly — flagging this rather than silently
 * fixing it, since that's a separate task from adding test coverage.
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // ---------- POST /api/users/signup ----------

    @Test
    void signup_validRequest_returns201WithAuthHeaderAndBody() throws Exception {
        UserResponse response = new UserResponse(1L, "user@example.com", "John", "Doe", "http://pic", "hash");
        when(userService.signup(any())).thenReturn(response);
        when(userService.login(any(), any(HttpSession.class))).thenReturn("Login successful");
        when(userService.generateTokenForUser("user@example.com")).thenReturn("token123");

        MockMultipartFile file = new MockMultipartFile(
                "profilePicture", "pic.png", MediaType.IMAGE_PNG_VALUE, "img-bytes".getBytes());

        mockMvc.perform(multipart("/api/users/signup")
                        .file(file)
                        .param("email", "user@example.com")
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("password", "abc123"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Authorization", "Bearer token123"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void signup_passwordTooShort_returns400ValidationError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "profilePicture", "pic.png", MediaType.IMAGE_PNG_VALUE, "img-bytes".getBytes());

        mockMvc.perform(multipart("/api/users/signup")
                        .file(file)
                        .param("email", "user@example.com")
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("password", "abc")) // < 6 chars
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("All Fields Must be filled"));
    }

    // ---------- POST /api/users/login ----------

    @Test
    void login_validCredentials_returns200WithAuthHeader() throws Exception {
        when(userService.login(any(), any(HttpSession.class))).thenReturn("Login successful");
        when(userService.generateTokenForUser("user@example.com")).thenReturn("token123");

        String body = """
                {"email":"user@example.com","password":"abc123"}
                """;

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", "Bearer token123"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void login_invalidEmailFormat_returns400ValidationError() throws Exception {
        String body = """
                {"email":"not-an-email","password":"abc123"}
                """;

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("All Fields Must be filled"));
    }

    @Test
    void login_blankPassword_returns400ValidationError() throws Exception {
        String body = """
                {"email":"user@example.com","password":""}
                """;

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /api/users/me ----------

    @Test
    void getProfile_returns200WithUserResponse() throws Exception {
        UserResponse response = new UserResponse(1L, "user@example.com", "John", "Doe", "http://pic", "hash");
        when(userService.getCurrentUser(any(HttpSession.class))).thenReturn(response);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    // ---------- PUT /api/users/me ----------

    @Test
    void updateProfilePicture_returns200WithUpdatedUserResponse() throws Exception {
        UserResponse response = new UserResponse(1L, "user@example.com", "John", "Doe", "http://newpic", "hash");
        when(userService.updateProfilePicture(any(), any(HttpSession.class))).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "profilePicture", "new.png", MediaType.IMAGE_PNG_VALUE, "new-bytes".getBytes());

        mockMvc.perform(multipart("/api/users/me")
                        .file(file)
                        .with(req -> { req.setMethod("PUT"); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profilePicture").value("http://newpic"));
    }

    // ---------- POST /api/users/logout ----------

    @Test
    void logout_returns200WithConfirmationMessage() throws Exception {
        mockMvc.perform(post("/api/users/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(userService).logout(any(HttpSession.class));
    }

    // ---------- PUT /api/users/me/password ----------

    @Test
    void changePassword_validRequest_returns200() throws Exception {
        String body = """
                {"oldPassword":"old123","newPassword":"new456"}
                """;

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully."));

        verify(userService).changePassword(any(), any(HttpSession.class));
    }

    @Test
    void changePassword_blankNewPassword_returns400ValidationError() throws Exception {
        String body = """
                {"oldPassword":"old123","newPassword":""}
                """;

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /api/users/settings ----------

    @Test
    void getSettings_returns200WithList() throws Exception {
        SettingsDTO setting = new SettingsDTO("id-1", "Traffic", "trafficDensity", 80f, "above");
        when(userService.getSettings(any(HttpSession.class))).thenReturn(List.of(setting));

        mockMvc.perform(get("/api/users/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("id-1"))
                .andExpect(jsonPath("$[0].type").value("Traffic"));
    }

    // ---------- POST /api/users/settings ----------

    @Test
    void updateSettings_validRequest_returns200WithSavedSetting() throws Exception {
        when(userService.validateSettingsRequest(any())).thenReturn(null);
        SettingsDTO saved = new SettingsDTO("id-1", "Traffic", "trafficDensity", 80f, "above");
        when(userService.addSetting(any(), any(HttpSession.class))).thenReturn(saved);

        String body = """
                {"type":"Traffic","metric":"trafficDensity","thresholdValue":80.0,"alertType":"above"}
                """;

        mockMvc.perform(post("/api/users/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("id-1"));
    }

    @Test
    void updateSettings_serviceLevelValidationError_returns400WithApiResponse() throws Exception {
        when(userService.validateSettingsRequest(any()))
                .thenReturn("Threshold for trafficDensity must be between 0 and 500.");

        String body = """
                {"type":"Traffic","metric":"trafficDensity","thresholdValue":9999.0,"alertType":"above"}
                """;

        mockMvc.perform(post("/api/users/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Threshold for trafficDensity must be between 0 and 500."));
    }

    // ---------- PUT /api/users/settings/{id} ----------

    @Test
    void updateSetting_returns200WithUpdatedSetting() throws Exception {
        SettingsDTO updated = new SettingsDTO("id-1", "Air", "ozone", 150f, "below");
        when(userService.updateSetting(eq("id-1"), any(), any(HttpSession.class))).thenReturn(updated);

        String body = """
                {"type":"Air","metric":"ozone","thresholdValue":150.0,"alertType":"below"}
                """;

        mockMvc.perform(put("/api/users/settings/id-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metric").value("ozone"));
    }

    // ---------- DELETE /api/users/settings/{id} ----------

    @Test
    void deleteSetting_returns200WithConfirmationMessage() throws Exception {
        mockMvc.perform(delete("/api/users/settings/id-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Setting deleted successfully"));

        verify(userService).deleteSetting(eq("id-1"), any(HttpSession.class));
    }

    // ---------- GET /api/users/notifications ----------

    @Test
    void getNotifications_returns200WithList() throws Exception {
        NotificationDTO n = new NotificationDTO();
        n.setId("notif-1");
        n.setType("Traffic");
        when(userService.getNotifications(any(HttpSession.class))).thenReturn(List.of(n));

        mockMvc.perform(get("/api/users/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("notif-1"));
    }

    // ---------- PUT /api/users/notifications/{id}/read ----------

    @Test
    void markNotificationAsRead_returns200WithConfirmationMessage() throws Exception {
        mockMvc.perform(put("/api/users/notifications/notif-1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification marked as read"));

        verify(userService).markNotificationAsRead(eq("notif-1"), any(HttpSession.class));
    }

    // ---------- PUT /api/users/notifications/read-all ----------

    @Test
    void markAllNotificationsAsRead_returns200WithConfirmationMessage() throws Exception {
        mockMvc.perform(put("/api/users/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All notifications marked as read"));

        verify(userService).markAllNotificationsAsRead(any(HttpSession.class));
    }

    // ---------- DELETE /api/users/notifications/{id} ----------

    @Test
    void deleteNotification_returns200WithConfirmationMessage() throws Exception {
        mockMvc.perform(delete("/api/users/notifications/notif-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification deleted successfully"));

        verify(userService).deleteNotification(eq("notif-1"), any(HttpSession.class));
    }
}
