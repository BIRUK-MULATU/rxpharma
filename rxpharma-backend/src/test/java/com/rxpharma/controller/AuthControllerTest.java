package com.rxpharma.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxpharma.dto.request.LoginRequest;
import com.rxpharma.dto.request.RegisterRequest;
import com.rxpharma.entity.User;
import com.rxpharma.repository.UserRepository;
import com.rxpharma.security.CustomUserDetailsService;
import com.rxpharma.security.JwtUtil;
import com.rxpharma.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@TestPropertySource(properties = {"google.client.id=test-client-id"})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@rxpharma.com");
        request.setPassword("password123");

        User user = User.builder().id(1L).email("test@rxpharma.com")
                .fullName("Test").role(User.Role.ADMIN).approved(true).build();

        when(authService.login("test@rxpharma.com", "password123")).thenReturn(user);
        when(jwtUtil.generateToken("test@rxpharma.com", "ADMIN")).thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("test@rxpharma.com"));
    }

    @Test
    void login_notApproved_returns403() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("pending@rxpharma.com");
        request.setPassword("password123");

        User user = User.builder().id(1L).email("pending@rxpharma.com")
                .fullName("Pending").role(User.Role.CASHIER).approved(false).build();

        when(authService.login("pending@rxpharma.com", "password123")).thenReturn(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Your account is pending admin approval."));
    }

    @Test
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("New User");
        request.setEmail("new@rxpharma.com");
        request.setPassword("password123");
        request.setRole(User.Role.PHARMACIST);

        when(authService.register("New User", "new@rxpharma.com", "password123", User.Role.PHARMACIST))
                .thenReturn("jwt-token");

        User savedUser = User.builder().id(2L).email("new@rxpharma.com")
                .fullName("New User").role(User.Role.PHARMACIST).build();
        when(userRepository.findByEmail("new@rxpharma.com")).thenReturn(Optional.of(savedUser));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("new@rxpharma.com"));
    }
}
