package com.codeforge.controller;

import com.codeforge.dto.OverviewResponse;
import com.codeforge.service.OverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller serving dashboard telemetry and aggregated practice statistics.
 *
 * <p>Exposes the {@code /api/overview} endpoint consumed by the CodeForge dashboard
 * to populate greeting headers, weekly practice goals, learning track roadmaps,
 * and recent submission history.</p>
 */
@RestController
@RequestMapping("/api/overview")
@RequiredArgsConstructor
public class OverviewController {

    /** Service aggregating user metrics, next challenge recommendation, and recent submissions. */
    private final OverviewService overviewService;

    /**
     * Retrieves aggregated overview metrics for the calling user or anonymous guest.
     *
     * <p>HTTP Endpoint: {@code GET /api/overview}</p>
     *
     * @param authentication Security context containing caller credentials (if authenticated).
     * @return {@link ResponseEntity} with HTTP 200 OK and populated {@link OverviewResponse}.
     */
    @GetMapping
    public ResponseEntity<OverviewResponse> getOverview(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        OverviewResponse response = overviewService.getOverview(username);
        return ResponseEntity.ok(response);
    }
}
