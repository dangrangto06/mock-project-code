package com.codeforge.service;

import com.codeforge.dto.AdminDto;
import com.codeforge.entity.CommunityReport;
import com.codeforge.entity.Discussion;
import com.codeforge.entity.DiscussionReply;
import com.codeforge.entity.Exercise;
import com.codeforge.entity.PageVisitLog;
import com.codeforge.entity.User;
import com.codeforge.entity.UserLoginLog;
import com.codeforge.exception.ApiException;
import com.codeforge.repository.CommunityReportRepository;
import com.codeforge.repository.DiscussionReplyRepository;
import com.codeforge.repository.DiscussionRepository;
import com.codeforge.repository.ExerciseRepository;
import com.codeforge.repository.PageVisitLogRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.repository.UserLoginLogRepository;
import com.codeforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service managing administrative operations, moderation queue decisions, and
 * user roles.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

        private final UserRepository userRepository;
        private final ExerciseRepository exerciseRepository;
        private final DiscussionRepository discussionRepository;
        private final DiscussionReplyRepository discussionReplyRepository;
        private final CommunityReportRepository communityReportRepository;
        private final SubmissionRepository submissionRepository;
        private final com.codeforge.repository.FeedbackRepository feedbackRepository;
        private final UserLoginLogRepository userLoginLogRepository;
        private final PageVisitLogRepository pageVisitLogRepository;

        /**
         * Aggregates platform telemetry and pending counts.
         */
        @Transactional(readOnly = true)
        public AdminDto.StatsResponse getStats() {
                long totalUsers = userRepository.count();
                long totalProblems = exerciseRepository.count();
                long totalDiscussions = discussionRepository.count();
                long totalSubmissions = submissionRepository.count();
                long pendingProblemsCount = exerciseRepository.countByModerationStatus("PENDING");
                long pendingDiscussionsCount = discussionRepository.countByModerationStatus("PENDING");
                long pendingReportsCount = communityReportRepository.countByStatus("PENDING");
                long totalFeedbacks = feedbackRepository.count();
                long pendingFeedbacksCount = feedbackRepository.countByStatus("NEW");

                return AdminDto.StatsResponse.builder()
                                .totalUsers(totalUsers)
                                .totalProblems(totalProblems)
                                .totalDiscussions(totalDiscussions)
                                .totalSubmissions(totalSubmissions)
                                .totalFeedbacks(totalFeedbacks)
                                .pendingProblemsCount(pendingProblemsCount)
                                .pendingDiscussionsCount(pendingDiscussionsCount)
                                .pendingReportsCount(pendingReportsCount)
                                .pendingFeedbacksCount(pendingFeedbacksCount)
                                .build();
        }

        /**
         * Retrieves all items currently awaiting moderator review.
         */
        @Transactional(readOnly = true)
        public AdminDto.ModerationQueueResponse getModerationQueue() {
                List<Exercise> pendingProblems = exerciseRepository
                                .findByModerationStatusOrderByCreatedAtDesc("PENDING");
                List<Discussion> pendingDiscussions = discussionRepository
                                .findByModerationStatusOrderByCreatedAtDesc("PENDING");
                List<CommunityReport> pendingReports = communityReportRepository
                                .findByStatusOrderByCreatedAtDesc("PENDING");

                List<AdminDto.PendingProblemDto> problemDtos = pendingProblems.stream()
                                .map(this::toPendingProblemDto)
                                .collect(Collectors.toList());

                List<AdminDto.PendingDiscussionDto> discussionDtos = pendingDiscussions.stream()
                                .map(this::toPendingDiscussionDto)
                                .collect(Collectors.toList());

                List<AdminDto.PendingReportDto> reportDtos = pendingReports.stream()
                                .map(this::toPendingReportDto)
                                .collect(Collectors.toList());

                return AdminDto.ModerationQueueResponse.builder()
                                .pendingProblems(problemDtos)
                                .pendingDiscussions(discussionDtos)
                                .pendingReports(reportDtos)
                                .build();
        }

        /**
         * Approves a custom exercise challenge.
         */
        @Transactional
        public void approveProblem(Long id) {
                Exercise exercise = exerciseRepository.findById(id)
                                .orElseThrow(() -> ApiException.notFound("Problem not found: " + id));

                exercise.setModerationStatus("APPROVED");
                exercise.setRejectionReason(null);
                exerciseRepository.save(exercise);
        }

        /**
         * Rejects a custom exercise with administrator explanation.
         */
        @Transactional
        public void rejectProblem(Long id, String reason) {
                Exercise exercise = exerciseRepository.findById(id)
                                .orElseThrow(() -> ApiException.notFound("Problem not found: " + id));

                exercise.setModerationStatus("REJECTED");
                exercise.setRejectionReason(reason != null ? reason.trim() : "Does not meet guidelines.");
                exerciseRepository.save(exercise);
        }

        /**
         * Approves a community discussion topic.
         */
        @Transactional
        public void approveDiscussion(Long id) {
                Discussion discussion = discussionRepository.findById(id)
                                .orElseThrow(() -> ApiException.notFound("Discussion not found: " + id));

                discussion.setModerationStatus("APPROVED");
                discussion.setRejectionReason(null);
                discussionRepository.save(discussion);
        }

        /**
         * Rejects a community discussion topic with explanation.
         */
        @Transactional
        public void rejectDiscussion(Long id, String reason) {
                Discussion discussion = discussionRepository.findById(id)
                                .orElseThrow(() -> ApiException.notFound("Discussion not found: " + id));

                discussion.setModerationStatus("REJECTED");
                discussion.setRejectionReason(reason != null ? reason.trim() : "Violates community guidelines.");
                discussionRepository.save(discussion);
        }

        /**
         * Returns full directory of registered platform users.
         */
        @Transactional(readOnly = true)
        public List<AdminDto.AdminUserDto> getUsers() {
                return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                                .stream()
                                .map(u -> AdminDto.AdminUserDto.builder()
                                                .id(u.getId())
                                                .username(u.getUsername())
                                                .email(u.getEmail())
                                                .fullName(u.getFullName())
                                                .role(u.getRole() != null ? u.getRole() : "Learner")
                                                .solvedCount(u.getSolvedCount() != null ? u.getSolvedCount() : 0)
                                                .createdAt(u.getCreatedAt())
                                                .build())
                                .collect(Collectors.toList());
        }

        /**
         * Changes a user's authorization role.
         */
        @Transactional
        public void updateUserRole(Long userId, String role) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> ApiException.notFound("User not found: " + userId));

                String normalizedRole = "Admin".equalsIgnoreCase(role) ? "Admin" : "Learner";
                user.setRole(normalizedRole);
                userRepository.save(user);
        }

        private AdminDto.PendingProblemDto toPendingProblemDto(Exercise e) {
                List<String> topicsList = e.getTopics() != null
                                ? Arrays.stream(e.getTopics().split(","))
                                                .map(String::trim)
                                                .filter(s -> !s.isEmpty())
                                                .collect(Collectors.toList())
                                : List.of();

                String authorName = e.getAuthor() != null ? e.getAuthor().getFullName() : "Anonymous";
                String authorEmail = e.getAuthor() != null ? e.getAuthor().getEmail() : "N/A";

                return AdminDto.PendingProblemDto.builder()
                                .id(e.getId())
                                .title(e.getTitle())
                                .difficulty(e.getDifficulty())
                                .topics(topicsList)
                                .description(e.getDescription())
                                .authorName(authorName)
                                .authorEmail(authorEmail)
                                .createdAt(e.getCreatedAt())
                                .build();
        }

        private AdminDto.PendingDiscussionDto toPendingDiscussionDto(Discussion d) {
                String authorName = d.getAuthor() != null ? d.getAuthor().getFullName() : "Anonymous";
                String authorEmail = d.getAuthor() != null ? d.getAuthor().getEmail() : "N/A";

                return AdminDto.PendingDiscussionDto.builder()
                                .id(d.getId())
                                .title(d.getTitle())
                                .category(d.getCategory())
                                .problemId(d.getProblemId())
                                .problemTitle(d.getProblemTitle())
                                .content(d.getContent())
                                .authorName(authorName)
                                .authorEmail(authorEmail)
                                .createdAt(d.getCreatedAt())
                                .build();
        }

        private AdminDto.PendingReportDto toPendingReportDto(CommunityReport r) {
                String targetTitle = "N/A";
                String targetContent = "";
                String targetAuthorName = "Unknown";

                if ("DISCUSSION".equalsIgnoreCase(r.getReportType())) {
                        Optional<Discussion> discOpt = discussionRepository.findById(r.getTargetId());
                        if (discOpt.isPresent()) {
                                Discussion disc = discOpt.get();
                                targetTitle = disc.getTitle();
                                targetContent = disc.getContent();
                                targetAuthorName = disc.getAuthor() != null ? disc.getAuthor().getFullName() : "Anonymous";
                        } else {
                                targetTitle = "Discussion #" + r.getTargetId() + " (Already removed)";
                        }
                } else if ("REPLY".equalsIgnoreCase(r.getReportType())) {
                        Optional<DiscussionReply> repOpt = discussionReplyRepository.findById(r.getTargetId());
                        if (repOpt.isPresent()) {
                                DiscussionReply rep = repOpt.get();
                                targetTitle = (rep.getDiscussion() != null && rep.getDiscussion().getTitle() != null)
                                                ? "Comment on: " + rep.getDiscussion().getTitle()
                                                : "Comment #" + rep.getId();
                                targetContent = rep.getContent();
                                targetAuthorName = rep.getAuthor() != null ? rep.getAuthor().getFullName() : "Anonymous";
                        } else {
                                targetTitle = "Comment #" + r.getTargetId() + " (Already removed)";
                        }
                }

                String reporterName = r.getReporter() != null ? r.getReporter().getFullName() : "Anonymous";
                String reporterEmail = r.getReporter() != null ? r.getReporter().getEmail() : "N/A";

                return AdminDto.PendingReportDto.builder()
                                .id(r.getId())
                                .reportType(r.getReportType())
                                .targetId(r.getTargetId())
                                .targetTitle(targetTitle)
                                .targetContent(targetContent)
                                .targetAuthorName(targetAuthorName)
                                .reporterName(reporterName)
                                .reporterEmail(reporterEmail)
                                .reason(r.getReason())
                                .details(r.getDetails())
                                .createdAt(r.getCreatedAt())
                                .build();
        }

        /**
         * Dismisses a report without removing the target content.
         */
        @Transactional
        public void dismissReport(Long reportId) {
                CommunityReport report = communityReportRepository.findById(reportId)
                                .orElseThrow(() -> ApiException.notFound("Report not found: " + reportId));
                report.setStatus("DISMISSED");
                communityReportRepository.save(report);
        }

        /**
         * Deletes the reported discussion or comment from database and resolves the report.
         */
        @Transactional
        public void deleteReportedItem(Long reportId) {
                CommunityReport report = communityReportRepository.findById(reportId)
                                .orElseThrow(() -> ApiException.notFound("Report not found: " + reportId));

                Long targetId = report.getTargetId();
                String type = report.getReportType();

                if ("DISCUSSION".equalsIgnoreCase(type)) {
                        discussionRepository.findById(targetId).ifPresent(disc -> {
                                if (disc.getReplies() != null) {
                                        for (DiscussionReply rep : disc.getReplies()) {
                                                communityReportRepository.deleteByReportTypeAndTargetId("REPLY", rep.getId());
                                        }
                                }
                                discussionRepository.delete(disc);
                        });
                        communityReportRepository.deleteByReportTypeAndTargetId("DISCUSSION", targetId);
                } else if ("REPLY".equalsIgnoreCase(type)) {
                        discussionReplyRepository.findById(targetId).ifPresent(rep -> {
                                if (rep.getDiscussion() != null && rep.getDiscussion().getReplies() != null) {
                                        rep.getDiscussion().getReplies().remove(rep);
                                }
                                discussionReplyRepository.delete(rep);
                        });
                        communityReportRepository.deleteByReportTypeAndTargetId("REPLY", targetId);
                }
                if (communityReportRepository.existsById(reportId)) {
                        communityReportRepository.deleteById(reportId);
                }
        }

        /**
         * Permanently removes a discussion and all associated replies and reports from database.
         */
        @Transactional
        public void deleteDiscussion(Long id) {
                Discussion discussion = discussionRepository.findById(id)
                                .orElseThrow(() -> ApiException.notFound("Discussion not found: " + id));
                if (discussion.getReplies() != null) {
                        for (DiscussionReply rep : discussion.getReplies()) {
                                communityReportRepository.deleteByReportTypeAndTargetId("REPLY", rep.getId());
                        }
                }
                communityReportRepository.deleteByReportTypeAndTargetId("DISCUSSION", id);
                discussionRepository.delete(discussion);
        }

        /**
         * Aggregates telemetry summary metrics for the Admin Tracking Dashboard.
         */
        @Transactional(readOnly = true)
        public AdminDto.TrackingSummaryDto getTrackingSummary() {
                long totalLogins = userLoginLogRepository.count();
                long totalPageVisits = pageVisitLogRepository.count();

                Set<String> uniqueLocs = new HashSet<>();
                uniqueLocs.addAll(userLoginLogRepository.findDistinctLocations());
                uniqueLocs.addAll(pageVisitLogRepository.findDistinctLocations());
                long uniqueLocations = uniqueLocs.stream()
                                .filter(loc -> loc != null && !loc.trim().isBlank())
                                .count();

                List<PageVisitLog> allVisits = pageVisitLogRepository.findAll();
                Map<String, List<PageVisitLog>> grouped = allVisits.stream()
                                .collect(Collectors.groupingBy(PageVisitLog::getPageRoute));

                List<AdminDto.TopPageDto> topPages = grouped.entrySet().stream()
                                .map(entry -> {
                                        String route = entry.getKey();
                                        long visits = entry.getValue().size();
                                        String title = entry.getValue().stream()
                                                        .map(PageVisitLog::getPageTitle)
                                                        .filter(t -> t != null && !t.isBlank())
                                                        .findFirst()
                                                        .orElse(route);
                                        return AdminDto.TopPageDto.builder()
                                                        .pageRoute(route)
                                                        .pageTitle(title)
                                                        .visits(visits)
                                                        .build();
                                })
                                .sorted((a, b) -> Long.compare(b.getVisits(), a.getVisits()))
                                .limit(10)
                                .collect(Collectors.toList());

                String mostVisited = (!topPages.isEmpty()) ? topPages.get(0).getPageRoute() : "None";

                return AdminDto.TrackingSummaryDto.builder()
                                .totalLogins(totalLogins)
                                .uniqueLocations(uniqueLocations)
                                .totalPageVisits(totalPageVisits)
                                .mostVisitedPage(mostVisited)
                                .topPages(topPages)
                                .build();
        }

        /**
         * Retrieves user login history with optional search and method filtering.
         */
        @Transactional(readOnly = true)
        public List<AdminDto.UserLoginLogDto> getLoginLogs(String search, String method) {
                List<UserLoginLog> logs = userLoginLogRepository.findAllByOrderByLoggedInAtDesc();

                String searchLower = (search != null) ? search.trim().toLowerCase() : "";
                String methodFilter = (method != null) ? method.trim().toUpperCase() : "ALL";

                return logs.stream()
                                .filter(l -> {
                                        if (!"ALL".equals(methodFilter) && !methodFilter.equalsIgnoreCase(l.getLoginMethod())) {
                                                return false;
                                        }
                                        if (!searchLower.isEmpty()) {
                                                boolean matchUser = l.getUsername() != null && l.getUsername().toLowerCase().contains(searchLower);
                                                boolean matchEmail = l.getEmail() != null && l.getEmail().toLowerCase().contains(searchLower);
                                                boolean matchLoc = l.getLocation() != null && l.getLocation().toLowerCase().contains(searchLower);
                                                boolean matchIp = l.getIpAddress() != null && l.getIpAddress().toLowerCase().contains(searchLower);
                                                return matchUser || matchEmail || matchLoc || matchIp;
                                        }
                                        return true;
                                })
                                .limit(200)
                                .map(l -> AdminDto.UserLoginLogDto.builder()
                                                .id(l.getId())
                                                .userId(l.getUserId())
                                                .username(l.getUsername())
                                                .email(l.getEmail())
                                                .loginMethod(l.getLoginMethod())
                                                .ipAddress(l.getIpAddress())
                                                .location(l.getLocation())
                                                .userAgent(l.getUserAgent())
                                                .loggedInAt(l.getLoggedInAt())
                                                .build())
                                .collect(Collectors.toList());
        }

        /**
         * Retrieves page navigation logs with optional search and route filtering.
         */
        @Transactional(readOnly = true)
        public List<AdminDto.PageVisitLogDto> getPageVisitLogs(String search, String route) {
                List<PageVisitLog> logs = pageVisitLogRepository.findAllByOrderByVisitedAtDesc();

                String searchLower = (search != null) ? search.trim().toLowerCase() : "";
                String routeFilter = (route != null) ? route.trim().toLowerCase() : "all";

                return logs.stream()
                                .filter(l -> {
                                        if (!"all".equalsIgnoreCase(routeFilter) && !routeFilter.isBlank()) {
                                                String pageRoute = (l.getPageRoute() != null) ? l.getPageRoute().toLowerCase() : "";
                                                if (!pageRoute.equals(routeFilter) && !pageRoute.startsWith(routeFilter)) {
                                                        return false;
                                                }
                                        }
                                        if (!searchLower.isEmpty()) {
                                                boolean matchUser = l.getUsername() != null && l.getUsername().toLowerCase().contains(searchLower);
                                                boolean matchRoute = l.getPageRoute() != null && l.getPageRoute().toLowerCase().contains(searchLower);
                                                boolean matchTitle = l.getPageTitle() != null && l.getPageTitle().toLowerCase().contains(searchLower);
                                                boolean matchLoc = l.getLocation() != null && l.getLocation().toLowerCase().contains(searchLower);
                                                boolean matchIp = l.getIpAddress() != null && l.getIpAddress().toLowerCase().contains(searchLower);
                                                return matchUser || matchRoute || matchTitle || matchLoc || matchIp;
                                        }
                                        return true;
                                })
                                .limit(200)
                                .map(l -> AdminDto.PageVisitLogDto.builder()
                                                .id(l.getId())
                                                .userId(l.getUserId())
                                                .username(l.getUsername())
                                                .pageRoute(l.getPageRoute())
                                                .pageTitle(l.getPageTitle())
                                                .ipAddress(l.getIpAddress())
                                                .location(l.getLocation())
                                                .visitedAt(l.getVisitedAt())
                                                .build())
                                .collect(Collectors.toList());
        }
}