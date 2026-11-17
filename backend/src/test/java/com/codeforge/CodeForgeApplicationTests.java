package com.codeforge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test suite for validating Spring Boot application context bootstrapping.
 *
 * <p>Activates the {@code dev} profile (utilizing an in-memory H2 database rather than MS SQL Server)
 * to ensure that all bean definitions, security filter chains, JPA entity configurations,
 * and component scans initialize cleanly without bean dependency circularities or configuration errors.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
class CodeForgeApplicationTests {

    /**
     * Verifies that the Spring application context starts up without errors.
     *
     * <p>Acts as a smoke test for the overall Spring application wiring.</p>
     */
    @Test
    void contextLoads() {
        // Verifies that the Spring application context starts up without errors.
    }
}
