package com.codeforge.controller;

import com.codeforge.service.TrackingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public endpoint allowing the client application to emit page navigation pings.
 * Tracks user paths whether authenticated or guest.
 */
@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageVisitRequest {
        private String pageRoute;
        private String pageTitle;
    }

    /**
     * POST /api/tracking/visit
     * Records a page view event. Accessible anonymously so guests are also recorded.
     */
    @PostMapping("/visit")
    public ResponseEntity<Map<String, Object>> recordVisit(
            @RequestBody(required = false) PageVisitRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String username = (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal()))
                ? authentication.getName()
                : "Guest";

        String route = (request != null && request.getPageRoute() != null) ? request.getPageRoute() : "#dashboard";
        String title = (request != null) ? request.getPageTitle() : null;

        trackingService.recordPageVisit(username, route, title, httpRequest);

        return ResponseEntity.ok(Map.of("recorded", true));
    }
}
