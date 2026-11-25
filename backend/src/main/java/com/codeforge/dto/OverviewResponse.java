package com.codeforge.dto;

import lombok.*;

import java.util.List;

/**
 * Composite Data Transfer Object (DTO) powering the user dashboard and overview screen.
 *
 * <p>Consolidates user identity metrics, streak status, recommended next algorithmic challenge,
 * weekly goal completion calendar, structured learning tracks, and recent submission logs
 * into a single high-efficiency API response payload ({@code GET /api/overview}).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverviewResponse {

    /**
     * Profile metrics and practice statistics for the authenticated user (or Guest).
     */
    private UserSummary user;

    /**
     * Recommended algorithm challenge to solve next.
     */
    private NextChallenge nextChallenge;

    /**
     * Weekly target metrics and day-by-day practice status.
     */
    private WeeklyGoal weeklyGoal;

    /**
     * Curated skill learning roadmaps and completion progress.
     */
    private List<LearningPath> learningPaths;

    /**
     * History of the user's most recent code submissions.
     */
    private List<RecentActivityItem> recentActivity;

    /**
     * User profile summary and performance telemetry metrics.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserSummary {

        /** Full display name (e.g., "Alex Morgan"). */
        private String name;

        /** Derived avatar initials (e.g., "AM"). */
        private String initials;

        /** First name used in personalized greetings (e.g., "Alex"). */
        private String greetingName;

        /** Consecutive active practice days count. */
        private Integer streakDays;

        /** Lifetime count of unique problems solved. */
        private Integer solvedCount;

        /** Number of problems solved in the current calendar week. */
        private Integer solvedWeek;

        /** Platform submission acceptance rate formatted as a percentage string (e.g., "76.2%"). */
        private String acceptanceRate;

        /** Weekly delta in acceptance rate (e.g., "+2.1%"). */
        private String acceptanceDelta;

        /** Cumulative practice duration formatted as hours (e.g., "12.5h"). */
        private String practiceTime;

        /** Weekly delta in practice duration (e.g., "+1.5h"). */
        private String practiceDelta;
    }

    /**
     * Details of the recommended challenge surfaced on the dashboard.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NextChallenge {

        /** Challenge ID. */
        private Long id;

        /** Challenge title. */
        private String title;

        /** Difficulty tier ("Easy", "Medium", "Hard"). */
        private String difficulty;

        /** Call to action status label (e.g., "Continue" or "Start"). */
        private String status;

        /** Truncated problem preview snippet. */
        private String description;

        /** Category metadata and topic tags. */
        private String meta;

        /** Step indicator in current learning track (e.g., "Step 1 of 3"). */
        private String stepInfo;

        /** Numerical target parameter for algorithm demonstration. */
        private Integer target;

        /** Preview array elements for visual explanation. */
        private List<Integer> previewNumbers;

        /** Short summary of the algorithmic objective. */
        private String goal;

        /** Timestamp indicator of when the problem was last opened. */
        private String lastOpened;
    }

    /**
     * Weekly goal tracker measuring problems solved against target.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeeklyGoal {

        /** Completed problems this week. */
        private Integer done;

        /** Target problems to solve this week. */
        private Integer target;

        /** Completion percentage (0 - 100). */
        private Integer pct;

        /** 7-day breakdown of completion status. */
        private List<DayStatus> days;
    }

    /**
     * Daily status record for the 7-day weekly goal bar.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DayStatus {

        /** Day of the week abbreviation (e.g., "Mon", "Tue"). */
        private String day;

        /** Whether the daily practice goal was met on this day. */
        private Boolean done;
    }

    /**
     * Structured algorithmic learning path progress.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LearningPath {

        /** Unique identifier for the learning path (e.g., "arrays", "dp"). */
        private String id;

        /** Title of the curriculum track (e.g., "Arrays & Hashing"). */
        private String title;

        /** Skill level subtitle (e.g., "Foundation", "Advanced"). */
        private String subtitle;

        /** Visual emoji or icon representing the track. */
        private String icon;

        /** Number of challenges completed within this path. */
        private Integer completed;

        /** Total challenges available in this path. */
        private Integer total;

        /** Percentage of the curriculum completed. */
        private Integer pct;
    }

    /**
     * Activity log item representing a recent submission for the dashboard feed.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentActivityItem {

        /** Submission record ID. */
        private Long id;

        /** Title of the problem submitted. */
        private String title;

        /** Submission verdict status (e.g., "Accepted", "Wrong Answer"). */
        private String status;

        /** Formatted execution duration (e.g., "32 ms"). */
        private String runtime;

        /** Formatted memory consumption (e.g., "17.4 MB"). */
        private String memory;

        /** Relative or absolute submission date (e.g., "Oct 28, 2026"). */
        private String date;
    }
}
