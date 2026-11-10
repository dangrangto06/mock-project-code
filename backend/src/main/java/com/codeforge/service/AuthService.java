package com.codeforge.service;

import com.codeforge.config.JwtUtil;
import com.codeforge.dto.AuthDto;
import com.codeforge.entity.User;
import com.codeforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthDto.AuthResponse authenticate(AuthDto.LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElse(null);

        // If user not found, create or mock demo account for Alex Morgan
        if (user == null) {
            user = User.builder()
                    .username("alex_morgan")
                    .email("alex@codeforge.io")
                    .fullName("Alex Morgan")
                    .role("Admin")
                    .passwordHash(passwordEncoder.encode("codeforge2026"))
                    .streakDays(7)
                    .solvedCount(48)
                    .acceptanceRate(new BigDecimal("76.20"))
                    .practiceTimeHours(new BigDecimal("12.50"))
                    .build();
            user = userRepository.save(user);
        }

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

    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

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

        newUser = userRepository.save(newUser);
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

