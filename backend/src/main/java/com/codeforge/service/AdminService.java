package com.codeforge.service;

import com.codeforge.dto.AdminDto;
import com.codeforge.entity.Discussion;
import com.codeforge.entity.Exercise;
import com.codeforge.entity.User;
import com.codeforge.exception.ApiException;
import com.codeforge.repository.DiscussionRepository;
import com.codeforge.repository.ExerciseRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing administrative operations, moderation queue decisions, and user roles.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final DiscussionRepository discussionRepository;
    private final SubmissionRepository submissionRepository;

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

        return AdminDto.StatsResponse.builder()
                .totalUsers(totalUsers)
                .totalProblems(totalProblems)
                .totalDiscussions(totalDiscussions)
                .totalSubmissions(totalSubmissions)
                .pendingProblemsCount(pendingProblemsCount)
                .pendingDiscussionsCount(pendingDiscussionsCount)
                .build();
    }

    /**
     * Retrieves all items currently awaiting moderator review.
     */
    @Transactional(readOnly = true)
    public AdminDto.ModerationQueueResponse getModerationQueue() {
        List<Exercise> pendingProblems = exerciseRepository.findByModerationStatusOrderByCreatedAtDesc("PENDING");
        List<Discussion> pendingDiscussions = discussionRepository.findByModerationStatusOrderByCreatedAtDesc("PENDING");

        List<AdminDto.PendingProblemDto> problemDtos = pendingProblems.stream()
                .map(this::toPendingProblemDto)
                .collect(Collectors.toList());

        List<AdminDto.PendingDiscussionDto> discussionDtos = pendingDiscussions.stream()
                .map(this::toPendingDiscussionDto)
                .collect(Collectors.toList());

        return AdminDto.ModerationQueueResponse.builder()
                .pendingProblems(problemDtos)
                .pendingDiscussions(discussionDtos)
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
}

