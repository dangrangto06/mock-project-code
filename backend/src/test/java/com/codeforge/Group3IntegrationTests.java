package com.codeforge;

import com.codeforge.dto.AuthDto;
import com.codeforge.entity.User;
import com.codeforge.repository.UserRepository;
import com.codeforge.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class Group3IntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("group3tester")
                .email("group3tester@codeforge.io")
                .fullName("Group 3 Tester")
                .passwordHash(passwordEncoder.encode("OldPassword123!"))
                .role("Learner")
                .streakDays(3)
                .solvedCount(5)
                .acceptanceRate(BigDecimal.valueOf(80.00))
                .practiceTimeHours(BigDecimal.valueOf(2.5))
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Task 1: Forgot Password - Existing User generates token and returns 200 generic message")
    void testForgotPassword_ExistingUser() throws Exception {
        AuthDto.ForgotPasswordRequest request = new AuthDto.ForgotPasswordRequest("group3tester@codeforge.io");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account exists, a reset link has been sent."));

        User updatedUser = userRepository.findByEmail("group3tester@codeforge.io").orElseThrow();
        assertNotNull(updatedUser.getResetToken());
        assertNotNull(updatedUser.getResetTokenExpiry());
        assertTrue(updatedUser.getResetTokenExpiry().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Task 1: Forgot Password - Non-existent email returns 200 identical message without error (no enumeration)")
    void testForgotPassword_NonExistentEmail() throws Exception {
        AuthDto.ForgotPasswordRequest request = new AuthDto.ForgotPasswordRequest("nobody@codeforge.io");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account exists, a reset link has been sent."));
    }

    @Test
    @DisplayName("Task 1: Reset Password - Valid token resets password, clears token, and enables login with new password")
    void testResetPassword_Success() throws Exception {
        String token = UUID.randomUUID().toString();
        testUser.setResetToken(token);
        testUser.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(testUser);

        AuthDto.ResetPasswordRequest request = new AuthDto.ResetPasswordRequest(token, "BrandNewPassword456!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully."));

        User reloaded = userRepository.findByEmail("group3tester@codeforge.io").orElseThrow();
        assertNull(reloaded.getResetToken());
        assertNull(reloaded.getResetTokenExpiry());
        assertTrue(passwordEncoder.matches("BrandNewPassword456!", reloaded.getPasswordHash()));

        // Verify user can now authenticate with the new password
        AuthDto.LoginRequest loginReq = new AuthDto.LoginRequest("group3tester", "BrandNewPassword456!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.username").value("group3tester"));
    }

    @Test
    @DisplayName("Task 1: Reset Password - Expired or Invalid token throws 400 Bad Request")
    void testResetPassword_InvalidAndExpiredTokens() throws Exception {
        // 1. Completely bogus token
        AuthDto.ResetPasswordRequest bogusRequest = new AuthDto.ResetPasswordRequest("bogus-token-xyz", "NewPass123!");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bogusRequest)))
                .andExpect(status().isBadRequest());

        // 2. Expired token
        String expiredToken = UUID.randomUUID().toString();
        testUser.setResetToken(expiredToken);
        testUser.setResetTokenExpiry(LocalDateTime.now().minusMinutes(5));
        userRepository.save(testUser);

        AuthDto.ResetPasswordRequest expiredRequest = new AuthDto.ResetPasswordRequest(expiredToken, "NewPass123!");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expiredRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Task 2: Google Sign-In - Valid access token creates/authenticates user and returns AuthResponse")
    void testGoogleLogin_Success() throws Exception {
        RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);
        authService.setRestTemplate(mockRestTemplate);

        Map<String, Object> googleProfile = Map.of(
                "email", "newgoogleuser@codeforge.io",
                "name", "Google Coder"
        );

        Mockito.when(mockRestTemplate.exchange(
                eq("https://www.googleapis.com/oauth2/v3/userinfo"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(googleProfile));

        AuthDto.GoogleLoginRequest request = new AuthDto.GoogleLoginRequest("mock-google-access-token");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email").value("newgoogleuser@codeforge.io"))
                .andExpect(jsonPath("$.role").value("Learner"));

        assertTrue(userRepository.existsByEmail("newgoogleuser@codeforge.io"));
    }

    @Test
    @DisplayName("Task 3: GitHub OAuth - Unconfigured client redirects with oauthError=github_not_configured")
    void testGithubAuth_NotConfigured() throws Exception {
        Object originalClientId = org.springframework.test.util.ReflectionTestUtils.getField(authService, "githubClientId");
        try {
            org.springframework.test.util.ReflectionTestUtils.setField(authService, "githubClientId", "YOUR_GITHUB_CLIENT_ID");
            mockMvc.perform(get("/api/auth/github"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("oauthError=github_not_configured")));
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(authService, "githubClientId", originalClientId);
        }
    }

    @Test
    @DisplayName("Task 3: GitHub OAuth - Configured client redirects to GitHub authorize URL")
    void testGithubAuth_Configured() throws Exception {
        mockMvc.perform(get("/api/auth/github"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("https://github.com/login/oauth/authorize")));
    }

    @Test
    @DisplayName("Task 3: GitHub Callback - Missing code redirects with oauthError=missing_code")
    void testGithubCallback_MissingCode() throws Exception {
        mockMvc.perform(get("/api/auth/github/callback"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("oauthError=missing_code")));
    }

    @Test
    @DisplayName("Task 3: GitHub Callback - Successful code exchange redirects to frontend with oauthToken and user profile")
    void testGithubCallback_Success() throws Exception {
        RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);
        authService.setRestTemplate(mockRestTemplate);

        // Mock token exchange
        Map<String, Object> tokenResponse = Map.of("access_token", "gho_mocktoken123");
        Mockito.when(mockRestTemplate.postForEntity(
                eq("https://github.com/login/oauth/access_token"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(tokenResponse));

        // Mock GitHub user profile
        Map<String, Object> userProfile = Map.of(
                "login", "octocat-forge",
                "email", "octocat@codeforge.io",
                "name", "Mona Lisa Octocat"
        );
        Mockito.when(mockRestTemplate.exchange(
                eq("https://api.github.com/user"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(userProfile));

        mockMvc.perform(get("/api/auth/github/callback").param("code", "gh_test_code_123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("oauthToken=")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("user=")));

        assertTrue(userRepository.existsByEmail("octocat@codeforge.io"));
    }
}

