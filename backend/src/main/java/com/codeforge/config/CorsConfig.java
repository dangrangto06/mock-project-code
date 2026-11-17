package com.codeforge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;

/**
 * Cross-Origin Resource Sharing (CORS) configuration.
 *
 * <p>Enables and configures CORS filters across all application endpoints to allow the
 * decoupled frontend client (running on separate domains/ports such as {@code localhost:3000},
 * {@code localhost:5500}, or Live Server) to communicate securely with the Spring Boot backend
 * REST API.</p>
 *
 * <p><strong>Key Capabilities Configured:</strong></p>
 * <ul>
 *   <li><b>Credential Support:</b> Allows transmission of cookies and authorization headers.</li>
 *   <li><b>Origin Patterns:</b> Accommodates dynamic origin matching for development and production environments.</li>
 *   <li><b>Allowed Headers & Methods:</b> Whitelists standard HTTP verbs (GET, POST, PUT, DELETE, OPTIONS, PATCH)
 *       and critical request headers including {@code Authorization}.</li>
 * </ul>
 */
@Configuration
public class CorsConfig {

    /**
     * Builds and registers the global {@link CorsFilter} bean.
     *
     * <p>Applies a permissive yet secure CORS policy matching all application URL paths ({@code "/**"}).
     * Pre-flight {@code OPTIONS} requests are intercepted and handled with appropriate CORS response headers.</p>
     *
     * @return Configured {@link CorsFilter} bean used by the Spring filter chain.
     */
    @Bean
    public CorsFilter corsFilter() {
        // Source that resolves CorsConfiguration instances based on request URLs
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Allow cookies and HTTP authentication credentials in cross-origin requests
        config.setAllowCredentials(true);

        // Allow all origin patterns (supports wildcard with allowCredentials=true via origin patterns)
        config.setAllowedOriginPatterns(Collections.singletonList("*"));

        // Whitelist essential HTTP headers needed for JWT auth, content negotiation, and AJAX requests
        config.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization", "X-Requested-With"));

        // Permit all standard RESTful HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Register this configuration for all routes in the application
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
