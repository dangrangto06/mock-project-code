package com.codeforge.controller;

import com.codeforge.dto.ExecutionDto;
import com.codeforge.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller managing remote code execution and solution judging.
 *
 * <p>Exposes endpoints under {@code /api/execute} for sandbox dry-runs (evaluating custom test inputs)
 * and formal solution submissions (evaluating against all test cases and tracking database records).</p>
 */
@RestController
@RequestMapping("/api/execute")
@RequiredArgsConstructor
public class ExecutionController {

    /** Service coordinating code compilation, testing, and submission logging. */
    private final ExecutionService executionService;

    /**
     * Executes arbitrary solution code against custom standard input in an isolated sandbox.
     *
     * <p>HTTP Endpoint: {@code POST /api/execute/run}</p>
     * <p>Does not record a submission in the database; designed for interactive IDE testing.</p>
     *
     * @param request The execution request containing language, code, and optional stdin.
     * @return {@link ResponseEntity} with HTTP 200 OK and {@link ExecutionDto.RunResponse} execution results.
     */
    @PostMapping("/run")
    public ResponseEntity<ExecutionDto.RunResponse> run(
            @RequestBody ExecutionDto.RunRequest request) {
        return ResponseEntity.ok(executionService.run(request));
    }

    /**
     * Submits solution code for full judging against all exercise test cases and records a submission.
     *
     * <p>HTTP Endpoint: {@code POST /api/execute/submit}</p>
     * <p>Extracts the user identity from Spring Security's {@link Authentication} object.
     * Persists a {@link com.codeforge.entity.Submission} entity and updates user progress.</p>
     *
     * @param request        The submission request specifying problem ID, language, and source code.
     * @param authentication Security context representing the authenticated user (or null if anonymous).
     * @return {@link ResponseEntity} with HTTP 200 OK and {@link ExecutionDto.SubmitResponse} evaluation verdict.
     */
    @PostMapping("/submit")
    public ResponseEntity<ExecutionDto.SubmitResponse> submit(
            @RequestBody ExecutionDto.SubmitRequest request,
            Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "anonymous";
        return ResponseEntity.ok(executionService.submit(request, username));
    }

    /**
     * Diagnostic health check for the code execution engine.
     *
     * <p>HTTP Endpoint: {@code GET /api/execute/health}</p>
     *
     * @return {@link ResponseEntity} with HTTP 200 OK and execution service operational status.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "CodeForge Execution Engine"
        ));
    }
}
