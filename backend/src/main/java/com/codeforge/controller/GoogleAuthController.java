package com.codeforge.controller;

import com.codeforge.dto.AuthDto;
import com.codeforge.dto.GoogleLoginRequest;
import com.codeforge.service.GoogleAuthService;
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

    /** POST /api/auth/google - sign in or sign up with a Google access token. */
    @PostMapping("/google")
    public ResponseEntity<AuthDto.AuthResponse> google(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(googleAuthService.authenticate(request.getAccessToken()));
    }
}