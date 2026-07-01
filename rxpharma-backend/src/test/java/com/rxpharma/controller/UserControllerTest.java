package com.rxpharma.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxpharma.dto.request.ChangePasswordRequest;
import com.rxpharma.dto.request.UpdateRoleRequest;
import com.rxpharma.entity.User;
import com.rxpharma.security.CustomUserDetailsService;
import com.rxpharma.security.JwtUtil;
import com.rxpharma.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = UserController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private User createTestUser() {
        return User.builder().id(1L).fullName("Abebe Kebede")
                .email("abebe@rxpharma.com").role(User.Role.ADMIN)
                .approved(true).build();
    }

    @Test
    void getAllUsers_returnsList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(createTestUser()));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("abebe@rxpharma.com"));
    }

    @Test
    void getPendingUsers_returnsList() throws Exception {
        User pending = User.builder().id(2L).fullName("Pending").approved(false).build();
        when(userService.getPendingUsers()).thenReturn(List.of(pending));

        mockMvc.perform(get("/api/users/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].approved").value(false));
    }

    @Test
    void getUserById_found() throws Exception {
        when(userService.getUserById(1L)).thenReturn(createTestUser());

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("abebe@rxpharma.com"));
    }

    @Test
    void updateUser_success() throws Exception {
        User updated = User.builder().id(1L).fullName("Updated Name")
                .email("updated@rxpharma.com").role(User.Role.PHARMACIST).build();
        when(userService.updateUser(eq(1L), eq("Updated Name"), eq("updated@rxpharma.com"), eq(User.Role.PHARMACIST)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/users/1")
                        .param("fullName", "Updated Name")
                        .param("email", "updated@rxpharma.com")
                        .param("role", "PHARMACIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"));
    }

    @Test
    void deleteUser_noContent() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void changePassword_success() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPass");
        request.setNewPassword("newPass");
        request.setConfirmPassword("newPass");

        mockMvc.perform(patch("/api/users/1/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    void adminResetPassword_success() throws Exception {
        mockMvc.perform(patch("/api/users/1/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("newPassword", "newPass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));
    }

    @Test
    void updateRole_success() throws Exception {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRole(User.Role.PHARMACIST);

        User updated = createTestUser();
        updated.setRole(User.Role.PHARMACIST);
        when(userService.updateRole(1L, User.Role.PHARMACIST)).thenReturn(updated);

        mockMvc.perform(patch("/api/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role updated successfully"))
                .andExpect(jsonPath("$.newRole").value("PHARMACIST"));
    }

    @Test
    void approveUser_success() throws Exception {
        User approved = createTestUser();
        approved.setApproved(true);
        when(userService.approveUser(1L)).thenReturn(approved);

        mockMvc.perform(patch("/api/users/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User approved successfully"));
    }

    @Test
    void denyUser_success() throws Exception {
        mockMvc.perform(delete("/api/users/1/deny"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registration denied and removed"));
    }
}
