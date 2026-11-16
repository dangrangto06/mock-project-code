package com.codeforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the CodeForge Core Engine backend application.
 *
 * <p>CodeForge is an enterprise-grade competitive programming and coding practice platform
 * inspired by LeetCode. This Spring Boot application orchestrates code execution (via Judge0 API
 * and sandbox fallbacks), user authentication (stateless JWT security), coding problem management,
 * user progress tracking, and statistical reporting.</p>
 *
 * <p><strong>Architectural Overview:</strong></p>
 * <ul>
 *   <li><b>Presentation Layer:</b> REST controllers exposing APIs under {@code /api/*}.</li>
 *   <li><b>Security Layer:</b> Spring Security 6 with custom JWT filter and stateless session policy.</li>
 *   <li><b>Service Layer:</b> Business logic handling problem queries, auth, telemetry, and Judge0 integration.</li>
 *   <li><b>Persistence Layer:</b> Spring Data JPA repositories backed by Microsoft SQL Server (production) or H2 (dev).</li>
 * </ul>
 *
 * @author CodeForge Engineering Team
 * @version 1.0.0
 */
@SpringBootApplication
public class CodeForgeApplication {

    /**
     * Bootstraps and launches the Spring Boot application context.
     *
     * <p>Performs component scanning across {@code com.codeforge}, initializes Spring beans,
     * configures database connectivity, sets up security filter chains, and executes
     * {@link org.springframework.boot.ApplicationRunner} instances such as {@link com.codeforge.config.DataSeeder}.</p>
     *
     * @param args Command-line arguments passed to the application at launch.
     */
    public static void main(String[] args) {
        SpringApplication.run(CodeForgeApplication.class, args);
    }
}
