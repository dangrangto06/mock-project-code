package com.codeforge.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverviewResponse {

    private UserSummary user;
    private NextChallenge nextChallenge;
    private WeeklyGoal weeklyGoal;
    private List<LearningPath> learningPaths;
    private List<RecentActivityItem> recentActivity;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserSummary {
        private String name;
        private String initials;
        private String greetingName;
        private Integer streakDays;
        private Integer solvedCount;
        private Integer solvedWeek;
        private String acceptanceRate;
        private String acceptanceDelta;
        private String practiceTime;
        private String practiceDelta;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NextChallenge {
        private Long id;
        private String title;
        private String difficulty;
        private String status;
        private String description;
        private String meta;
        private String stepInfo;
        private Integer target;
        private List<Integer> previewNumbers;
        private String goal;
        private String lastOpened;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeeklyGoal {
        private Integer done;
        private Integer target;
        private Integer pct;
        private List<DayStatus> days;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DayStatus {
        private String day;
        private Boolean done;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LearningPath {
        private String id;
        private String title;
        private String subtitle;
        private String icon;
        private Integer completed;
        private Integer total;
        private Integer pct;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentActivityItem {
        private Long id;
        private String title;
        private String status;
        private String runtime;
        private String memory;
        private String date;
    }
}

