package com.codeforge.controller;

import com.codeforge.dto.AuthDto;
import com.codeforge.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller handling user authentication and registration endpoints.
 *
 * <p>Exposes public identity management endpoints under {@code /api/auth}.
 * Leverages {@link AuthService} to validate credentials, register accounts, and return
 * cryptographically signed JWT tokens for subsequent authenticated API requests.</p>
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
