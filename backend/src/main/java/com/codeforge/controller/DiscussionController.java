package com.codeforge.controller;

import com.codeforge.dto.DiscussionDto;
import com.codeforge.service.DiscussionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Community Discussions, upvotes, and replies.
 */
@RestController
@RequestMapping("/api/discussions")
@RequiredArgsConstructor
public class DiscussionController {

    private final DiscussionService discussionService;

    /**
     * Public endpoint to retrieve community discussions.
     */
    @GetMapping
    public ResponseEntity<List<DiscussionDto.Response>> getDiscussions(
            @RequestParam(required = false) String category,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(discussionService.getAllDiscussions(category, username));
    }

    /**
     * Public endpoint to retrieve a single discussion topic.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DiscussionDto.Response> getDiscussionById(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(discussionService.getDiscussionById(id, username));
    }

    /**
     * Authenticated endpoint to create a new discussion thread.
     */
    @PostMapping
    public ResponseEntity<DiscussionDto.Response> createDiscussion(
            @Valid @RequestBody DiscussionDto.CreateDiscussionRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        DiscussionDto.Response created = discussionService.createDiscussion(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Authenticated endpoint to toggle an upvote on a discussion.
     */
    @PostMapping("/{id}/upvote")
    public ResponseEntity<Map<String, Object>> toggleUpvote(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(discussionService.toggleUpvote(id, username));
    }

    /**
     * Authenticated endpoint to submit a reply to a discussion thread.
     */
    @PostMapping("/{id}/replies")
    public ResponseEntity<DiscussionDto.ReplyResponse> addReply(
            @PathVariable Long id,
            @Valid @RequestBody DiscussionDto.CreateReplyRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        DiscussionDto.ReplyResponse reply = discussionService.addReply(id, request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(reply);
    }

    /**
     * Authenticated endpoint to report a discussion or reply.
     */
    @PostMapping("/reports")
    public ResponseEntity<Map<String, Object>> reportContent(
            @Valid @RequestBody DiscussionDto.CreateReportRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(discussionService.reportContent(request, username));
    }
}

