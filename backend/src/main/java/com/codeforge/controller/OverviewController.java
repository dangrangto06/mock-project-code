package com.codeforge.controller;

import com.codeforge.dto.OverviewResponse;
import com.codeforge.service.OverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/overview")
@RequiredArgsConstructor
public class OverviewController {

    private final OverviewService overviewService;

    /**
     * GET /api/overview
     * Returns dashboard statistics for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<OverviewResponse> getOverview(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        OverviewResponse response = overviewService.getOverview(username);
        return ResponseEntity.ok(response);
    }
}

