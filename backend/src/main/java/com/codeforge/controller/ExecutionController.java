package com.codeforge.controller;

import com.codeforge.dto.ExecutionDto;
import com.codeforge.dto.SubmissionDto;
import com.codeforge.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller managing remote Judge0 compilation, code execution, language catalog, and solution judging.
 */
@RestController
@RequestMapping("/api/execute")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    /**
     * Compiles and executes standalone source code directly using the Judge0 API.
     *
     * <p>HTTP Endpoint: {@code POST /api/execute/compile}</p>
     */
    @PostMapping("/compile")
    public ResponseEntity<ExecutionDto.CompileResponse> compile(
            @RequestBody ExecutionDto.CompileRequest request) {
        return ResponseEntity.ok(executionService.compile(request));
    }

    /**
     * Retrieves all active programming languages supported by the configured Judge0 engine.
     *
     * <p>HTTP Endpoint: {@code GET /api/execute/languages}</p>
     */
    @GetMapping("/languages")
    public ResponseEntity<List<ExecutionDto.Judge0LanguageDto>> getLanguages() {
        return ResponseEntity.ok(executionService.getLanguages());
    }

    /**
     * Retrieves Judge0 API connectivity and version diagnostics.
     *
     * <p>HTTP Endpoint: {@code GET /api/execute/judge0/status}</p>
     */
    @GetMapping("/judge0/status")
    public ResponseEntity<Map<String, Object>> getJudge0Status() {
        return ResponseEntity.ok(executionService.getJudge0Status());
    }

    /**
     * Executes solution code against sample or custom test cases in the IDE sandbox.
     *
     * <p>HTTP Endpoint: {@code POST /api/execute/run}</p>
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
     */
    @PostMapping("/submit")
    public ResponseEntity<ExecutionDto.SubmitResponse> submit(
            @RequestBody ExecutionDto.SubmitRequest request,
            Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "anonymous";
        return ResponseEntity.ok(executionService.submit(request, username));
    }

    /**
     * Retrieves all submissions for the currently authenticated user across all exercises.
     *
     * <p>HTTP Endpoint: {@code GET /api/execute/submissions}</p>
     */
    @GetMapping("/submissions")
    public ResponseEntity<List<SubmissionDto>> getUserSubmissions(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(executionService.getUserSubmissions(username));
    }

    /**
     * Diagnostic health check for the code execution engine.
     *
     * <p>HTTP Endpoint: {@code GET /api/execute/health}</p>
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "CodeForge Execution Engine"
        ));
    }
}
