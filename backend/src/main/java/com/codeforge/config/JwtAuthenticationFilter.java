package com.codeforge.config;

import com.codeforge.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * HTTP filter that intercepts each incoming request to authenticate users via JWT Bearer tokens.
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee a single execution per request dispatch.
 * Populates Spring Security context with assigned {@link SimpleGrantedAuthority} roles.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Injected JWT utility for cryptographic parsing and token validation.
     */
    private final JwtUtil jwtUtil;

    /**
     * Repository to load user authorization role upon valid token presentation.
     */
    private final UserRepository userRepository;

    /**
     * Intercepts incoming HTTP requests, processes Bearer tokens, and sets Spring Security context.
     *
     * @param request  Incoming HTTP servlet request.
     * @param response Outgoing HTTP servlet response.
     * @param filterChain Downstream filter execution chain.
     * @throws ServletException If a general servlet error occurs.
     * @throws IOException      If an I/O exception occurs during request processing.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extract the raw token string from the Authorization header
            String jwt = parseJwt(request);

            // Verify token existence, signature authenticity, and expiration timestamp
            if (jwt != null && jwtUtil.validateToken(jwt)) {
                String username = jwtUtil.getUsernameFromToken(jwt);

                // Determine user role and construct GrantedAuthorities
                String role = "LEARNER";
                try {
                    com.codeforge.entity.User userEntity = userRepository.findByUsername(username)
                            .or(() -> userRepository.findByEmail(username))
                            .orElse(null);
                    if (userEntity != null && userEntity.getRole() != null && !userEntity.getRole().isBlank()) {
                        role = userEntity.getRole().toUpperCase();
                    }
                } catch (Exception ex) {
                    logger.warn("Could not query user role for token: " + ex.getMessage());
                }

                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                // Construct UserDetails principal with assigned role authority for the SecurityContext
                UserDetails userDetails = new User(username, "", authorities);

                // Build authentication token populated with role authorities
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

                // Attach request-specific details (remote IP, session details if any)
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Establish authenticated identity in the security context for the current thread
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Log error without aborting filter chain; unauthenticated requests fail at authorization gate if needed
            logger.error("Cannot set user authentication: " + e.getMessage());
        }

        // Delegate to the next filter in the pipeline
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the raw JWT token from the HTTP {@code Authorization} header.
     *
     * <p>Checks that the header has text and strictly begins with the {@code "Bearer "} prefix (case-sensitive).</p>
     *
     * @param request Incoming HTTP servlet request.
     * @return 7-character substring containing the raw token, or {@code null} if missing or malformed.
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
