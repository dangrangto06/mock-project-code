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

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;

    // ── Query ─────────────────────────────────────────────────────────────────

    public List<ExerciseDto> getAll(String search, String difficulty, String topic,
                                    String tab, String username) {
        List<Exercise> exercises = exerciseRepository.searchExercises(
                (search != null && !search.isBlank()) ? search.trim() : null,
                (difficulty != null && !difficulty.isBlank() && !"all".equalsIgnoreCase(difficulty)) ? difficulty : null,
                (topic != null && !topic.isBlank() && !"all".equalsIgnoreCase(topic)) ? topic : null
        );

        User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;

        return exercises.stream()
                .map(e -> toDto(e, user))
                .collect(Collectors.toList());
    }

    public Optional<ExerciseDto> getById(Long id, String username) {
        User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;
        return exerciseRepository.findById(id).map(e -> toDto(e, user));
    }

    public ExerciseDto getRandom(String username) {
        List<Exercise> all = exerciseRepository.findAll();
        if (all.isEmpty()) return null;
        int idx = (int) (Math.random() * all.size());
        User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;
        return toDto(all.get(idx), user);
    }

    public ExerciseDto create(ExerciseDto.CreateExerciseRequest req, String username) {
        Exercise exercise = new Exercise();
        exercise.setTitle(req.getTitle());
        exercise.setDescription(req.getStatement());
        exercise.setDifficulty(req.getDifficulty() != null ? req.getDifficulty() : "Medium");
        exercise.setTopics(req.getTopics() != null ? String.join(", ", req.getTopics()) : "");
        exercise.setTimeLimitMs(2000);
        exercise.setMemoryLimitMb(256);
        exercise.setIsCustom(true);

        userRepository.findByUsername(username).ifPresent(exercise::setAuthor);

        Exercise saved = exerciseRepository.save(exercise);
        return toDto(saved, null);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private ExerciseDto toDto(Exercise e, User user) {
        // Determine acceptance string
        String acceptance = e.getAcceptanceRate() != null
                ? e.getAcceptanceRate().toPlainString() + "%" : "N/A";

        // Build test cases list (sample only for the list view)
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

        // Status for the authenticated user
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
