package com.rxpharma.service;

import com.rxpharma.entity.PasswordResetToken;
import com.rxpharma.entity.User;
import com.rxpharma.exception.BadRequestException;
import com.rxpharma.exception.ResourceNotFoundException;
import com.rxpharma.repository.PasswordResetTokenRepository;
import com.rxpharma.repository.UserRepository;
import com.rxpharma.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private PasswordResetTokenRepository tokenRepository;

    @InjectMocks private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .fullName("Abebe Kebede")
                .email("abebe@rxpharma.com")
                .password("encodedPassword")
                .role(User.Role.ADMIN)
                .approved(true)
                .build();
    }

    // ─── login ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login - success returns User")
    void login_success() {
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("abebe@rxpharma.com")).thenReturn(Optional.of(testUser));

        User result = authService.login("abebe@rxpharma.com", "password123");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("abebe@rxpharma.com");
        assertThat(result.getRole()).isEqualTo(User.Role.ADMIN);
    }

    @Test
    @DisplayName("login - wrong credentials throws exception")
    void login_wrongCredentials_throwsException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login("abebe@rxpharma.com", "wrongpassword"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login - user not found after auth throws RuntimeException")
    void login_userNotFound_throwsRuntimeException() {
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("missing@rxpharma.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("missing@rxpharma.com", "pass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ─── register ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register - success returns JWT token")
    void register_success_returnsToken() {
        when(userRepository.existsByEmail("new@rxpharma.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken("new@rxpharma.com", "ADMIN")).thenReturn("jwt-token");

        String token = authService.register("Abebe Kebede", "new@rxpharma.com",
                "password123", User.Role.ADMIN);

        assertThat(token).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("register - duplicate email throws RuntimeException")
    void register_duplicateEmail_throwsException() {
        when(userRepository.existsByEmail("abebe@rxpharma.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("Abebe", "abebe@rxpharma.com",
                "pass", User.Role.PHARMACIST))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already in use");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - different roles are accepted")
    void register_differentRoles() {
        for (User.Role role : User.Role.values()) {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(userRepository.save(any())).thenReturn(testUser);
            when(jwtUtil.generateToken(any(), any())).thenReturn("token");

            String result = authService.register("Test", role.name() + "@test.com", "pass", role);
            assertThat(result).isNotNull();
        }
    }

    // ─── forgotPassword ──────────────────────────────────────────────────────

    @Test
    @DisplayName("forgotPassword - success returns reset token string")
    void forgotPassword_success_returnsToken() {
        when(userRepository.findByEmail("abebe@rxpharma.com")).thenReturn(Optional.of(testUser));
        doNothing().when(tokenRepository).deleteByUserId(1L);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(i -> i.getArgument(0));

        String token = authService.forgotPassword("abebe@rxpharma.com");

        assertThat(token).isNotNull();
        assertThat(token).hasSize(32);
        assertThat(token).isUpperCase();
        verify(tokenRepository).deleteByUserId(1L);
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("forgotPassword - unknown email throws ResourceNotFoundException")
    void forgotPassword_unknownEmail_throwsException() {
        when(userRepository.findByEmail("ghost@rxpharma.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.forgotPassword("ghost@rxpharma.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No account found with email");
    }

    @Test
    @DisplayName("forgotPassword - old tokens are deleted before creating new one")
    void forgotPassword_deletesOldTokensFirst() {
        when(userRepository.findByEmail("abebe@rxpharma.com")).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.forgotPassword("abebe@rxpharma.com");

        verify(tokenRepository).deleteByUserId(1L);
    }

    // ─── resetPassword ───────────────────────────────────────────────────────

    @Test
    @DisplayName("resetPassword - success updates user password")
    void resetPassword_success() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("VALIDTOKEN123")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();

        when(tokenRepository.findByToken("VALIDTOKEN123")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNew");
        when(userRepository.save(any())).thenReturn(testUser);
        when(tokenRepository.save(any())).thenReturn(resetToken);

        authService.resetPassword("VALIDTOKEN123", "newPassword", "newPassword");

        verify(userRepository).save(argThat(u -> u.getPassword().equals("encodedNew")));
        assertThat(resetToken.isUsed()).isTrue();
    }

    @Test
    @DisplayName("resetPassword - mismatched passwords throws BadRequestException")
    void resetPassword_mismatchedPasswords_throwsException() {
        assertThatThrownBy(() -> authService.resetPassword("TOKEN", "password1", "password2"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("do not match");
    }

    @Test
    @DisplayName("resetPassword - invalid token throws BadRequestException")
    void resetPassword_invalidToken_throwsException() {
        when(tokenRepository.findByToken("BADTOKEN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("BADTOKEN", "pass", "pass"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid reset token");
    }

    @Test
    @DisplayName("resetPassword - expired token throws BadRequestException")
    void resetPassword_expiredToken_throwsException() {
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .token("EXPIREDTOKEN")
                .user(testUser)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();

        when(tokenRepository.findByToken("EXPIREDTOKEN")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.resetPassword("EXPIREDTOKEN", "pass", "pass"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("resetPassword - already used token throws BadRequestException")
    void resetPassword_alreadyUsedToken_throwsException() {
        PasswordResetToken usedToken = PasswordResetToken.builder()
                .token("USEDTOKEN")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(true)
                .build();

        when(tokenRepository.findByToken("USEDTOKEN")).thenReturn(Optional.of(usedToken));

        assertThatThrownBy(() -> authService.resetPassword("USEDTOKEN", "pass", "pass"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been used");
    }

    // ─── verifyResetToken ────────────────────────────────────────────────────

    @Test
    @DisplayName("verifyResetToken - valid token returns true")
    void verifyResetToken_validToken_returnsTrue() {
        PasswordResetToken validToken = PasswordResetToken.builder()
                .token("VALID")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();

        when(tokenRepository.findByToken("VALID")).thenReturn(Optional.of(validToken));

        assertThat(authService.verifyResetToken("VALID")).isTrue();
    }

    @Test
    @DisplayName("verifyResetToken - expired token returns false")
    void verifyResetToken_expiredToken_returnsFalse() {
        PasswordResetToken expired = PasswordResetToken.builder()
                .token("EXPIRED")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();

        when(tokenRepository.findByToken("EXPIRED")).thenReturn(Optional.of(expired));

        assertThat(authService.verifyResetToken("EXPIRED")).isFalse();
    }

    @Test
    @DisplayName("verifyResetToken - used token returns false")
    void verifyResetToken_usedToken_returnsFalse() {
        PasswordResetToken used = PasswordResetToken.builder()
                .token("USED")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(true)
                .build();

        when(tokenRepository.findByToken("USED")).thenReturn(Optional.of(used));

        assertThat(authService.verifyResetToken("USED")).isFalse();
    }

    @Test
    @DisplayName("verifyResetToken - unknown token returns false")
    void verifyResetToken_unknownToken_returnsFalse() {
        when(tokenRepository.findByToken("UNKNOWN")).thenReturn(Optional.empty());

        assertThat(authService.verifyResetToken("UNKNOWN")).isFalse();
    }
}