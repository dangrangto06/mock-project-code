package com.codeforge.controller;

import com.codeforge.dto.AuthDto;
import com.codeforge.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * REST controller handling user authentication, registration, password recovery,
 * and third-party OAuth2 federations (Google and GitHub).
 *
 * <p>Exposes public identity management endpoints under {@code /api/auth}.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /** Business service handling user verification, password hashing, and token issuance. */
    private final AuthService authService;

    /**
     * Authenticates a user with a username/email and plaintext password.
     *
     * <p>HTTP Endpoint: {@code POST /api/auth/login}</p>
     *
     * @param request Validated login payload containing username/email and password.
     * @return {@link ResponseEntity} with HTTP 200 OK and {@link AuthDto.AuthResponse} (JWT token + profile).
     */
    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        AuthDto.AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Registers a new user account with initial 'Learner' privileges.
     *
     * <p>HTTP Endpoint: {@code POST /api/auth/register}</p>
     *
     * @param request Validated registration payload (full name, username, email, password).
     * @return {@link ResponseEntity} with HTTP 200 OK and {@link AuthDto.AuthResponse} containing issued JWT token.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthDto.AuthResponse> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        AuthDto.AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Initiates password recovery process by generating and emailing a secure reset link.
     *
     * <p>HTTP Endpoint: {@code POST /api/auth/forgot-password}</p>
     *
     * @param request Payload containing destination email.
     * @return {@link ResponseEntity} with HTTP 200 OK and status message.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<AuthDto.MessageResponse> forgotPassword(@Valid @RequestBody AuthDto.ForgotPasswordRequest request) {
        AuthDto.MessageResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Resets a user's password using a verified one-time reset token.
     *
     * <p>HTTP Endpoint: {@code POST /api/auth/reset-password}</p>
     *
     * @param request Payload containing token and candidate new password.
     * @return {@link ResponseEntity} with HTTP 200 OK and status message.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<AuthDto.MessageResponse> resetPassword(@Valid @RequestBody AuthDto.ResetPasswordRequest request) {
        AuthDto.MessageResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Initiates GitHub OAuth authentication flow by redirecting to GitHub or frontend error route.
     *
     * <p>HTTP Endpoint: {@code GET /api/auth/github}</p>
     *
     * @return HTTP 302 Redirect.
     */
    @GetMapping("/github")
    public ResponseEntity<Void> githubLogin() {
        String redirectUrl = authService.getGithubAuthUrl();
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }

    /**
     * Receives OAuth authorization code callback from GitHub, generates token, and redirects to frontend.
     *
     * <p>HTTP Endpoint: {@code GET /api/auth/github/callback}</p>
     *
     * @param code Authorization code returned by GitHub.
     * @return HTTP 302 Redirect to frontend dashboard or login error.
     */
    @GetMapping("/github/callback")
    public ResponseEntity<Void> githubCallback(@RequestParam(name = "code", required = false) String code) {
        String redirectUrl = authService.handleGithubCallback(code);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }

    /**
     * Verifies operational availability of the authentication service.
     *
     * <p>HTTP Endpoint: {@code GET /api/auth/health}</p>
     *
     * @return {@link ResponseEntity} with HTTP 200 OK and status confirmation message.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }
}
