package com.rxpharma.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "my-super-secret-key-that-is-at-least-256-bits-long-for-hs256");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
    }

    @Test
    void generateAndExtractEmail() {
        String token = jwtUtil.generateToken("test@rxpharma.com", "ADMIN");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("test@rxpharma.com");
    }

    @Test
    void extractRole() {
        String token = jwtUtil.generateToken("test@rxpharma.com", "PHARMACIST");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("PHARMACIST");
    }

    @Test
    void isTokenValid_withValidToken() {
        String token = jwtUtil.generateToken("test@rxpharma.com", "ADMIN");
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_withInvalidToken() {
        assertThat(jwtUtil.isTokenValid("invalid-jwt-token")).isFalse();
    }

    @Test
    void isTokenValid_withEmptyToken() {
        assertThat(jwtUtil.isTokenValid("")).isFalse();
    }
}
