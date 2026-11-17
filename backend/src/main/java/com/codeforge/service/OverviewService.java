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

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service aggregating dashboard telemetry, personalized user metrics, and
 * practice roadmaps.
 *
 * <p>
 * Consolidates database records across users, exercises, and submissions to
 * formulate
 * a unified {@link OverviewResponse} that populates the CodeForge main
 * dashboard.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OverviewService {

    /**
     * Repository for accessing exercise data to populate recommended challenges.
     */
    private final ExerciseRepository exerciseRepository;

    /** Repository for retrieving user submission history and recent activities. */
    private final SubmissionRepository submissionRepository;

    /** Repository for fetching profile metrics and practice telemetry. */
    private final UserRepository userRepository;

    /**
     * Builds and returns the comprehensive overview response for a given user.
     *
     * <p>
     * Data Assembly Lifecycle:
     * </p>
     * <ol>
     * <li>Retrieve user profile from database; falls back to guest defaults if
     * unauthenticated.</li>
     * <li>Populate user metrics (streak, solved counts, acceptance rates, and
     * practice hours).</li>
     * <li>Select the next recommended coding challenge from the catalog.</li>
     * <li>Formulate weekly goal progress and day-by-day practice status.</li>
     * <li>Populate structured learning paths (Arrays & Hashing, Dynamic
     * Programming, Graphs).</li>
     * <li>Retrieve up to 5 most recent submissions to populate the recent activity
     * log.</li>
     * </ol>
     *
     * @param username The username of the user requesting the overview (or
     *                 "anonymous").
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
        String[] dayLabels = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
        List<OverviewResponse.DayStatus> weekDays = new ArrayList<>();
        int weeklyDoneCount = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            boolean done = false;
            if (user != null) {
                done = userSubs.stream()
                        .anyMatch(s -> s.getSubmittedAt() != null && s.getSubmittedAt().toLocalDate().equals(d));
            }
            if (done)
                weeklyDoneCount++;
            weekDays.add(new OverviewResponse.DayStatus(dayLabels[i], done));
        }

        int target = 5;
        int pct = (user != null && target > 0) ? Math.min(100, (int) Math.round((weeklyDoneCount * 100.0) / target))
                : 0;

        OverviewResponse.WeeklyGoal weeklyGoal = OverviewResponse.WeeklyGoal.builder()
                .done(weeklyDoneCount)
                .target(target)
                .pct(pct)
                .days(weekDays)
                .build();

        // ── Solved Exercise IDs ───────────────────────────────────────────────
        Set<Long> solvedIds = (user != null)
                ? userSubs.stream()
                        .filter(s -> "Accepted".equalsIgnoreCase(s.getStatus()) && s.getExercise() != null)
                        .map(s -> s.getExercise().getId())
                        .collect(Collectors.toSet())
                : Set.of();

        // ── User summary ──────────────────────────────────────────────────────
        OverviewResponse.UserSummary userSummary = new OverviewResponse.UserSummary();
        if (user != null) {
            String firstName = user.getFullName() != null && !user.getFullName().isBlank()
                    ? user.getFullName().split(" ")[0]
                    : user.getUsername();
            String initials = buildInitials(user.getFullName());

            userSummary.setName(user.getFullName() != null ? user.getFullName() : user.getUsername());
            userSummary.setInitials(initials);
            userSummary.setGreetingName(firstName);
            userSummary.setStreakDays(user.getStreakDays() != null ? user.getStreakDays() : 0);
            userSummary.setSolvedCount(user.getSolvedCount() != null ? user.getSolvedCount() : solvedIds.size());
            userSummary.setSolvedWeek(weeklyDoneCount);

            if (userSubs.isEmpty()) {
                userSummary.setAcceptanceRate("0%");
                userSummary.setAcceptanceDelta("+0%");
                userSummary.setPracticeTime("0h");
                userSummary.setPracticeDelta("+0h");
            } else {
                userSummary.setAcceptanceRate(
                        user.getAcceptanceRate() != null && user.getAcceptanceRate().compareTo(BigDecimal.ZERO) > 0
                                ? user.getAcceptanceRate().toPlainString() + "%"
                                : "0%");

                // Compute real acceptanceDelta
                long thisWeekSubs = userSubs.stream()
                        .filter(s -> s.getSubmittedAt() != null && !s.getSubmittedAt().toLocalDate().isBefore(monday))
                        .count();
                long thisWeekAccepted = userSubs.stream()
                        .filter(s -> s.getSubmittedAt() != null && !s.getSubmittedAt().toLocalDate().isBefore(monday)
                                && "Accepted".equalsIgnoreCase(s.getStatus()))
                        .count();
                long priorSubs = userSubs.stream()
                        .filter(s -> s.getSubmittedAt() != null && s.getSubmittedAt().toLocalDate().isBefore(monday))
                        .count();
                long priorAccepted = userSubs.stream()
                        .filter(s -> s.getSubmittedAt() != null && s.getSubmittedAt().toLocalDate().isBefore(monday)
                                && "Accepted".equalsIgnoreCase(s.getStatus()))
                        .count();

                if (priorSubs > 0 && thisWeekSubs > 0) {
                    double priorRate = priorAccepted * 100.0 / priorSubs;
                    double currentRate = userSubs.stream().filter(s -> "Accepted".equalsIgnoreCase(s.getStatus()))
                            .count() * 100.0 / userSubs.size();
                    double delta = currentRate - priorRate;
                    userSummary.setAcceptanceDelta((delta >= 0 ? "+" : "") + String.format(Locale.US, "%.1f%%", delta));
                } else if (thisWeekSubs > 0) {
                    double currentRate = userSubs.stream().filter(s -> "Accepted".equalsIgnoreCase(s.getStatus()))
                            .count() * 100.0 / userSubs.size();
                    userSummary.setAcceptanceDelta(String.format(Locale.US, "+%.1f%%", currentRate));
                } else {
                    userSummary.setAcceptanceDelta("+0%");
                }

                // Practice time & delta
                userSummary.setPracticeTime(user.getPracticeTimeHours() != null
                        && user.getPracticeTimeHours().compareTo(BigDecimal.ZERO) > 0
                                ? user.getPracticeTimeHours().toPlainString() + "h"
                                : "0h");
                double thisWeekHours = Math.round(thisWeekSubs * 0.25 * 10.0) / 10.0;
                userSummary.setPracticeDelta(thisWeekHours > 0
                        ? String.format(Locale.US, "+%.1fh", thisWeekHours)
                        : "+0h");
            }
        } else {
            // Anonymous guest defaults
            userSummary.setName("Guest");
            userSummary.setInitials("G");
            userSummary.setGreetingName("Guest");
            userSummary.setStreakDays(0);
            userSummary.setSolvedCount(0);
            userSummary.setSolvedWeek(0);
            userSummary.setAcceptanceRate("0%");
            userSummary.setAcceptanceDelta("+0%");
            userSummary.setPracticeTime("0h");
            userSummary.setPracticeDelta("+0h");
        }

        // ── Next challenge ────────────────────────────────────────────────────
        List<Exercise> allExercises = exerciseRepository.findAll();
        List<Exercise> approvedExercises = allExercises.stream()
                .filter(e -> e.getModerationStatus() == null || "APPROVED".equalsIgnoreCase(e.getModerationStatus()))
                .toList();

        Exercise nextEx = null;
        if (user != null) {
            nextEx = approvedExercises.stream()
                    .filter(e -> !solvedIds.contains(e.getId()))
                    .findFirst()
                    .orElse(approvedExercises.isEmpty() ? null : approvedExercises.get(0));
        } else {
            nextEx = approvedExercises.isEmpty() ? null : approvedExercises.get(0);
        }

        OverviewResponse.NextChallenge nextChallenge = null;
        if (nextEx != null) {
            String challengeStatus = "Start";
            String lastOpenedStr = "Not started yet";

            if (user != null && !userSubs.isEmpty()) {
                final Long exId = nextEx.getId();
                List<Submission> exSubs = userSubs.stream()
                        .filter(s -> s.getExercise() != null && exId.equals(s.getExercise().getId()))
                        .toList();

                if (exSubs.isEmpty()) {
                    challengeStatus = "Start";
                    lastOpenedStr = "Not started yet";
                } else {
                    boolean isSolved = exSubs.stream().anyMatch(s -> "Accepted".equalsIgnoreCase(s.getStatus()));
                    challengeStatus = isSolved ? "Solved" : "Continue";
                    LocalDateTime lastSubTime = exSubs.get(0).getSubmittedAt();
                    if (lastSubTime != null) {
                        lastOpenedStr = "Last attempted " + lastSubTime.format(DateTimeFormatter.ofPattern("MMM d"));
                    } else {
                        lastOpenedStr = "Recently opened";
                    }
                }
            }

            nextChallenge = OverviewResponse.NextChallenge.builder()
                    .id(nextEx.getId())
                    .title(nextEx.getTitle())
                    .difficulty(nextEx.getDifficulty())
                    .status(challengeStatus)
                    .description(nextEx.getDescription() != null
                            ? nextEx.getDescription().substring(0, Math.min(120, nextEx.getDescription().length()))
                                    + "…"
                            : "")
                    .meta(nextEx.getTopics())
                    .stepInfo(nextEx.getTopics() != null ? nextEx.getTopics() : "Algorithm")
                    .target(10)
                    .previewNumbers(List.of(2, 7, 11, 15))
                    .goal("Solve the challenge to build your intuition")
                    .lastOpened(lastOpenedStr)
                    .build();
        }

        // ── Learning paths ────────────────────────────────────────────────────
        List<String> track1Keywords = List.of("arrays", "hash map", "hash set", "prefix sum");
        List<String> track2Keywords = List.of("strings", "sorting", "two pointers");
        List<String> track3Keywords = List.of("stacks", "heap", "trees");

        List<Exercise> track1Exercises = approvedExercises.stream().filter(e -> matchesTrack(e, track1Keywords))
                .toList();
        List<Exercise> track2Exercises = approvedExercises.stream().filter(e -> matchesTrack(e, track2Keywords))
                .toList();
        List<Exercise> track3Exercises = approvedExercises.stream().filter(e -> matchesTrack(e, track3Keywords))
                .toList();

        int track1Total = track1Exercises.size();
        int track1Completed = (int) track1Exercises.stream().filter(e -> solvedIds.contains(e.getId())).count();
        int track1Pct = track1Total > 0 ? (int) Math.round((track1Completed * 100.0) / track1Total) : 0;

        int track2Total = track2Exercises.size();
        int track2Completed = (int) track2Exercises.stream().filter(e -> solvedIds.contains(e.getId())).count();
        int track2Pct = track2Total > 0 ? (int) Math.round((track2Completed * 100.0) / track2Total) : 0;

        int track3Total = track3Exercises.size();
        int track3Completed = (int) track3Exercises.stream().filter(e -> solvedIds.contains(e.getId())).count();
        int track3Pct = track3Total > 0 ? (int) Math.round((track3Completed * 100.0) / track3Total) : 0;

        List<OverviewResponse.LearningPath> learningPaths = List.of(
                new OverviewResponse.LearningPath("arrays", "Arrays & Hashing", "Foundation", "🧩", track1Completed,
                        track1Total, track1Pct),
                new OverviewResponse.LearningPath("strings", "Two Pointers & Strings", "Essentials", "↔",
                        track2Completed, track2Total, track2Pct),
                new OverviewResponse.LearningPath("stacks", "Trees, Stacks & Heaps", "Problem Solving", "🌲",
                        track3Completed, track3Total, track3Pct));

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
                                    ? s.getSubmittedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                                    : "")
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
     * <p>
     * Example: {@code "Alex Morgan"} &rarr; {@code "AM"}, {@code "Hieu"} &rarr;
     * {@code "H"}.
     * </p>
     *
     * @param fullName The user's full display name string.
     * @return Uppercase initials string, or "?" if empty.
     */
    private String buildInitials(String fullName) {
        if (fullName == null || fullName.isBlank())
            return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1)
            return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    /**
     * Checks if an exercise matches any of the track's topic keywords.
     */
    private boolean matchesTrack(Exercise e, List<String> keywords) {
        if (e == null || e.getTopics() == null || e.getTopics().isBlank())
            return false;
        String lowerTopics = e.getTopics().toLowerCase();
        for (String kw : keywords) {
            if (lowerTopics.contains(kw.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}