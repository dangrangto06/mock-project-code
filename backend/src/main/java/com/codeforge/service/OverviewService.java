package com.codeforge.service;

import com.codeforge.dto.OverviewResponse;
import com.codeforge.entity.Exercise;
import com.codeforge.entity.Submission;
import com.codeforge.entity.User;
import com.codeforge.repository.ExerciseRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service aggregating dashboard telemetry, personalized user metrics, and practice roadmaps.
 *
 * <p>Consolidates database records across users, exercises, and submissions to formulate
 * a unified {@link OverviewResponse} that populates the CodeForge main dashboard.</p>
 */
@Service
@RequiredArgsConstructor
public class OverviewService {

    /** Repository for accessing exercise data to populate recommended challenges. */
    private final ExerciseRepository exerciseRepository;

    /** Repository for retrieving user submission history and recent activities. */
    private final SubmissionRepository submissionRepository;

    /** Repository for fetching profile metrics and practice telemetry. */
    private final UserRepository userRepository;

    /**
     * Builds and returns the comprehensive overview response for a given user.
     *
     * <p>Data Assembly Lifecycle:</p>
     * <ol>
     *   <li>Retrieve user profile from database; falls back to guest defaults if unauthenticated.</li>
     *   <li>Populate user metrics (streak, solved counts, acceptance rates, and practice hours).</li>
     *   <li>Select the next recommended coding challenge from the catalog.</li>
     *   <li>Formulate weekly goal progress and day-by-day practice status.</li>
     *   <li>Populate structured learning paths (Arrays & Hashing, Dynamic Programming, Graphs).</li>
     *   <li>Retrieve up to 5 most recent submissions to populate the recent activity log.</li>
     * </ol>
     *
     * @param username The username of the user requesting the overview (or "anonymous").
     * @return Fully populated {@link OverviewResponse} DTO.
     */
    @Transactional(readOnly = true)
    public OverviewResponse getOverview(String username) {

        // Fetch the authenticated user entity from the repository
        User user = userRepository.findByUsername(username).orElse(null);

        // Fetch user submissions once for metrics computation
        List<Submission> userSubs = (user != null)
                ? submissionRepository.findByUserIdOrderBySubmittedAtDesc(user.getId())
                : List.of();

        // ── Weekly goal (Mon–Sun) ─────────────────────────────────────────────
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String[] dayLabels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        List<OverviewResponse.DayStatus> weekDays = new ArrayList<>();
        int weeklyDoneCount = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            boolean done = false;
            if (user != null) {
                done = userSubs.stream()
                        .anyMatch(s -> s.getSubmittedAt() != null && s.getSubmittedAt().toLocalDate().equals(d));
            }
            if (done) weeklyDoneCount++;
            weekDays.add(new OverviewResponse.DayStatus(dayLabels[i], done));
        }

        int target = 5;
        int pct = (user != null && target > 0) ? Math.min(100, (int) Math.round((weeklyDoneCount * 100.0) / target)) : 0;

        OverviewResponse.WeeklyGoal weeklyGoal = OverviewResponse.WeeklyGoal.builder()
                .done(weeklyDoneCount)
                .target(target)
                .pct(pct)
                .days(weekDays)
                .build();

        // ── User summary ──────────────────────────────────────────────────────
        OverviewResponse.UserSummary userSummary = new OverviewResponse.UserSummary();
        if (user != null) {
            String firstName = user.getFullName() != null
                    ? user.getFullName().split(" ")[0] : user.getUsername();
            String initials = buildInitials(user.getFullName());

            userSummary.setName(user.getFullName());
            userSummary.setInitials(initials);
            userSummary.setGreetingName(firstName);
            userSummary.setStreakDays(user.getStreakDays() != null ? user.getStreakDays() : 0);
            userSummary.setSolvedCount(user.getSolvedCount() != null ? user.getSolvedCount() : 0);
            userSummary.setSolvedWeek(weeklyDoneCount);
            userSummary.setAcceptanceRate(user.getAcceptanceRate() != null
                    ? user.getAcceptanceRate().toPlainString() + "%" : "0%");
            userSummary.setAcceptanceDelta("+2.1%");
            userSummary.setPracticeTime(user.getPracticeTimeHours() != null
                    ? user.getPracticeTimeHours().toPlainString() + "h" : "0h");
            userSummary.setPracticeDelta("+1.5h");
        } else {
            // Anonymous guest defaults
            userSummary.setName("Guest");
            userSummary.setInitials("G");
            userSummary.setGreetingName("Guest");
        }

        // ── Next challenge ────────────────────────────────────────────────────
        List<Exercise> allExercises = exerciseRepository.findAll();
        Exercise nextEx = null;
        if (user != null) {
            Set<Long> solvedIds = userSubs.stream()
                    .filter(s -> "Accepted".equalsIgnoreCase(s.getStatus()) && s.getExercise() != null)
                    .map(s -> s.getExercise().getId())
                    .collect(Collectors.toSet());
            nextEx = allExercises.stream()
                    .filter(e -> !solvedIds.contains(e.getId()))
                    .findFirst()
                    .orElse(allExercises.isEmpty() ? null : allExercises.get(0));
        } else {
            nextEx = allExercises.isEmpty() ? null : allExercises.get(0);
        }

        OverviewResponse.NextChallenge nextChallenge = nextEx != null
                ? OverviewResponse.NextChallenge.builder()
                        .id(nextEx.getId())
                        .title(nextEx.getTitle())
                        .difficulty(nextEx.getDifficulty())
                        .status("Continue")
                        .description(nextEx.getDescription() != null
                                ? nextEx.getDescription().substring(0, Math.min(120, nextEx.getDescription().length())) + "…"
                                : "")
                        .meta(nextEx.getTopics())
                        .stepInfo("Step 1 of 3")
                        .target(10)
                        .previewNumbers(List.of(2, 7, 11, 15))
                        .goal("Solve the challenge to build your intuition")
                        .lastOpened("Recently opened")
                        .build()
                : null;

        // ── Learning paths ────────────────────────────────────────────────────
        List<OverviewResponse.LearningPath> learningPaths = List.of(
                new OverviewResponse.LearningPath("arrays", "Arrays & Hashing", "Foundation", "🧩", 12, 20, 60),
                new OverviewResponse.LearningPath("dp", "Dynamic Programming", "Intermediate", "🔁", 5, 15, 33),
                new OverviewResponse.LearningPath("graphs", "Graphs & Trees", "Advanced", "🌲", 3, 18, 17)
        );

        // ── Recent activity ───────────────────────────────────────────────────
        List<OverviewResponse.RecentActivityItem> recentActivity = List.of();
        if (user != null) {
            recentActivity = submissionRepository
                    .findTop5ByUserIdOrderBySubmittedAtDesc(user.getId())
                    .stream()
                    .map(s -> OverviewResponse.RecentActivityItem.builder()
                            .id(s.getId())
                            .title(s.getExercise() != null ? s.getExercise().getTitle() : "Problem #" + s.getId())
                            .status(s.getStatus())
                            .runtime(s.getRuntimeMs() != null ? s.getRuntimeMs() + " ms" : "—")
                            .memory(s.getMemoryMb() != null ? s.getMemoryMb().toPlainString() + " MB" : "—")
                            .date(s.getSubmittedAt() != null
                                    ? s.getSubmittedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy")) : "")
                            .build())
                    .collect(Collectors.toList());
        }

        return OverviewResponse.builder()
                .user(userSummary)
                .nextChallenge(nextChallenge)
                .weeklyGoal(weeklyGoal)
                .learningPaths(learningPaths)
                .recentActivity(recentActivity)
                .build();
    }

    /**
     * Extracts uppercase avatar initials from a user's full name.
     *
     * <p>Example: {@code "Alex Morgan"} &rarr; {@code "AM"}, {@code "Hieu"} &rarr; {@code "H"}.</p>
     *
     * @param fullName The user's full display name string.
     * @return Uppercase initials string, or "?" if empty.
     */
    private String buildInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
