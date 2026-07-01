package com.rxpharma.service;

import com.rxpharma.entity.User;
import com.rxpharma.exception.BadRequestException;
import com.rxpharma.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User pendingUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .fullName("Abebe Kebede")
                .email("abebe@rxpharma.com")
                .password("encodedPassword")
                .role(User.Role.ADMIN)
                .approved(true)
                .authProvider("LOCAL")
                .build();

        pendingUser = User.builder()
                .id(2L)
                .fullName("Pending User")
                .email("pending@gmail.com")
                .password("encodedPassword")
                .role(User.Role.PHARMACIST)
                .approved(false)
                .authProvider("GOOGLE")
                .build();
    }

    // ─── getAllUsers ─────────────────────────────────────────────

    @Test
    @DisplayName("getAllUsers - returns all users")
    void getAllUsers_returnsAll() {
        when(userRepository.findAll()).thenReturn(List.of(testUser, pendingUser));

        List<User> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("getAllUsers - empty list")
    void getAllUsers_empty() {
        when(userRepository.findAll()).thenReturn(List.of());

        assertThat(userService.getAllUsers()).isEmpty();
    }

    // ─── getUserById ─────────────────────────────────────────────

    @Test
    void getUserById_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User result = userService.getUserById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getUserById_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ─── getUserByEmail ──────────────────────────────────────────

    @Test
    void getUserByEmail_found() {
        when(userRepository.findByEmail("abebe@rxpharma.com"))
                .thenReturn(Optional.of(testUser));

        User result = userService.getUserByEmail("abebe@rxpharma.com");

        assertThat(result.getEmail()).isEqualTo("abebe@rxpharma.com");
    }

    @Test
    void getUserByEmail_notFound() {
        when(userRepository.findByEmail("ghost@rxpharma.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("ghost@rxpharma.com"))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── updateUser ──────────────────────────────────────────────

    @Test
    void updateUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.updateUser(
                1L,
                "Abebe Updated",
                "updated@rxpharma.com",
                User.Role.PHARMACIST
        );

        assertThat(result.getFullName()).isEqualTo("Abebe Updated");
        assertThat(result.getEmail()).isEqualTo("updated@rxpharma.com");
        assertThat(result.getRole()).isEqualTo(User.Role.PHARMACIST);
    }

    // ─── deleteUser ──────────────────────────────────────────────

    @Test
    void deleteUser_success() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_notFound() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── changePassword ──────────────────────────────────────────

    @Test
    void changePassword_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPass", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.matches("newPass", "encodedPassword")).thenReturn(false);
        when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPass");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userService.changePassword(1L, "currentPass", "newPass", "newPass");

        verify(userRepository).save(any(User.class));
    }

    // ─── approveUser ─────────────────────────────────────────────

    @Test
    void approveUser_success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(pendingUser));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = userService.approveUser(2L);

        assertThat(result.isApproved()).isTrue();
    }

    // ─── denyUser ────────────────────────────────────────────────

    @Test
    void denyUser_success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(pendingUser));

        userService.denyUser(2L);

        verify(userRepository).delete(pendingUser);
    }

    @Test
    void denyUser_alreadyApproved() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userService.denyUser(1L))
                .isInstanceOf(BadRequestException.class);
    }

    // ─── getPendingUsers ────────────────────────────────────────

    @Test
    void getPendingUsers_returnsList() {
        when(userRepository.findByApprovedFalse()).thenReturn(List.of(pendingUser));

        List<User> result = userService.getPendingUsers();

        assertThat(result).hasSize(1);
    }
}