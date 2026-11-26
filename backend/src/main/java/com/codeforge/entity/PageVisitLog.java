package com.codeforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing an in-app page / route visit event.
 * Tracks user navigation journeys across problem library, editor, notes, community, etc.
 */
@Entity
@Table(name = "page_visit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageVisitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "page_route", nullable = false, length = 120)
    private String pageRoute;

    @Column(name = "page_title", length = 150)
    private String pageTitle;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(length = 150)
    private String location;

    @Column(name = "visited_at", nullable = false)
    private LocalDateTime visitedAt;
}
