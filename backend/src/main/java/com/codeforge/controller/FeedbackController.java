package com.codeforge.controller;

import com.codeforge.dto.FeedbackDto;
import com.codeforge.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller managing user feedback submissions.
 *
 * <p>Access requires user authentication. If unauthenticated, returns 401 Unauthorized.</p>
 */
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Submits user feedback, saves it to the database, and dispatches a confirmation email to the user.
     */
    @PostMapping
    public ResponseEntity<FeedbackDto.Response> submitFeedback(
            @Valid @RequestBody FeedbackDto.Request request,
            Authentication authentication) {
        String principal = authentication != null ? authentication.getName() : null;
        FeedbackDto.Response response = feedbackService.submitFeedback(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
