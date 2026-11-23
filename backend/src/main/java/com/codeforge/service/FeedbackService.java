package com.codeforge.service;

import com.codeforge.dto.FeedbackDto;
import com.codeforge.entity.Feedback;
import com.codeforge.entity.User;
import com.codeforge.exception.ApiException;
import com.codeforge.repository.FeedbackRepository;
import com.codeforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing user feedback lifecycle, database persistence, and email notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Resolves authenticated user entity from the Spring Security principal.
     */
    private User resolveUser(String principal) {
        if (principal == null || principal.isBlank() || "anonymousUser".equals(principal)) {
            throw ApiException.unauthorized("Authentication required. Please sign in to submit feedback.");
        }
        return userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> ApiException.unauthorized("User account not found"));
    }

    /**
     * Records new user feedback, persists it to the database, and dispatches a confirmation email.
     */
    @Transactional
    public FeedbackDto.Response submitFeedback(FeedbackDto.Request request, String principal) {
        User user = resolveUser(principal);

        String authorName = (request.getName() != null && !request.getName().isBlank())
                ? request.getName().trim()
                : (user.getFullName() != null ? user.getFullName() : user.getUsername());

        // Always tie the feedback email to the logged in user's verified account email
        String authorEmail = (user.getEmail() != null && !user.getEmail().isBlank())
                ? user.getEmail().trim()
                : request.getEmail().trim();

        Feedback feedback = Feedback.builder()
                .user(user)
                .name(authorName)
                .email(authorEmail)
                .subject(request.getSubject() != null && !request.getSubject().isBlank() ? request.getSubject().trim() : "General Feedback")
                .message(request.getMessage().trim())
                .status("NEW")
                .build();

        Feedback saved = feedbackRepository.save(feedback);
        log.info("Persisted feedback ID {} from user '{}' ({})", saved.getId(), user.getUsername(), saved.getEmail());

        // Trigger confirmation email
        try {
            emailService.sendFeedbackConfirmationEmail(saved.getEmail(), saved.getName(), saved.getMessage());
        } catch (Exception e) {
            log.warn("Failed to dispatch feedback confirmation email: {}", e.getMessage());
        }

        return toDto(saved);
    }

    /**
     * Retrieves all user feedback submissions ordered by creation date descending.
     */
    @Transactional(readOnly = true)
    public List<FeedbackDto.Response> getAllFeedbacks() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Updates moderation status of a feedback item (e.g. NEW -> REVIEWED / RESOLVED).
     */
    @Transactional
    public FeedbackDto.Response updateFeedbackStatus(Long id, String status) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "Feedback not found with ID: " + id));
        feedback.setStatus(status.toUpperCase());
        return toDto(feedbackRepository.save(feedback));
    }

    /**
     * Deletes a feedback item permanently.
     */
    @Transactional
    public void deleteFeedback(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "Feedback not found with ID: " + id));
        feedbackRepository.delete(feedback);
        log.info("Deleted feedback ID {}", id);
    }

    /**
     * Returns total count of feedbacks.
     */
    @Transactional(readOnly = true)
    public long countFeedbacks() {
        return feedbackRepository.count();
    }

    private FeedbackDto.Response toDto(Feedback feedback) {
        return FeedbackDto.Response.builder()
                .id(feedback.getId())
                .userId(feedback.getUser() != null ? feedback.getUser().getId() : null)
                .username(feedback.getUser() != null ? feedback.getUser().getUsername() : null)
                .name(feedback.getName())
                .email(feedback.getEmail())
                .subject(feedback.getSubject())
                .message(feedback.getMessage())
                .status(feedback.getStatus())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
