package com.codeforge.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility component for generating, parsing, and validating JSON Web Tokens (JWT).
 *
 * <p>Employs the JJWT 0.12+ library using HMAC-SHA256 symmetric cryptographic signatures.
 * In CodeForge's stateless authentication model, these tokens are issued upon successful user
 * login or registration and subsequently verified on incoming HTTP requests via
 * {@link JwtAuthenticationFilter}.</p>
 *
 * <p><strong>Key Responsibilities:</strong></p>
 * <ul>
 *   <li>Convert configured signing secret into a cryptographically sound {@link SecretKey}.</li>
 *   <li>Generate compact JWT tokens embedding the username as the subject claim and expiration timestamp.</li>
 *   <li>Extract the subject (username) from signed and verified JWT claims.</li>
 *   <li>Validate token integrity, signature authenticity, and non-expiration.</li>
 * </ul>
 */
@Component
public class JwtUtil {

    /**
     * Secret key string utilized for HMAC-SHA256 signing and verification.
     * Configurable via {@code jwt.secret} in {@code application.properties}.
     */
    @Value("${jwt.secret:CodeForgeSuperSecretSigningKeyThatIsLongEnoughForHMACSHA256Security123456}")
    private String jwtSecret;

    /**
     * Token validity lifetime in milliseconds (defaults to 24 hours / 86,400,000 ms).
     * Configurable via {@code jwt.expirationMs} in {@code application.properties}.
     */
    @Value("${jwt.expirationMs:86400000}")
    private int jwtExpirationMs;

    /**
     * Derives a cryptographic {@link SecretKey} from the configured raw secret bytes.
     *
     * @return Cryptographic HMAC-SHA key compliant with standard JJWT signing specs.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed, stateless JWT token for an authenticated user.
     *
     * <p>Token Payload Structure:</p>
     * <ul>
     *   <li>{@code sub} (Subject): The user's unique username.</li>
     *   <li>{@code iat} (Issued At): Current system timestamp.</li>
     *   <li>{@code exp} (Expiration): Current timestamp + configured validity period.</li>
     * </ul>
     *
     * @param username The username of the authenticated subject.
     * @return Compact, URL-safe JWT token string.
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Parses and decrypts a signed JWT token to retrieve the subject (username).
     *
     * <p>Enforces cryptographic signature verification before reading any claims payload.
     * If the token was altered or expired, a {@link JwtException} is thrown during parsing.</p>
     *
     * @param token Compact JWT token string from the HTTP Authorization header.
     * @return The username embedded in the token's subject claim.
     * @throws JwtException If the token is expired, corrupted, or cryptographically invalid.
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Validates whether a provided token has a valid signature and has not expired.
     *
     * <p>Catches all JJWT exceptions (e.g., {@link ExpiredJwtException}, {@link MalformedJwtException},
     * {@link SecurityException}) and returns {@code false} cleanly without bubbling exceptions
     * to the filter chain.</p>
     *
     * @param token Compact JWT token string to evaluate.
     * @return {@code true} if the token is valid, signed, and unexpired; {@code false} otherwise.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token is malformed, expired, unsupported, or has an invalid signature
            return false;
        }
    }
}
