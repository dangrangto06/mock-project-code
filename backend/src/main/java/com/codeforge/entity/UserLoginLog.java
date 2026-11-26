package com.codeforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing an individual user login event.
 * Stores authentication metadata including client IP, geo-location, user-agent,
 * and login method (e.g. PASSWORD, GOOGLE, GITHUB, REGISTER).
 */
@Entity
@Table(name = "user_login_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(length = 255)
    private String email;

    @Column(name = "login_method", nullable = false, length = 30)
    private String loginMethod;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(length = 150)
    private String location;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "logged_in_at", nullable = false)
    private LocalDateTime loggedInAt;
}
