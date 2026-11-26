package com.codeforge.service;

import com.codeforge.entity.PageVisitLog;
import com.codeforge.entity.User;
import com.codeforge.entity.UserLoginLog;
import com.codeforge.repository.PageVisitLogRepository;
import com.codeforge.repository.UserLoginLogRepository;
import com.codeforge.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service handling client telemetry, IP extraction, geo-location resolution,
 * user login auditing, and page visit logging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {

    private final UserLoginLogRepository userLoginLogRepository;
    private final PageVisitLogRepository pageVisitLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, String> locationCache = new ConcurrentHashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(1500))
            .build();

    /**
     * Extracts client IP address from proxy headers (X-Forwarded-For, X-Real-IP)
     * or standard remote socket address.
     */
    public String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            if (parts.length > 0 && !parts[0].trim().isBlank()) {
                return parts[0].trim();
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr.trim() : "127.0.0.1";
    }

    /**
     * Resolves human-readable geographical location from IP address.
     * Local/private IPs default to "Hanoi, Vietnam (Localhost)".
     * Public IPs are resolved via ip-api.com with cache and a strict 1.5s timeout.
     */
    public String resolveLocation(String ip) {
        if (ip == null || ip.isBlank()) {
            return "Hanoi, Vietnam (Localhost)";
        }

        String trimmed = ip.trim();
        if (isLocalOrPrivateIp(trimmed)) {
            return "Hanoi, Vietnam (Localhost)";
        }

        if (locationCache.containsKey(trimmed)) {
            return locationCache.get(trimmed);
        }

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json/" + trimmed + "?fields=status,city,country"))
                    .timeout(Duration.ofMillis(1500))
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null) {
                JsonNode root = objectMapper.readTree(resp.body());
                if ("success".equalsIgnoreCase(root.path("status").asText())) {
                    String city = root.path("city").asText("");
                    String country = root.path("country").asText("");
                    String loc = (!city.isBlank() && !country.isBlank())
                            ? (city + ", " + country)
                            : (!country.isBlank() ? country : (!city.isBlank() ? city : "Unknown Location"));
                    locationCache.put(trimmed, loc);
                    return loc;
                }
            }
        } catch (Exception e) {
            log.debug("Geo-location lookup failed for IP {}: {}", trimmed, e.getMessage());
        }

        return "Unknown Location";
    }

    private boolean isLocalOrPrivateIp(String ip) {
        return "127.0.0.1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip)
                || "::1".equals(ip)
                || "localhost".equalsIgnoreCase(ip)
                || ip.startsWith("192.168.")
                || ip.startsWith("10.")
                || ip.startsWith("169.254.")
                || ip.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*");
    }

    /**
     * Records a successful user login event.
     */
    public void recordLogin(User user, String method, HttpServletRequest request) {
        try {
            String ip = extractClientIp(request);
            String location = resolveLocation(ip);
            String userAgent = request != null ? request.getHeader("User-Agent") : null;
            if (userAgent != null && userAgent.length() > 500) {
                userAgent = userAgent.substring(0, 500);
            }

            UserLoginLog logEntry = UserLoginLog.builder()
                    .userId(user != null ? user.getId() : null)
                    .username(user != null ? user.getUsername() : "Unknown")
                    .email(user != null ? user.getEmail() : null)
                    .loginMethod(method != null ? method : "PASSWORD")
                    .ipAddress(ip)
                    .location(location)
                    .userAgent(userAgent)
                    .loggedInAt(LocalDateTime.now())
                    .build();

            userLoginLogRepository.save(logEntry);
            log.debug("Recorded login for user={} via method={} from {}", logEntry.getUsername(), method, location);
        } catch (Exception e) {
            log.warn("Failed to record user login log: {}", e.getMessage());
        }
    }

    /**
     * Records a page / site navigation event.
     */
    public void recordPageVisit(String username, String pageRoute, String pageTitle, HttpServletRequest request) {
        try {
            String ip = extractClientIp(request);
            String location = resolveLocation(ip);

            User user = null;
            if (username != null && !username.isBlank() && !"Guest".equalsIgnoreCase(username)) {
                user = userRepository.findByUsername(username).orElse(null);
            }

            String route = (pageRoute != null && !pageRoute.isBlank()) ? pageRoute : "#dashboard";
            if (route.length() > 120) {
                route = route.substring(0, 120);
            }

            String title = pageTitle;
            if (title != null && title.length() > 150) {
                title = title.substring(0, 150);
            }

            PageVisitLog logEntry = PageVisitLog.builder()
                    .userId(user != null ? user.getId() : null)
                    .username((username != null && !username.isBlank()) ? username : "Guest")
                    .pageRoute(route)
                    .pageTitle(title)
                    .ipAddress(ip)
                    .location(location)
                    .visitedAt(LocalDateTime.now())
                    .build();

            pageVisitLogRepository.save(logEntry);
            log.debug("Recorded page visit: user={} route={}", logEntry.getUsername(), route);
        } catch (Exception e) {
            log.warn("Failed to record page visit log: {}", e.getMessage());
        }
    }
}
