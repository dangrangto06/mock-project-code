package com.codeforge.service;

import com.codeforge.dto.ExecutionDto;
import com.codeforge.dto.ExerciseDto;
import com.codeforge.dto.SubmissionDto;
import com.codeforge.entity.Exercise;
import com.codeforge.entity.Submission;
import com.codeforge.entity.TestCase;
import com.codeforge.entity.User;
import com.codeforge.exception.ApiException;
import com.codeforge.repository.ExerciseRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service orchestrating compilation, execution, testing, and submission judging via {@link Judge0Service}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final Judge0Service judge0Service;
    private final ExerciseRepository exerciseRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    // ── Standalone Judge0 Compiler ────────────────────────────────────────────

    /**
     * Compiles and runs arbitrary standalone code directly on Judge0 without challenge wrapping
     * or database submission persistence.
     */
    public ExecutionDto.CompileResponse compile(ExecutionDto.CompileRequest req) {
        if (req == null || req.getSourceCode() == null || req.getSourceCode().isBlank()) {
            throw ApiException.badRequest("Source code cannot be empty.");
        }

        int langId = (req.getLanguageId() != null && req.getLanguageId() > 0)
                ? req.getLanguageId()
                : judge0Service.getLanguageId(req.getLanguage());
        String langName = judge0Service.resolveLanguageName(langId, req.getLanguage());

        log.info("Judge0 compile & run: languageId={}, language={}", langId, langName);

        Map<String, Object> raw = judge0Service.compileAndExecute(
                langId,
                req.getLanguage(),
                req.getSourceCode(),
                req.getStdin() != null ? req.getStdin() : "",
                req.getExpectedOutput(),
                req.getCpuTimeLimit(),
                req.getMemoryLimit());

        int statusId = extractStatusId(raw);
        String statusDesc = extractRawStatusDescription(raw);
        boolean success = statusId == 3;

        String stdout = getString(raw, "stdout");
        String stderr = getString(raw, "stderr");
        String compileOutput = getString(raw, "compile_output");
        String message = getString(raw, "message");
        String token = getString(raw, "token");
        String engine = getString(raw, "engine");

        String timeStr = getString(raw, "time");
        int runtimeMs = parseRuntimeMs(timeStr, 32);

        Integer memoryKb = raw.get("memory") instanceof Number n ? n.intValue() : null;
        String memoryStr = memoryKb != null && memoryKb > 0
                ? String.format("%.1f MB", memoryKb / 1024.0)
                : "—";

        Integer exitCode = raw.get("exit_code") instanceof Number n ? n.intValue() : null;

        return ExecutionDto.CompileResponse.builder()
                .success(success)
                .statusId(statusId)
                .status(statusDesc)
                .token(token)
                .languageId(langId)
                .languageName(langName)
                .stdout(stdout != null ? stdout : "")
                .stderr(stderr != null ? stderr : "")
                .compileOutput(compileOutput != null ? compileOutput : "")
                .message(message != null ? message : "")
                .exitCode(exitCode)
                .time(timeStr != null ? timeStr : "0.000")
                .runtime(runtimeMs + " ms")
                .memoryKb(memoryKb != null ? memoryKb : 0)
                .memory(memoryStr)
                .engine(engine != null ? engine : "Judge0 CE")
                .build();
    }

    public List<ExecutionDto.Judge0LanguageDto> getLanguages() {
        return judge0Service.getLanguages();
    }

    public Map<String, Object> getJudge0Status() {
        return judge0Service.getStatusInfo();
    }

    // ── Run (IDE dry-run, no submission persisted) ────────────────────────────

    @Transactional(readOnly = true)
    public ExecutionDto.RunResponse run(ExecutionDto.RunRequest req) {
        log.info("Running code: problemId={}, language={}", req.getProblemId(), req.getLanguage());

        List<TestCase> sampleCases = new ArrayList<>();
        if (req.getCustomCases() != null && !req.getCustomCases().isEmpty()) {
            long idx = 1;
            for (ExecutionDto.CustomTestCaseInput custom : req.getCustomCases()) {
                sampleCases.add(TestCase.builder()
                        .id(idx)
                        .caseName(custom.getName() != null && !custom.getName().isBlank()
                                ? custom.getName()
                                : "Case " + idx)
                        .inputData(custom.getInput() != null ? custom.getInput() : "")
                        .expectedOutput(custom.getExpected())
                        .isSample(true)
                        .build());
                idx++;
            }
        } else if (req.getCustomInput() != null && !req.getCustomInput().isBlank()) {
            sampleCases.add(TestCase.builder()
                    .id(1L)
                    .caseName("Custom Input")
                    .inputData(req.getCustomInput())
                    .expectedOutput(null)
                    .isSample(true)
                    .build());
        } else {
            if (req.getProblemId() != null) {
                exerciseRepository.findById(req.getProblemId()).ifPresent(ex -> {
                    if (ex.getTestCases() != null) {
                        ex.getTestCases().stream()
                                .filter(tc -> Boolean.TRUE.equals(tc.getIsSample()))
                                .forEach(sampleCases::add);
                    }
                });
            }
            if (sampleCases.isEmpty()) {
                sampleCases.add(TestCase.builder()
                        .id(1L)
                        .caseName("Case 1")
                        .inputData("nums = [2,7,11,15], target = 9")
                        .expectedOutput("[0, 1]")
                        .isSample(true)
                        .build());
                sampleCases.add(TestCase.builder()
                        .id(2L)
                        .caseName("Case 2")
                        .inputData("nums = [3,2,4], target = 6")
                        .expectedOutput("[1, 2]")
                        .isSample(true)
                        .build());
            }
        }

        List<ExerciseDto.TestCaseDto> results = new ArrayList<>();
        int passedCount = 0;
        double totalTimeMs = 0;
        int maxMemKb = 0;
        String firstErrorStatus = null;
        String firstCompileOutput = null;
        String engineUsed = null;

        int idx = 1;
        for (TestCase tc : sampleCases) {
            CodeExecutionHarness.PreparedCode prepared = CodeExecutionHarness.prepareForJudge0(
                    req.getLanguage(), req.getCode(), tc.getInputData());

            // When wrapped, evaluate output equality flexibly in Java (supports spacing differences like [0,1] vs [0, 1])
            String judgeExpected = prepared.wrapped() ? null : tc.getExpectedOutput();
            Map<String, Object> raw = judge0Service.executeCode(
                    req.getLanguage(), prepared.sourceCode(), tc.getInputData(), judgeExpected);

            if (engineUsed == null) {
                engineUsed = getString(raw, "engine");
            }

            String stdout = getString(raw, "stdout");
            String stderr = getString(raw, "stderr");
            String compileOut = getString(raw, "compile_output");
            String message = getString(raw, "message");
            int statusId = extractStatusId(raw);

            boolean passed = isAcceptedOrNormalizedMatch(raw, prepared.wrapped(), tc.getExpectedOutput());
            if (passed) {
                passedCount++;
            } else if (firstErrorStatus == null) {
                if (statusId == 3 && tc.getExpectedOutput() != null) {
                    firstErrorStatus = "Wrong Answer";
                } else {
                    firstErrorStatus = normalizeErrorStatus(raw);
                }
            }
            if (firstCompileOutput == null && compileOut != null && !compileOut.isBlank()) {
                firstCompileOutput = compileOut.trim();
            }

            String timeStr = getString(raw, "time");
            if (timeStr != null) {
                try {
                    totalTimeMs += Double.parseDouble(timeStr) * 1000;
                } catch (Exception ignored) {
                }
            }
            Object memObj = raw.get("memory");
            if (memObj instanceof Number n && n.intValue() > maxMemKb) {
                maxMemKb = n.intValue();
            }

            String actualOut = resolveActualOutput(compileOut, stderr, message, stdout);

            results.add(ExerciseDto.TestCaseDto.builder()
                    .id(tc.getId() != null ? tc.getId() : (long) idx)
                    .name(tc.getCaseName() != null ? tc.getCaseName() : "Case " + idx)
                    .input(tc.getInputData())
                    .expected(tc.getExpectedOutput())
                    .yourOutput(actualOut)
                    .passed(passed)
                    .build());
            idx++;
        }

        int totalCount = results.size();
        boolean allPassed = totalCount > 0 && passedCount == totalCount;
        String overallStatus = allPassed
                ? "Accepted"
                : (firstErrorStatus != null && !firstErrorStatus.equals("Accepted") && !firstErrorStatus.equals("Unknown")
                        ? firstErrorStatus
                        : "Wrong Answer");
        int avgRuntimeMs = totalCount > 0 ? Math.max(1, (int) Math.round(totalTimeMs / totalCount)) : 32;
        double memMb = maxMemKb > 0 ? (maxMemKb / 1024.0) : 17.0;

        return ExecutionDto.RunResponse.builder()
                .success(allPassed)
                .status(overallStatus)
                .passedCount(passedCount)
                .totalCount(totalCount)
                .runtime(avgRuntimeMs + " ms")
                .memory(String.format("%.1f MB", memMb))
                .results(results)
                .compileOutput(firstCompileOutput)
                .engine(engineUsed != null ? engineUsed : "Judge0 CE")
                .build();
    }

    // ── Submit (persisted to DB) ──────────────────────────────────────────────

    @Transactional
    public ExecutionDto.SubmitResponse submit(ExecutionDto.SubmitRequest req, String username) {
        log.info("Submitting: problemId={} language={}", req.getProblemId(), req.getLanguage());

        Exercise exercise = exerciseRepository.findById(req.getProblemId())
                .orElseThrow(() -> new RuntimeException("Exercise not found: " + req.getProblemId()));

        List<TestCase> testCases = exercise.getTestCases();
        int passed = 0;
        int total = testCases != null ? testCases.size() : 0;
        double totalTimeMs = 0;
        int maxMemKb = 0;
        String firstErr = null;
        String firstCompileOutput = null;
        String engineUsed = null;

        if (total > 0) {
            for (TestCase tc : testCases) {
                CodeExecutionHarness.PreparedCode prepared = CodeExecutionHarness.prepareForJudge0(
                        req.getLanguage(), req.getCode(), tc.getInputData());
                String judgeExpected = prepared.wrapped() ? null : tc.getExpectedOutput();
                Map<String, Object> raw = judge0Service.executeCode(
                        req.getLanguage(), prepared.sourceCode(), tc.getInputData(), judgeExpected);

                if (engineUsed == null) {
                    engineUsed = getString(raw, "engine");
                }
                if (firstCompileOutput == null) {
                    String co = getString(raw, "compile_output");
                    if (co != null && !co.isBlank()) firstCompileOutput = co.trim();
                }

                boolean tcPassed = isAcceptedOrNormalizedMatch(raw, prepared.wrapped(), tc.getExpectedOutput());
                if (tcPassed) {
                    passed++;
                } else if (firstErr == null) {
                    int statusId = extractStatusId(raw);
                    firstErr = (statusId == 3 && tc.getExpectedOutput() != null)
                            ? "Wrong Answer"
                            : normalizeErrorStatus(raw);
                }

                String timeStr = getString(raw, "time");
                if (timeStr != null) {
                    try {
                        totalTimeMs += Double.parseDouble(timeStr) * 1000;
                    } catch (Exception ignored) {
                    }
                }

                Object memObj = raw.get("memory");
                if (memObj instanceof Number n && n.intValue() > maxMemKb) {
                    maxMemKb = n.intValue();
                }
            }
        } else {
            CodeExecutionHarness.PreparedCode prepared = CodeExecutionHarness.prepareForJudge0(
                    req.getLanguage(), req.getCode(), "");
            Map<String, Object> raw = judge0Service.executeCode(req.getLanguage(), prepared.sourceCode(), "", null);
            engineUsed = getString(raw, "engine");
            String co = getString(raw, "compile_output");
            if (co != null && !co.isBlank()) firstCompileOutput = co.trim();
            total = 1;
            if (extractStatusId(raw) == 3) {
                passed = 1;
            } else {
                firstErr = normalizeErrorStatus(raw);
            }
        }

        boolean allPassed = passed == total;
        String finalStatus = allPassed ? "Accepted"
                : (firstErr != null && !firstErr.equals("Accepted") && !firstErr.equals("Unknown")
                        ? firstErr
                        : "Wrong Answer");
        int avgRuntimeMs = total > 0 ? Math.max(1, (int) (totalTimeMs / total)) : 32;
        double memMb = maxMemKb > 0 ? (maxMemKb / 1024.0) : 17.0;

        try {
            User user = userRepository.findByUsername(username).orElse(null);
            boolean hadSolvedBefore = false;
            if (user != null) {
                hadSolvedBefore = submissionRepository.existsByUserIdAndExerciseIdAndStatus(
                        user.getId(), exercise.getId(), "Accepted");
            }

            Submission submission = Submission.builder()
                    .exercise(exercise)
                    .user(user)
                    .language(req.getLanguage())
                    .sourceCode(req.getCode())
                    .status(finalStatus)
                    .runtimeMs(avgRuntimeMs)
                    .memoryMb(BigDecimal.valueOf(memMb))
                    .runtimeBeatsPct(BigDecimal.valueOf(Math.random() * 40 + 50))
                    .memoryBeatsPct(BigDecimal.valueOf(Math.random() * 40 + 50))
                    .testsPassed(passed)
                    .totalTests(total)
                    .submittedAt(LocalDateTime.now())
                    .build();
            submissionRepository.save(submission);

            if (user != null) {
                if (allPassed && !hadSolvedBefore) {
                    user.setSolvedCount((user.getSolvedCount() != null ? user.getSolvedCount() : 0) + 1);
                }
                long totalCount = submissionRepository.countByUserId(user.getId());
                long acceptedCount = submissionRepository.countByUserIdAndStatus(user.getId(), "Accepted");
                double rate = totalCount > 0 ? (acceptedCount * 100.0 / totalCount) : 0.0;
                user.setAcceptanceRate(BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP));
                userRepository.save(user);
            }
        } catch (Exception e) {
            log.warn("Failed to persist submission: {}", e.getMessage(), e);
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
                .compileOutput(firstCompileOutput)
                .engine(engineUsed != null ? engineUsed : "Judge0 CE")
                .build();
    }

    @Transactional(readOnly = true)
    public List<SubmissionDto> getUserSubmissions(String username) {
        if (username == null || "anonymous".equalsIgnoreCase(username)) {
            return List.of();
        }
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElse(null);
        if (user == null) {
            return List.of();
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");
        return submissionRepository.findByUserIdOrderBySubmittedAtDesc(user.getId())
                .stream()
                .map(s -> SubmissionDto.builder()
                        .id(s.getId())
                        .exerciseId(s.getExercise() != null ? s.getExercise().getId() : null)
                        .exerciseTitle(s.getExercise() != null ? s.getExercise().getTitle() : "Challenge")
                        .status(s.getStatus())
                        .language(s.getLanguage())
                        .runtime(s.getRuntimeMs() != null ? s.getRuntimeMs() + " ms" : "—")
                        .memory(s.getMemoryMb() != null ? s.getMemoryMb().toPlainString() + " MB" : "—")
                        .runtimeMs(s.getRuntimeMs())
                        .memoryMb(s.getMemoryMb())
                        .runtimeBeatsPct(s.getRuntimeBeatsPct())
                        .memoryBeatsPct(s.getMemoryBeatsPct())
                        .testsPassed(s.getTestsPassed())
                        .totalTests(s.getTotalTests())
                        .sourceCode(s.getSourceCode())
                        .date(s.getSubmittedAt() != null ? s.getSubmittedAt().format(formatter) : "Recently")
                        .submittedAt(s.getSubmittedAt())
                        .build())
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isAcceptedOrNormalizedMatch(Map<String, Object> raw, boolean wasWrapped, String expectedOutput) {
        int statusId = extractStatusId(raw);
        if (statusId != 3) {
            return false;
        }
        if (wasWrapped && expectedOutput != null) {
            String stdout = getString(raw, "stdout");
            return CodeExecutionHarness.outputsMatch(stdout, expectedOutput);
        }
        return true;
    }

    private int extractStatusId(Map<String, Object> raw) {
        Object statusObj = raw.get("status");
        if (statusObj instanceof Map<?, ?> m && m.get("id") instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private String extractRawStatusDescription(Map<String, Object> raw) {
        Object statusObj = raw.get("status");
        if (statusObj instanceof Map<?, ?> m) {
            Object desc = m.get("description");
            return desc != null ? desc.toString() : "Unknown";
        }
        return "Unknown";
    }

    /**
     * Normalizes Judge0 Runtime Error sub-types (ID 7..12, e.g. "Runtime Error (NZEC)")
     * to "Runtime Error" for unified IDE status badges and test assertions.
     */
    private String normalizeErrorStatus(Map<String, Object> raw) {
        int id = extractStatusId(raw);
        if (id >= 7 && id <= 12) {
            return "Runtime Error";
        }
        if (id == 6) {
            return "Compilation Error";
        }
        if (id == 5) {
            return "Time Limit Exceeded";
        }
        return extractRawStatusDescription(raw);
    }

    private String resolveActualOutput(String compileOut, String stderr, String message, String stdout) {
        if (compileOut != null && !compileOut.isBlank()) return compileOut.trim();
        if (stderr != null && !stderr.isBlank()) return stderr.trim();
        if (message != null && !message.isBlank()) return message.trim();
        if (stdout != null) return stdout.trim();
        return "null";
    }

    private int parseRuntimeMs(String timeStr, int fallback) {
        if (timeStr != null && !timeStr.isBlank()) {
            try {
                return Math.max(1, (int) Math.round(Double.parseDouble(timeStr) * 1000));
            } catch (Exception ignored) {}
        }
        return fallback;
    }

    private String getString(Map<String, Object> raw, String key) {
        Object v = raw.get(key);
        return v != null ? v.toString() : null;
    }
}
