package com.codeforge.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 6 infrastructure configuration.
 *
 * <p>Configures web security, password encryption policies, session management, URL authorization rules,
 * and the JWT authentication filter chain pipeline.</p>
 *
 * <p><strong>Key Architecture Choices:</strong></p>
 * <ul>
 *   <li><b>Stateless Architecture:</b> HTTP sessions are disabled ({@link SessionCreationPolicy#STATELESS})
 *       because authentication state is carried entirely within cryptographically signed JWT tokens.</li>
 *   <li><b>CSRF Disabled:</b> Cross-Site Request Forgery protection is disabled as the REST API does not rely on
 *       browser session cookies for authentication.</li>
 *   <li><b>Fine-Grained Route Whitelisting:</b> Public read endpoints (browsing problems, running code in sandbox,
 *       auth endpoints) are permitted anonymously, while state-mutating actions (creating problems, recording submissions)
 *       evaluate user authentication tokens.</li>
 *   <li><b>BCrypt Hashing:</b> Uses adaptive salted BCrypt hashing with work factor 10 for credential security.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Injected custom JWT authentication filter for intercepting requests before standard auth filters.
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Provides a BCrypt password encoder bean for hashing passwords before database persistence
     * and verifying candidate credentials during user login.
     *
     * @return {@link BCryptPasswordEncoder} instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the Spring {@link AuthenticationManager} bean from the central configuration.
     *
     * @param authConfig Spring authentication configuration.
     * @return Configured {@link AuthenticationManager}.
     * @throws Exception If manager retrieval fails.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Constructs and configures the core {@link SecurityFilterChain} for all HTTP requests.
     *
     * <p>Configures security policies in order:</p>
     * <ol>
     *   <li>Disable CSRF (stateless token-based architecture).</li>
     *   <li>Enable CORS integration, delegating to {@link CorsConfig}.</li>
     *   <li>Enforce stateless session management (no HTTP session cookies created or read).</li>
     *   <li>Specify request matchers: allow public access to auth, problem browsing, code execution,
     *       and H2 development console; protect any unlisted endpoints.</li>
     *   <li>Disable X-Frame-Options to allow rendering the H2 web console in an iframe.</li>
     *   <li>Register {@link JwtAuthenticationFilter} prior to {@link UsernamePasswordAuthenticationFilter}.</li>
     * </ol>
     *
     * @param http {@link HttpSecurity} fluent builder.
     * @return Fully built {@link SecurityFilterChain}.
     * @throws Exception If an error occurs during filter chain construction.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable CSRF since tokens are transmitted in the Authorization header, not cookies
            .csrf(AbstractHttpConfigurer::disable)

            // 2. Enable CORS with default settings (resolved by CorsFilter bean)
            .cors(cors -> {})

            // 3. Configure stateless session management (server retains no HTTP session state)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 4. Define route access policies
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",      // Login, Registration, Health check
                    "/api/overview/**",  // Dashboard metrics & telemetry (supports anonymous/guest view)
                    "/api/problems/**",  // Problem library retrieval & detail views
                    "/api/execute/**",   // Code execution runner & test case judge endpoints
                    "/api/health",       // Application health monitoring
                    "/h2-console/**",    // In-memory database web console for local dev
                    "/error"             // Default Spring Boot error mapping
                ).permitAll()
                .anyRequest().authenticated()
            )

            // 5. Allow frame rendering for H2 database development console
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        // 6. Inject the JWT validation filter into the pipeline ahead of the standard username/password filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
