package com.codeforge.service;

import com.codeforge.dto.OverviewResponse;
import com.codeforge.entity.User;
import com.codeforge.repository.ExerciseRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OverviewService {

    private final ExerciseRepository exerciseRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public OverviewResponse getOverview(String username) {

        // Fetch the authenticated user
        User user = userRepository.findByUsername(username).orElse(null);

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
            userSummary.setSolvedWeek(3); // weekly solved (placeholder)
            userSummary.setAcceptanceRate(user.getAcceptanceRate() != null
                    ? user.getAcceptanceRate().toPlainString() + "%" : "0%");
            userSummary.setAcceptanceDelta("+2.1%");
            userSummary.setPracticeTime(user.getPracticeTimeHours() != null
                    ? user.getPracticeTimeHours().toPlainString() + "h" : "0h");
            userSummary.setPracticeDelta("+1.5h");
        } else {
            userSummary.setName("Guest");
            userSummary.setInitials("G");
            userSummary.setGreetingName("Guest");
        }

        // ── Next challenge ────────────────────────────────────────────────────
        OverviewResponse.NextChallenge nextChallenge = exerciseRepository.findAll().stream()
                .findFirst()
                .map(e -> OverviewResponse.NextChallenge.builder()
                        .id(e.getId())
                        .title(e.getTitle())
                        .difficulty(e.getDifficulty())
                        .status("Continue")
                        .description(e.getDescription() != null
                                ? e.getDescription().substring(0, Math.min(120, e.getDescription().length())) + "…"
                                : "")
                        .meta(e.getTopics())
                        .stepInfo("Step 1 of 3")
                        .target(10)
                        .previewNumbers(List.of(2, 7, 11, 15))
                        .goal("Return indices of two numbers that add up to target")
                        .lastOpened("Recently opened")
                        .build())
                .orElse(null);

        // ── Weekly goal ───────────────────────────────────────────────────────
        OverviewResponse.WeeklyGoal weeklyGoal = OverviewResponse.WeeklyGoal.builder()
                .done(3).target(5).pct(60)
                .days(List.of(
                        new OverviewResponse.DayStatus("Mon", true),
                        new OverviewResponse.DayStatus("Tue", true),
                        new OverviewResponse.DayStatus("Wed", true),
                        new OverviewResponse.DayStatus("Thu", false),
                        new OverviewResponse.DayStatus("Fri", false),
                        new OverviewResponse.DayStatus("Sat", false),
                        new OverviewResponse.DayStatus("Sun", false)))
                .build();

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
                            .title(s.getExercise().getTitle())
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

    private String buildInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
