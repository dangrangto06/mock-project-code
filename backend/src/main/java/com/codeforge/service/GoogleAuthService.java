package com.codeforge.service;

import com.codeforge.config.JwtUtil;
import com.codeforge.dto.AuthDto;
import com.codeforge.entity.User;
import com.codeforge.exception.ApiException;
import com.codeforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.PropertySource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@PropertySource("classpath:google.properties")
public class GoogleAuthService {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?access_token={token}";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Set with the environment variable GOOGLE_CLIENT_ID (or property
     * google.client-id).
     */
    @Value("${google.client-id:}")
    private String googleClientId;

    public AuthDto.AuthResponse authenticate(String accessToken) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw ApiException.badRequest("Google sign-in is not configured on the server");
        }

        // 1. The token must be valid AND issued for OUR app (blocks tokens minted for
        // other apps)
        Map<String, Object> tokenInfo = fetch(TOKENINFO_URL, new HttpHeaders(), accessToken);
        Object audience = tokenInfo.getOrDefault("aud", tokenInfo.get("azp"));
        if (!googleClientId.equals(audience)) {
            throw ApiException.unauthorized("Google token was not issued for this application");
        }

        // 2. Read the profile (email, name)
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        Map<String, Object> profile = fetch(USERINFO_URL, headers);

        String email = (String) profile.get("email");
        boolean verified = "true".equals(String.valueOf(profile.get("email_verified")));
        if (email == null || !verified) {
            throw ApiException.unauthorized("Google account does not have a verified email");
        }
        String fullName = profile.get("name") != null ? (String) profile.get("name") : email;

        // 3. Existing user (same email) -> sign in; otherwise create the account
        User user = userRepository.findByEmail(email).orElseGet(() -> createUser(email, fullName));

        return AuthDto.AuthResponse.builder()
                .token(jwtUtil.generateToken(user.getUsername()))
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    private User createUser(String email, String fullName) {
        // username: 3-20 chars, letters/digits/underscore (same rule as the signup
        // form)
        String base = email.substring(0, email.indexOf('@')).replaceAll("[^A-Za-z0-9_]", "_");
        if (base.length() > 14)
            base = base.substring(0, 14);
        if (base.length() < 3)
            base = base + "user";

        String username = base;
        while (userRepository.existsByUsername(username)) {
            username = base + "_" + ThreadLocalRandom.current().nextInt(1000, 10000);
        }

        User newUser = User.builder()
                .username(username)
                .email(email)
                .fullName(fullName)
                // Google users never type a password; store an unguessable random hash
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role("Learner")
                .streakDays(1)
                .solvedCount(0)
                .acceptanceRate(BigDecimal.ZERO)
                .practiceTimeHours(BigDecimal.ZERO)
                .build();
        return userRepository.save(newUser);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetch(String url, HttpHeaders headers, Object... uriVariables) {
        try {
            Map<String, Object> body = restTemplate
                    .exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class, uriVariables)
                    .getBody();
            if (body == null)
                throw ApiException.unauthorized("Invalid Google token");
            return body;
        } catch (RestClientException e) {
            throw ApiException.unauthorized("Invalid Google token");
        }
    }
}