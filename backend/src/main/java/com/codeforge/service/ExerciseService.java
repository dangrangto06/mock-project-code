package com.codeforge.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.codeforge.dto.ExerciseDto;
import com.codeforge.dto.SubmissionDto;
import com.codeforge.entity.Exercise;
import com.codeforge.entity.SavedExercise;
import com.codeforge.entity.Submission;
import com.codeforge.entity.User;
import com.codeforge.exception.ApiException;
import com.codeforge.repository.ExerciseRepository;
import com.codeforge.repository.SavedExerciseRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing algorithmic problems, search queries, problem detail
 * lookups, and creation.
 *
 * <p>
 * Coordinates data retrieval across {@link ExerciseRepository},
 * {@link SubmissionRepository},
 * and {@link UserRepository}. Enriches problem data with contextual user status
 * (e.g., whether
 * the authenticated user has already solved or attempted a challenge).
 * </p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseService {

    /** Repository for accessing and searching {@link Exercise} entities. */
    private final ExerciseRepository exerciseRepository;

    /** Repository for looking up user identities. */
    private final UserRepository userRepository;

    /** Repository for checking submission history per exercise. */
    private final SubmissionRepository submissionRepository;

    /** Repository for managing user exercise bookmarks. */
    private final SavedExerciseRepository savedExerciseRepository;

    // ── Query ─────────────────────────────────────────────────────────────────

    /**
     * Retrieves a filtered list of exercises enriched with caller-specific progress
     * metadata.
     *
     * <p>
     * Applies multi-criteria search filtering (by search keyword, difficulty, and
     * topic)
     * and maps each entity into an {@link ExerciseDto} reflecting the user's
     * solved/in-progress status.
     * </p>
     *
     * @param search     Optional text keyword matching title or problem ID.
     * @param difficulty Optional difficulty tier ("Easy", "Medium", "Hard") or
     *                   "all".
     * @param topic      Optional category topic (e.g. "Arrays", "Hash map") or
     *                   "all".
     * @param tab        Optional tab filter ("all", "saved", "my").
     * @param username   Username of the authenticated user, or {@code null} if
     *                   anonymous.
     * @return List of matching {@link ExerciseDto} objects.
     */
    public List<ExerciseDto> getAll(String search, String difficulty, String topic,
            String tab, String username) {
        // Query exercises using dynamic search criteria
        List<Exercise> exercises = exerciseRepository.searchExercises(
                (search != null && !search.isBlank()) ? search.trim() : null,
                (difficulty != null && !difficulty.isBlank() && !"all".equalsIgnoreCase(difficulty)) ? difficulty
                        : null,
                (topic != null && !topic.isBlank() && !"all".equalsIgnoreCase(topic)) ? topic : null);

        // Fetch user entity if authenticated to evaluate problem progress status
        User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;

        if ("my".equalsIgnoreCase(tab)) {
            if (user == null) {
                return List.of();
            }
            exercises = exercises.stream()
                    .filter(e -> e.getAuthor() != null && e.getAuthor().getId().equals(user.getId()))
                    .collect(Collectors.toList());
        } else if ("saved".equalsIgnoreCase(tab)) {
            if (user == null) {
                return List.of();
            }
            exercises = exercises.stream()
                    .filter(this::isApproved)
                    .filter(e -> savedExerciseRepository.existsByUserIdAndExerciseId(user.getId(), e.getId()))
                    .collect(Collectors.toList());
        } else {
            // "all" tab: approved problems, plus any problems authored by the current user
            // so they persist on refresh
            exercises = exercises.stream()
                    .filter(e -> isApproved(e)
                            || (user != null && e.getAuthor() != null && e.getAuthor().getId().equals(user.getId())))
                    .collect(Collectors.toList());
        }

        return exercises.stream()
                .map(e -> toDto(e, user))
                .collect(Collectors.toList());
    }

    private boolean isApproved(Exercise e) {
        return e.getModerationStatus() == null || "APPROVED".equalsIgnoreCase(e.getModerationStatus());
    }

    /**
     * Retrieves full problem details for a single exercise by ID.
     *
     * @param id       The unique ID of the problem.
     * @param username Username of the authenticated caller (for solving status
     *                 context).
     * @return An {@link Optional} containing the {@link ExerciseDto}, or empty if
     *         not found.
     */
    public Optional<ExerciseDto> getById(Long id, String username) {
        User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;
        return exerciseRepository.findById(id).map(e -> toDto(e, user));
    }

    /**
     * Selects and returns a random problem from the catalog for the "Pick Random"
     * feature.
     *
     * @param username Username of the authenticated caller.
     * @return Random {@link ExerciseDto}, or {@code null} if the catalog is empty.
     */
    public ExerciseDto getRandom(String username) {
        List<Exercise> all = exerciseRepository.findAll();
        if (all.isEmpty())
            return null;
        int idx = (int) (Math.random() * all.size());
        User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;
        return toDto(all.get(idx), user);
    }

    /**
     * Creates and persists a new custom coding problem authored by an authenticated
     * user.
     *
     * @param req      The problem creation request payload.
     * @param username The username of the author.
     * @return The persisted {@link ExerciseDto} representation.
     */
    @Transactional
    public ExerciseDto create(ExerciseDto.CreateExerciseRequest req, String username) {
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw com.codeforge.exception.ApiException.badRequest("Title is required");
        }
        long nextId = exerciseRepository.findMaxId() + 1;

        Exercise exercise = new Exercise();
        exercise.setId(nextId);
        exercise.setTitle(req.getTitle().trim());
        exercise.setDescription(req.getStatement() != null ? req.getStatement() : "");
        exercise.setDifficulty(req.getDifficulty() != null ? req.getDifficulty() : "Medium");
        exercise.setTopics(req.getTopics() != null ? String.join(", ", req.getTopics()) : "");
        exercise.setAcceptanceRate(new BigDecimal("100.00"));
        exercise.setTimeLimitMs(2000);
        exercise.setMemoryLimitMb(256);
        exercise.setIsCustom(true);
        exercise.setCreatedAt(LocalDateTime.now());

        User author = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElse(null);

        if (author != null) {
            exercise.setAuthor(author);
            boolean isAdmin = "ADMIN".equalsIgnoreCase(author.getRole());
            exercise.setModerationStatus(isAdmin ? "APPROVED" : "PENDING");
        } else {
            exercise.setModerationStatus("PENDING");
        }

        Exercise saved = exerciseRepository.save(exercise);
        return toDto(saved, author);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    /**
     * Maps an {@link Exercise} entity to an {@link ExerciseDto}, calculating
     * user-specific progress.
     *
     * @param e    The exercise entity.
     * @param user The calling user (may be {@code null} for anonymous requests).
     * @return Enriched {@link ExerciseDto}.
     */
    private ExerciseDto toDto(Exercise e, User user) {
        // Format acceptance rate percentage string
        String acceptance = e.getAcceptanceRate() != null
                ? e.getAcceptanceRate().toPlainString() + "%"
                : "N/A";

        // Map sample test cases for presentation in the IDE
        List<ExerciseDto.TestCaseDto> testCaseDtos = e.getTestCases() == null ? List.of()
                : e.getTestCases().stream()
                        .filter(tc -> Boolean.TRUE.equals(tc.getIsSample()))
                        .map(tc -> ExerciseDto.TestCaseDto.builder()
                                .id(tc.getId())
                                .name(tc.getCaseName())
                                .input(tc.getInputData())
                                .expected(tc.getExpectedOutput())
                                .passed(null)
                                .build())
                        .collect(Collectors.toList());

        // Compute personal progress status for the authenticated user
        String status = "unattempted";
        if (user != null) {
            List<Submission> subs = submissionRepository.findByUserIdAndExerciseIdOrderBySubmittedAtDesc(user.getId(),
                    e.getId());
            if (subs.stream().anyMatch(s -> "Accepted".equals(s.getStatus()))) {
                status = "solved";
            } else if (!subs.isEmpty()) {
                status = "in-progress";
            }
        }

        // Check if the authenticated user has bookmarked this exercise
        boolean saved = user != null && savedExerciseRepository.existsByUserIdAndExerciseId(user.getId(), e.getId());

        // Determine ownership and author display name
        boolean isMine = user != null && e.getAuthor() != null
                && e.getAuthor().getId().equals(user.getId());
        String authorName = e.getAuthor() != null ? e.getAuthor().getFullName() : "CodeForge";

        return ExerciseDto.builder()
                .id(e.getId())
                .title(e.getTitle())
                .difficulty(e.getDifficulty())
                .topics(e.getTopics() != null
                        ? List.of(e.getTopics().split(",\\s*"))
                        : List.of())
                .acceptance(acceptance)
                .status(status)
                .saved(saved)
                .isMine(isMine)
                .author(authorName)
                .description(e.getDescription())
                .constraints(e.getConstraints())
                .starterCode(e.getStarterCodePython())
                .testCases(testCaseDtos)
                .moderationStatus(e.getModerationStatus() != null ? e.getModerationStatus() : "APPROVED")
                .rejectionReason(e.getRejectionReason())
                .build();
    }

    /**
     * Toggles the bookmark status of an exercise for the specified user.
     *
     * @param exerciseId The exercise identifier.
     * @param username   The username of the authenticated user.
     * @return {@code true} if newly saved; {@code false} if unsaved.
     */
    @Transactional
    public boolean toggleBookmark(Long exerciseId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.unauthorized("User not found: " + username));
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> ApiException.notFound("Exercise not found: " + exerciseId));

        Optional<SavedExercise> existing = savedExerciseRepository.findByUserIdAndExerciseId(user.getId(),
                exercise.getId());
        if (existing.isPresent()) {
            savedExerciseRepository.delete(existing.get());
            return false;
        } else {
            savedExerciseRepository.save(SavedExercise.builder()
                    .user(user)
                    .exercise(exercise)
                    .createdAt(LocalDateTime.now())
                    .build());
            return true;
        }
    }

    /**
     * Retrieves the submission history for a specific exercise submitted by the
     * user.
     *
     * @param exerciseId The exercise identifier.
     * @param username   The username of the user.
     * @return List of {@link SubmissionDto} ordered descending by submission
     *         timestamp.
     */
    public List<SubmissionDto> getSubmissions(Long exerciseId, String username) {
        if (username == null)
            return List.of();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null)
            return List.of();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");
        return submissionRepository.findByUserIdAndExerciseIdOrderBySubmittedAtDesc(user.getId(), exerciseId)
                .stream()
                .map(s -> SubmissionDto.builder()
                        .id(s.getId())
                        .exerciseId(s.getExercise() != null ? s.getExercise().getId() : exerciseId)
                        .exerciseTitle(s.getExercise() != null ? s.getExercise().getTitle() : "Problem #" + exerciseId)
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
                        .date(s.getSubmittedAt() != null ? s.getSubmittedAt().format(formatter) : "")
                        .submittedAt(s.getSubmittedAt())
                        .build())
                .collect(Collectors.toList());
    }
}