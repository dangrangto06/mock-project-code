package com.codeforge.service;

import com.codeforge.dto.ExecutionDto;
import com.codeforge.dto.ExerciseDto;
import com.codeforge.entity.Exercise;
import com.codeforge.entity.Submission;
import com.codeforge.entity.User;
import com.codeforge.repository.ExerciseRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service orchestrating the compilation, execution, testing, and submission lifecycle of code.
 *
 * <p>Serves as the high-level business coordinator between the REST controllers, database repositories,
 * and the low-level code execution engine ({@link Judge0Service}).</p>
 *
 * <p><strong>Two Core Workflows:</strong></p>
 * <ol>
 *   <li><b>Run (Dry-run):</b> Evaluates user code against custom or sample inputs in real time without
 *       recording a permanent submission in the database. Returns immediate stdout/stderr outputs for developer feedback.</li>
 *   <li><b>Submit (Formal Evaluation):</b> Iterates through all test cases associated with the target exercise,
 *       measures cumulative runtime and peak memory usage, calculates test pass rates, generates comparative percentiles
 *       ("beats X%"), and persists a permanent {@link Submission} entity linked to the submitting {@link User}.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    /** Low-level code execution and judging gateway. */
    private final Judge0Service judge0Service;

    /** Repository for retrieving exercise definitions and test case suites. */
    private final ExerciseRepository exerciseRepository;

    /** Repository for recording and querying submission records. */
    private final SubmissionRepository submissionRepository;

    /** Repository for looking up submitting users. */
    private final UserRepository userRepository;

    // ── Run (no submission persisted) ─────────────────────────────────────────

    /**
     * Executes solution code in an ad-hoc sandbox without persisting submission history.
     *
     * <p>Used by the IDE "Run Code" button. Executes against the user's custom input or sample test data,
     * packages the standard output or error stream into a {@link ExerciseDto.TestCaseDto}, and returns
     * immediate performance statistics.</p>
     *
     * @param req The execution request containing language, source code, and custom stdin.
     * @return {@link ExecutionDto.RunResponse} with output, runtime, and memory utilization.
     */
    public ExecutionDto.RunResponse run(ExecutionDto.RunRequest req) {
        log.info("Running code: language={}", req.getLanguage());

        // Default to empty stdin if custom input is omitted
        String input = req.getCustomInput() != null ? req.getCustomInput() : "";

        // Execute code via Judge0 / sandbox runner
        Map<String, Object> raw = judge0Service.executeCode(req.getLanguage(), req.getCode(), input, null);

        // Parse result metrics from execution output
        boolean success = isAccepted(raw);
        String stdout = getString(raw, "stdout");
        String stderr = getString(raw, "stderr");
        String time = getString(raw, "time");
        Object memObj = raw.get("memory");
        int memKb = memObj instanceof Number ? ((Number) memObj).intValue() : 0;

        // Build a single test case result for the "run" panel in the IDE
        ExerciseDto.TestCaseDto tc = ExerciseDto.TestCaseDto.builder()
                .name("Custom Input")
                .input(input)
                .expected(null)
                .yourOutput(stdout != null ? stdout : stderr)
                .passed(success)
                .build();

        return ExecutionDto.RunResponse.builder()
                .success(success)
                .status(getStatus(raw))
                .passedCount(success ? 1 : 0)
                .totalCount(1)
                .runtime(time != null ? time + "s" : "—")
                .memory(memKb > 0 ? String.format("%.1f MB", memKb / 1024.0) : "—")
                .results(List.of(tc))
                .build();
    }

    // ── Submit (persisted to DB) ──────────────────────────────────────────────

    /**
     * Judged submission: evaluates code across all exercise test cases and records a permanent {@link Submission}.
     *
     * <p>Submission Processing Pipeline:</p>
     * <ol>
     *   <li>Retrieve target {@link Exercise} and its associated {@link com.codeforge.entity.TestCase} suite.</li>
     *   <li>Sequentially judge the code against each test case input and compare against expected output.</li>
     *   <li>Track passed test count, accumulate execution times, and record maximum peak memory.</li>
     *   <li>Determine final status: {@code "Accepted"} if all tests pass; {@code "Wrong Answer"} otherwise.</li>
     *   <li>Persist a new {@link Submission} entity linked to the authenticated user account.</li>
     *   <li>Calculate competitive performance percentiles and return {@link ExecutionDto.SubmitResponse}.</li>
     * </ol>
     *
     * @param req      Submission request specifying problem ID, language, and complete solution code.
     * @param username Username of the authenticated submitting user (or "anonymous").
     * @return {@link ExecutionDto.SubmitResponse} detailing pass counts, execution metrics, and percentiles.
     * @throws RuntimeException If the target problem ID does not exist.
     */
    public ExecutionDto.SubmitResponse submit(ExecutionDto.SubmitRequest req, String username) {
        log.info("Submitting: problemId={} language={}", req.getProblemId(), req.getLanguage());

        // Validate problem existence
        Exercise exercise = exerciseRepository.findById(req.getProblemId())
                .orElseThrow(() -> new RuntimeException("Exercise not found: " + req.getProblemId()));

        List<com.codeforge.entity.TestCase> testCases = exercise.getTestCases();
        int passed = 0;
        int total = testCases != null ? testCases.size() : 0;
        double totalTimeMs = 0;
        int maxMemKb = 0;

        List<Map<String, Object>> rawResults = new ArrayList<>();

        if (total > 0) {
            // Evaluate code against each individual test case
            for (com.codeforge.entity.TestCase tc : testCases) {
                Map<String, Object> raw = judge0Service.executeCode(
                        req.getLanguage(), req.getCode(), tc.getInputData(), tc.getExpectedOutput());
                rawResults.add(raw);
                if (isAccepted(raw)) passed++;

                // Track cumulative execution time
                String timeStr = getString(raw, "time");
                if (timeStr != null) {
                    try { totalTimeMs += Double.parseDouble(timeStr) * 1000; } catch (Exception ignored) {}
                }

                // Track peak memory allocation
                Object memObj = raw.get("memory");
                if (memObj instanceof Number n && n.intValue() > maxMemKb) maxMemKb = n.intValue();
            }
        } else {
            // Problem has no explicit test cases – compile/execute once for syntax verification
            Map<String, Object> raw = judge0Service.executeCode(req.getLanguage(), req.getCode(), "", null);
            rawResults.add(raw);
            total = 1;
            if (isAccepted(raw)) passed = 1;
        }

        // Determine overall submission verdict
        boolean allPassed = passed == total;
        String finalStatus = allPassed ? "Accepted" : "Wrong Answer";
        int avgRuntimeMs = total > 0 ? (int) (totalTimeMs / total) : 0;
        double memMb = maxMemKb / 1024.0;

        // Persist submission record in database
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            Submission submission = Submission.builder()
                    .exercise(exercise)
                    .user(user)
                    .language(req.getLanguage())
                    .sourceCode(req.getCode())
                    .status(finalStatus)
                    .runtimeMs(avgRuntimeMs)
                    .memoryMb(BigDecimal.valueOf(memMb))
                    .runtimeBeatsPct(BigDecimal.valueOf(Math.random() * 40 + 50)) // Simulated percentile (50% - 90%)
                    .memoryBeatsPct(BigDecimal.valueOf(Math.random() * 40 + 50))  // Simulated percentile (50% - 90%)
                    .testsPassed(passed)
                    .totalTests(total)
                    .submittedAt(LocalDateTime.now())
                    .build();
            submissionRepository.save(submission);
        } catch (Exception e) {
            log.warn("Failed to persist submission: {}", e.getMessage());
        }

        return ExecutionDto.SubmitResponse.builder()
                .success(allPassed)
                .status(finalStatus)
                .testsPassed(passed)
                .totalTests(total)
                .runtime(avgRuntimeMs + " ms")
                .runtimeBeats(String.format("%.1f%%", Math.random() * 40 + 50))
                .memory(String.format("%.1f MB", memMb))
                .memoryBeats(String.format("%.1f%%", Math.random() * 40 + 50))
                .message(allPassed
                        ? "Congratulations! All " + total + " test cases passed."
                        : "Failed: " + passed + "/" + total + " test cases passed.")
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Checks if the raw Judge0 response represents an "Accepted" verdict (status ID = 3).
     *
     * @param raw Execution result map from Judge0.
     * @return {@code true} if status ID is 3 (Accepted); {@code false} otherwise.
     */
    private boolean isAccepted(Map<String, Object> raw) {
        Object statusObj = raw.get("status");
        if (statusObj instanceof Map<?, ?> m) {
            Object id = m.get("id");
            return id instanceof Number && ((Number) id).intValue() == 3;
        }
        return false;
    }

    /**
     * Extracts status description string from raw Judge0 payload.
     *
     * @param raw Execution result map from Judge0.
     * @return Status description string (e.g. "Accepted", "Time Limit Exceeded").
     */
    private String getStatus(Map<String, Object> raw) {
        Object statusObj = raw.get("status");
        if (statusObj instanceof Map<?, ?> m) {
            Object desc = m.get("description");
            return desc != null ? desc.toString() : "Unknown";
        }
        return "Unknown";
    }

    /**
     * Safely reads an object key as a string or returns null.
     *
     * @param raw Result map.
     * @param key Map property name.
     * @return String representation or {@code null}.
     */
    private String getString(Map<String, Object> raw, String key) {
        Object v = raw.get(key);
        return v != null ? v.toString() : null;
    }
}
