package com.codeforge.service;

import com.codeforge.config.JwtUtil;
import com.codeforge.dto.AuthDto;
import com.codeforge.entity.User;
import com.codeforge.exception.ApiException;
import com.codeforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service managing user authentication, registration, and credential security.
 *
 * <p>Implements core identity workflows: verifying BCrypt-hashed credentials during login,
 * enforcing unique identity constraints during registration, and issuing cryptographically signed
 * stateless JWT access tokens via {@link JwtUtil}.</p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Repository for querying and persisting {@link User} records. */
    private final UserRepository userRepository;

    /** Cryptographic encoder for BCrypt password hashing and matching. */
    private final PasswordEncoder passwordEncoder;

    /** Token generator and validator for stateless JSON Web Tokens. */
    private final JwtUtil jwtUtil;

    /**
     * Authenticates user credentials and issues a signed JWT token.
     *
     * <p>Authentication Workflow:</p>
     * <ol>
     *   <li>Locate user record matching either the provided username handle or email address.</li>
     *   <li>Verify the plaintext password candidate against the stored salted BCrypt hash.</li>
     *   <li>If matching fails or the user does not exist, throw {@link ApiException#unauthorized(String)}.</li>
     *   <li>Generate a new signed JWT token embedding the user's username.</li>
     *   <li>Assemble and return {@link AuthDto.AuthResponse} with profile information.</li>
     * </ol>
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
     * <p>Registration Workflow:</p>
     * <ol>
     *   <li>Check for existing username to prevent handle collisions (throws HTTP 409).</li>
     *   <li>Check for existing email address to prevent duplicate accounts (throws HTTP 409).</li>
     *   <li>Hash password using BCrypt with automatic salting.</li>
     *   <li>Persist user record initialized with 'Learner' role and base statistics.</li>
     *   <li>Generate and return a signed JWT access token immediately so the user is signed in.</li>
     * </ol>
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
}
