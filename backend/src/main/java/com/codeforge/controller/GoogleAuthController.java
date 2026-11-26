package com.codeforge.controller;

import com.codeforge.dto.AuthDto;
import com.codeforge.dto.GoogleLoginRequest;
import com.codeforge.entity.User;
import com.codeforge.repository.UserRepository;
import com.codeforge.service.GoogleAuthService;
import com.codeforge.service.TrackingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;
    private final UserRepository userRepository;
    private final TrackingService trackingService;

    /** POST /api/auth/google - sign in or sign up with a Google access token. */
    @PostMapping("/google")
    public ResponseEntity<AuthDto.AuthResponse> google(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest httpRequest) {
        AuthDto.AuthResponse response = googleAuthService.authenticate(request.getAccessToken());
        User user = userRepository.findByEmail(response.getEmail()).orElse(null);
        trackingService.recordLogin(user, "GOOGLE", httpRequest);
        return ResponseEntity.ok(response);
    }
}
