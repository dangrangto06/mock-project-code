package com.codeforge.service;

import com.codeforge.dto.ExerciseDto;
import com.codeforge.entity.Exercise;
import com.codeforge.entity.User;
import com.codeforge.repository.ExerciseRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing algorithmic problems, search queries, problem detail lookups, and creation.
 *
 * <p>Coordinates data retrieval across {@link ExerciseRepository}, {@link SubmissionRepository},
 * and {@link UserRepository}. Enriches problem data with contextual user status (e.g., whether
 * the authenticated user has already solved or attempted a challenge).</p>
 */
@Service
@RequiredArgsConstructor
public class ExerciseService {

    /** Repository for accessing and searching {@link Exercise} entities. */
    private final ExerciseRepository exerciseRepository;

    /** Repository for looking up user identities. */
    private final UserRepository userRepository;

    /** Repository for checking submission history per exercise. */
    private final SubmissionRepository submissionRepository;

    // ── Query ─────────────────────────────────────────────────────────────────

    /**
     * Retrieves a filtered list of exercises enriched with caller-specific progress metadata.
     *
     * <p>Applies multi-criteria search filtering (by search keyword, difficulty, and topic)
     * and maps each entity into an {@link ExerciseDto} reflecting the user's solved/in-progress status.</p>
     *
     * @param search     Optional text keyword matching title or problem ID.
     * @param difficulty Optional difficulty tier ("Easy", "Medium", "Hard") or "all".
     * @param topic      Optional category topic (e.g. "Arrays", "Hash map") or "all".
     * @param tab        Optional tab filter ("all", "saved", "my").
     * @param username   Username of the authenticated user, or {@code null} if anonymous.
     * @return List of matching {@link ExerciseDto} objects.
     */
    public List<ExerciseDto> getAll(String search, String difficulty, String topic,
                                    String tab, String username) {
        // Query exercises using dynamic search criteria
        List<Exercise> exercises = exerciseRepository.searchExercises(
                (search != null && !search.isBlank()) ? search.trim() : null,
                (difficulty != null && !difficulty.isBlank() && !"all".equalsIgnoreCase(difficulty)) ? difficulty : null,
                (topic != null && !topic.isBlank() && !"all".equalsIgnoreCase(topic)) ? topic : null
        );

        // Fetch user entity if authenticated to evaluate problem progress status
        User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;

        return exercises.stream()
                .map(e -> toDto(e, user))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves full problem details for a single exercise by ID.
     *
     * @param id       The unique ID of the problem.
     * @param username Username of the authenticated caller (for solving status context).
     * @return An {@link Optional} containing the {@link ExerciseDto}, or empty if not found.
     */
    public Optional<ExerciseDto> getById(Long id, String username) {
        User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;
        return exerciseRepository.findById(id).map(e -> toDto(e, user));
    }

    /**
     * Selects and returns a random problem from the catalog for the "Pick Random" feature.
     *
     * @param username Username of the authenticated caller.
     * @return Random {@link ExerciseDto}, or {@code null} if the catalog is empty.
     */
    public ExerciseDto getRandom(String username) {
        List<Exercise> all = exerciseRepository.findAll();
        if (all.isEmpty()) return null;
        int idx = (int) (Math.random() * all.size());
        User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;
        return toDto(all.get(idx), user);
    }

    /**
     * Creates and persists a new custom coding problem authored by an authenticated user.
     *
     * @param req      The problem creation request payload.
     * @param username The username of the author.
     * @return The persisted {@link ExerciseDto} representation.
     */
    public ExerciseDto create(ExerciseDto.CreateExerciseRequest req, String username) {
        Exercise exercise = new Exercise();
        exercise.setTitle(req.getTitle());
        exercise.setDescription(req.getStatement());
        exercise.setDifficulty(req.getDifficulty() != null ? req.getDifficulty() : "Medium");
        exercise.setTopics(req.getTopics() != null ? String.join(", ", req.getTopics()) : "");
        exercise.setTimeLimitMs(2000);
        exercise.setMemoryLimitMb(256);
        exercise.setIsCustom(true);

        // Associate with the author account if present
        userRepository.findByUsername(username).ifPresent(exercise::setAuthor);

        Exercise saved = exerciseRepository.save(exercise);
        return toDto(saved, null);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    /**
     * Maps an {@link Exercise} entity to an {@link ExerciseDto}, calculating user-specific progress.
     *
     * @param e    The exercise entity.
     * @param user The calling user (may be {@code null} for anonymous requests).
     * @return Enriched {@link ExerciseDto}.
     */
    private ExerciseDto toDto(Exercise e, User user) {
        // Format acceptance rate percentage string
        String acceptance = e.getAcceptanceRate() != null
                ? e.getAcceptanceRate().toPlainString() + "%" : "N/A";

        // Map sample test cases for presentation in the IDE
        List<ExerciseDto.TestCaseDto> testCaseDtos = e.getTestCases() == null ? List.of() :
                e.getTestCases().stream()
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
            List<?> subs = submissionRepository.findByExerciseIdOrderBySubmittedAtDesc(e.getId());
            if (subs.stream().anyMatch(s -> "Accepted".equals(
                    ((com.codeforge.entity.Submission) s).getStatus()))) {
                status = "solved";
            } else if (!subs.isEmpty()) {
                status = "in-progress";
            }
        }

        // Determine ownership and author display name
        boolean isMine = user != null && e.getAuthor() != null
                && e.getAuthor().getId().equals(user.getId());
        String authorName = e.getAuthor() != null ? e.getAuthor().getFullName() : "CodeForge";

        return ExerciseDto.builder()
                .id(e.getId())
                .title(e.getTitle())
                .difficulty(e.getDifficulty())
                .topics(e.getTopics() != null
                        ? List.of(e.getTopics().split(",\\s*")) : List.of())
                .acceptance(acceptance)
                .status(status)
                .saved(false)
                .isMine(isMine)
                .author(authorName)
                .description(e.getDescription())
                .constraints(e.getConstraints())
                .testCases(testCaseDtos)
                .build();
    }
}
