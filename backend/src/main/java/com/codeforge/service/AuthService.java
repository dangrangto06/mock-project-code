package com.codeforge.service;

import com.codeforge.config.JwtUtil;
import com.codeforge.dto.AuthDto;
import com.codeforge.entity.User;
import com.codeforge.exception.ApiException;
import com.codeforge.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service managing user authentication, registration, credential security,
 * password recovery, and third-party OAuth2 federations (Google and GitHub).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    /** Repository for querying and persisting {@link User} records. */
    private final UserRepository userRepository;

    /** Cryptographic encoder for BCrypt password hashing and matching. */
    private final PasswordEncoder passwordEncoder;

    /** Token generator and validator for stateless JSON Web Tokens. */
    private final JwtUtil jwtUtil;

    /** Email delivery service for transactional notifications. */
    private final EmailService emailService;

    /** JSON object mapper for serializing OAuth user state payloads. */
    private final ObjectMapper objectMapper;

    @Value("${github.oauth.client-id:YOUR_GITHUB_CLIENT_ID}")
    private String githubClientId;

    @Value("${github.oauth.client-secret:YOUR_GITHUB_CLIENT_SECRET}")
    private String githubClientSecret;

    @Value("${github.oauth.redirect-uri:http://localhost:8080/api/auth/github/callback}")
    private String githubRedirectUri;

    @Value("${frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    /** RestTemplate for external OAuth provider communication (settable for testing). */
    @Setter
    private RestTemplate restTemplate = new RestTemplate();

    /**
     * Authenticates user credentials and issues a signed JWT token.
     *
     * @param request The login credentials payload.
     * @return {@link AuthDto.AuthResponse} containing the JWT bearer token and user profile details.
     * @throws ApiException (HTTP 401) If username/email or password is invalid.
     */
    public AuthDto.AuthResponse authenticate(AuthDto.LoginRequest request) {
        // Query user by username or email identifier
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid username/email or password"));

        // Verify plaintext password against the stored BCrypt hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid username/email or password");
        }

        // Mint signed JWT access token for this session
        String token = jwtUtil.generateToken(user.getUsername());

        return AuthDto.AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    /**
     * Registers a new user account, persists it to the database, and returns an access token.
     *
     * @param request Inbound registration details.
     * @return {@link AuthDto.AuthResponse} containing the new user profile and authentication token.
     * @throws ApiException (HTTP 409) If the username or email is already taken.
     */
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        // Enforce username uniqueness constraint
        if (userRepository.existsByUsername(request.getUsername())) {
            throw ApiException.conflict("Username is already taken");
        }

        // Enforce email uniqueness constraint
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.conflict("Email is already registered");
        }

        // Build new user record with default starter statistics
        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("Learner")
                .streakDays(1)
                .solvedCount(0)
                .acceptanceRate(BigDecimal.ZERO)
                .practiceTimeHours(BigDecimal.ZERO)
                .build();

        // Save new user entity in the database
        newUser = userRepository.save(newUser);

        // Generate JWT token for immediate access
        String token = jwtUtil.generateToken(newUser.getUsername());

        return AuthDto.AuthResponse.builder()
                .token(token)
                .id(newUser.getId())
                .username(newUser.getUsername())
                .email(newUser.getEmail())
                .fullName(newUser.getFullName())
                .role(newUser.getRole())
                .build();
    }

    /**
     * Handles forgot password requests by generating a 30-minute UUID reset token.
     * Always returns an identical success message to prevent user email enumeration.
     *
     * @param request Inbound payload with target email.
     * @return {@link AuthDto.MessageResponse} confirmation message.
     */
    public AuthDto.MessageResponse forgotPassword(AuthDto.ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), token);
        });

        return AuthDto.MessageResponse.builder()
                .message("If an account exists, a reset link has been sent.")
                .build();
    }

    /**
     * Validates a password reset token and updates the user's password.
     *
     * @param request Inbound payload containing reset token and new password.
     * @return {@link AuthDto.MessageResponse} confirmation message.
     * @throws ApiException (HTTP 400) If token is invalid or expired.
     */
    public AuthDto.MessageResponse resetPassword(AuthDto.ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired password reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("Invalid or expired password reset token");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return AuthDto.MessageResponse.builder()
                .message("Password reset successfully.")
                .build();
    }

    /**
     * Determines GitHub authorization URL or redirects with configuration error.
     *
     * @return Redirect destination URI string.
     */
    public String getGithubAuthUrl() {
        if (githubClientId == null || githubClientId.isBlank() || "YOUR_GITHUB_CLIENT_ID".equalsIgnoreCase(githubClientId)) {
            return frontendBaseUrl + "/login.html?oauthError=github_not_configured";
        }
        return "https://github.com/login/oauth/authorize?client_id=" + githubClientId
                + "&redirect_uri=" + URLEncoder.encode(githubRedirectUri, StandardCharsets.UTF_8)
                + "&scope=user:email";
    }

    /**
     * Exchanges GitHub authorization code for user profile, mints JWT, and prepares redirect URL.
     *
     * @param code Authorization code returned by GitHub OAuth.
     * @return Redirect URI string targeting the frontend login handler.
     */
    public String handleGithubCallback(String code) {
        if (code == null || code.isBlank()) {
            return frontendBaseUrl + "/login.html?oauthError=missing_code";
        }

        try {
            // 1. Exchange code for GitHub access token
            HttpHeaders tokenHeaders = new HttpHeaders();
            tokenHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
            tokenHeaders.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> tokenPayload = Map.of(
                    "client_id", githubClientId,
                    "client_secret", githubClientSecret,
                    "code", code,
                    "redirect_uri", githubRedirectUri
            );

            HttpEntity<Map<String, String>> tokenReq = new HttpEntity<>(tokenPayload, tokenHeaders);
            ResponseEntity<Map> tokenRes = restTemplate.postForEntity(
                    "https://github.com/login/oauth/access_token",
                    tokenReq,
                    Map.class
            );

            Map tokenBody = tokenRes.getBody();
            if (tokenBody == null || tokenBody.get("access_token") == null) {
                log.warn("GitHub token exchange failed: {}", tokenBody);
                return frontendBaseUrl + "/login.html?oauthError=github_auth_failed";
            }
            String accessToken = (String) tokenBody.get("access_token");

            // 2. Fetch user profile with User-Agent header (mandatory for GitHub API)
            HttpHeaders ghHeaders = new HttpHeaders();
            ghHeaders.setBearerAuth(accessToken);
            ghHeaders.set("User-Agent", "CodeForge-Backend");
            HttpEntity<Void> ghReq = new HttpEntity<>(ghHeaders);

            ResponseEntity<Map> userProfileRes = restTemplate.exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET,
                    ghReq,
                    Map.class
            );

            Map userProfile = userProfileRes.getBody();
            if (userProfile == null) {
                return frontendBaseUrl + "/login.html?oauthError=github_profile_failed";
            }

            String email = (String) userProfile.get("email");
            String name = (String) userProfile.get("name");
            String login = (String) userProfile.get("login");
            if (name == null || name.isBlank()) {
                name = login;
            }

            // 3. Fallback to /user/emails if primary email is private
            if (email == null || email.isBlank()) {
                try {
                    ResponseEntity<List> emailsRes = restTemplate.exchange(
                            "https://api.github.com/user/emails",
                            HttpMethod.GET,
                            ghReq,
                            List.class
                    );
                    List<Map<String, Object>> emailsList = (List<Map<String, Object>>) emailsRes.getBody();
                    if (emailsList != null) {
                        for (Map<String, Object> emailEntry : emailsList) {
                            Boolean primary = (Boolean) emailEntry.get("primary");
                            Boolean verified = (Boolean) emailEntry.get("verified");
                            if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                                email = (String) emailEntry.get("email");
                                break;
                            }
                        }
                        if (email == null && !emailsList.isEmpty()) {
                            email = (String) emailsList.get(0).get("email");
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Could not retrieve emails from GitHub: {}", ex.getMessage());
                }
            }

            if (email == null || email.isBlank()) {
                email = login + "@users.noreply.github.com";
            }

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                String baseUsername = (login != null && !login.isBlank())
                        ? login.replaceAll("[^a-zA-Z0-9_]", "")
                        : email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
                if (baseUsername.isBlank()) {
                    baseUsername = "coder";
                }
                String candidateUsername = baseUsername;
                int suffix = 1;
                while (userRepository.existsByUsername(candidateUsername)) {
                    candidateUsername = baseUsername + suffix++;
                }

                user = User.builder()
                        .username(candidateUsername)
                        .email(email)
                        .fullName(name != null ? name : candidateUsername)
                        .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .role("Learner")
                        .streakDays(1)
                        .solvedCount(0)
                        .acceptanceRate(BigDecimal.ZERO)
                        .practiceTimeHours(BigDecimal.ZERO)
                        .build();
                user = userRepository.save(user);
            }

            String jwt = jwtUtil.generateToken(user.getUsername());
            Map<String, Object> userJsonMap = Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "fullName", user.getFullName(),
                    "role", user.getRole()
            );

            String encodedUserJson = URLEncoder.encode(objectMapper.writeValueAsString(userJsonMap), StandardCharsets.UTF_8);
            return frontendBaseUrl + "/login.html?oauthToken=" + jwt + "&user=" + encodedUserJson;
        } catch (Exception e) {
            log.error("GitHub OAuth callback error: {}", e.getMessage(), e);
            return frontendBaseUrl + "/login.html?oauthError=github_auth_failed";
        }
    }
}
