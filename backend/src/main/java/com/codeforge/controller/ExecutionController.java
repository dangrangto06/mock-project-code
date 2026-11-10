package com.codeforge.controller;

import com.codeforge.dto.ExecutionDto;
import com.codeforge.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/execute")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    /**
     * POST /api/execute/run
     * Run code against a custom input (no submission persisted).
     */
    @PostMapping("/run")
    public ResponseEntity<ExecutionDto.RunResponse> run(
            @RequestBody ExecutionDto.RunRequest request) {
        return ResponseEntity.ok(executionService.run(request));
    }

    /**
     * POST /api/execute/submit
     * Submit code against all hidden test cases; persists a Submission record.
     */
    @PostMapping("/submit")
    public ResponseEntity<ExecutionDto.SubmitResponse> submit(
            @RequestBody ExecutionDto.SubmitRequest request,
            Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "anonymous";
        return ResponseEntity.ok(executionService.submit(request, username));
    }

    /**
     * GET /api/execute/health
     * Health check for the execution engine.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "CodeForge Execution Engine"
        ));
    }
}
