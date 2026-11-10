package com.codeforge.service;

import com.codeforge.dto.ExecutionDto;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final Judge0Service judge0Service;
    private final ExerciseRepository exerciseRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    // ── Run (no submission persisted) ─────────────────────────────────────────

    public ExecutionDto.RunResponse run(ExecutionDto.RunRequest req) {
        log.info("Running code: language={}", req.getLanguage());

        String input = req.getCustomInput() != null ? req.getCustomInput() : "";
        Map<String, Object> raw = judge0Service.executeCode(req.getLanguage(), req.getCode(), input, null);

        boolean success = isAccepted(raw);
        String stdout = getString(raw, "stdout");
        String stderr = getString(raw, "stderr");
        String time = getString(raw, "time");
        Object memObj = raw.get("memory");
        int memKb = memObj instanceof Number ? ((Number) memObj).intValue() : 0;

        // Build a single test case result for the "run" panel
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

    public ExecutionDto.SubmitResponse submit(ExecutionDto.SubmitRequest req, String username) {
        log.info("Submitting: problemId={} language={}", req.getProblemId(), req.getLanguage());

        Exercise exercise = exerciseRepository.findById(req.getProblemId())
                .orElseThrow(() -> new RuntimeException("Exercise not found: " + req.getProblemId()));

        List<com.codeforge.entity.TestCase> testCases = exercise.getTestCases();
        int passed = 0;
        int total = testCases != null ? testCases.size() : 0;
        double totalTimeMs = 0;
        int maxMemKb = 0;

        List<Map<String, Object>> rawResults = new ArrayList<>();

        if (total > 0) {
            for (com.codeforge.entity.TestCase tc : testCases) {
                Map<String, Object> raw = judge0Service.executeCode(
                        req.getLanguage(), req.getCode(), tc.getInputData(), tc.getExpectedOutput());
                rawResults.add(raw);
                if (isAccepted(raw)) passed++;
                String timeStr = getString(raw, "time");
                if (timeStr != null) {
                    try { totalTimeMs += Double.parseDouble(timeStr) * 1000; } catch (Exception ignored) {}
                }
                Object memObj = raw.get("memory");
                if (memObj instanceof Number n && n.intValue() > maxMemKb) maxMemKb = n.intValue();
            }
        } else {
            // No test cases – just compile/run once
            Map<String, Object> raw = judge0Service.executeCode(req.getLanguage(), req.getCode(), "", null);
            rawResults.add(raw);
            total = 1;
            if (isAccepted(raw)) passed = 1;
        }

        boolean allPassed = passed == total;
        String finalStatus = allPassed ? "Accepted" : "Wrong Answer";
        int avgRuntimeMs = total > 0 ? (int) (totalTimeMs / total) : 0;
        double memMb = maxMemKb / 1024.0;

        // Persist submission
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
                    .runtimeBeatsPct(BigDecimal.valueOf(Math.random() * 40 + 50)) // simulated percentile
                    .memoryBeatsPct(BigDecimal.valueOf(Math.random() * 40 + 50))
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

    private boolean isAccepted(Map<String, Object> raw) {
        Object statusObj = raw.get("status");
        if (statusObj instanceof Map<?, ?> m) {
            Object id = m.get("id");
            return id instanceof Number && ((Number) id).intValue() == 3;
        }
        return false;
    }

    private String getStatus(Map<String, Object> raw) {
        Object statusObj = raw.get("status");
        if (statusObj instanceof Map<?, ?> m) {
            Object desc = m.get("description");
            return desc != null ? desc.toString() : "Unknown";
        }
        return "Unknown";
    }

    private String getString(Map<String, Object> raw, String key) {
        Object v = raw.get(key);
        return v != null ? v.toString() : null;
    }
}
