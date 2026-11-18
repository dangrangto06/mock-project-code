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
import org.springframework.test.web.servlet.ResultActions;
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

    // ---------------------------------------------------------------------
    // Step 3 - backend validation for username, email and password
    // ---------------------------------------------------------------------

    /** Posts a registration payload to POST /api/auth/register. */
    private ResultActions register(String fullName, String username, String email, String password) throws Exception {
        Map<String, String> body = Map.of(
                "fullName", fullName,
                "username", username,
                "email", email,
                "password", password);
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    @Test
    @DisplayName("Validation: Register - valid payload returns 200 with token and Learner role")
    void testRegister_ValidPayload() throws Exception {
        register("Valid User", "valid_user1", "valid_user1@codeforge.io", "Passw0rd!")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("valid_user1"))
                .andExpect(jsonPath("$.role").value("Learner"));

        assertTrue(userRepository.existsByUsername("valid_user1"));
    }

    @Test
    @DisplayName("Validation: Register - username with illegal characters returns 400")
    void testRegister_UsernameIllegalCharacters() throws Exception {
        for (String bad : new String[] { "bad name", "bad-name", "bad.name", "bad@name", "t\u00EAn_l\u1EA1" }) {
            register("Some User", bad, "illegal_chars@codeforge.io", "Passw0rd!")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("username")));
        }
    }

    @Test
    @DisplayName("Validation: Register - username shorter than 3 or longer than 20 returns 400")
    void testRegister_UsernameLength() throws Exception {
        register("Some User", "ab", "short_name@codeforge.io", "Passw0rd!")
                .andExpect(status().isBadRequest());
        register("Some User", "a".repeat(21), "long_name@codeforge.io", "Passw0rd!")
                .andExpect(status().isBadRequest());

        // Boundaries 3 and 20 are accepted
        register("Some User", "abc", "bound_min@codeforge.io", "Passw0rd!")
                .andExpect(status().isOk());
        register("Some User", "a".repeat(20), "bound_max@codeforge.io", "Passw0rd!")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Validation: Register - malformed or over-long email returns 400")
    void testRegister_InvalidEmail() throws Exception {
        for (String bad : new String[] { "not-an-email", "missing-at.codeforge.io", "a@b", "two@@codeforge.io", "sp ace@codeforge.io" }) {
            register("Some User", "email_tester", bad, "Passw0rd!")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("email")));
        }

        // Structurally valid address (60-char local part, 60-char labels) that is 308 chars
        // long, so only the 255-char @Size limit rejects it.
        String tooLong = "a".repeat(60) + "@" + ("b".repeat(60) + ".").repeat(4) + "com";
        assertTrue(tooLong.length() > 255);
        register("Some User", "email_tester", tooLong, "Passw0rd!")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Validation: Register - password shorter than 8 or longer than 72 characters returns 400")
    void testRegister_PasswordLength() throws Exception {
        register("Some User", "pw_tester", "pw_tester@codeforge.io", "1234567")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("password")));
        register("Some User", "pw_tester", "pw_tester@codeforge.io", "a".repeat(73))
                .andExpect(status().isBadRequest());

        // Boundary 72 is accepted
        register("Some User", "pw_tester", "pw_tester@codeforge.io", "a".repeat(72))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Validation: Register - blank fields and over-long full name return 400")
    void testRegister_BlankAndFullNameTooLong() throws Exception {
        register("", "blank_tester", "blank_tester@codeforge.io", "Passw0rd!")
                .andExpect(status().isBadRequest());
        register("Some User", "", "blank_tester@codeforge.io", "Passw0rd!")
                .andExpect(status().isBadRequest());
        register("Some User", "blank_tester", "", "Passw0rd!")
                .andExpect(status().isBadRequest());
        register("Some User", "blank_tester", "blank_tester@codeforge.io", "")
                .andExpect(status().isBadRequest());
        register("x".repeat(151), "name_tester", "name_tester@codeforge.io", "Passw0rd!")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Validation: Reset Password - new password shorter than 8 or longer than 72 characters returns 400")
    void testResetPassword_PasswordLength() throws Exception {
        for (String bad : new String[] { "short7!", "a".repeat(73) }) {
            AuthDto.ResetPasswordRequest request = new AuthDto.ResetPasswordRequest("any-token", bad);

            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("newPassword")));
        }
    }

    @Test
    @DisplayName("Validation: Forgot Password - malformed email returns 400")
    void testForgotPassword_InvalidEmail() throws Exception {
        AuthDto.ForgotPasswordRequest request = new AuthDto.ForgotPasswordRequest("not-an-email");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
