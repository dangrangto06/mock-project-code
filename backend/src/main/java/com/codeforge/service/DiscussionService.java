package com.codeforge.service;

import com.codeforge.dto.DiscussionDto;
import com.codeforge.entity.Discussion;
import com.codeforge.entity.DiscussionReply;
import com.codeforge.entity.DiscussionUpvote;
import com.codeforge.entity.User;
import com.codeforge.exception.ApiException;
import com.codeforge.repository.DiscussionReplyRepository;
import com.codeforge.repository.DiscussionRepository;
import com.codeforge.repository.DiscussionUpvoteRepository;
import com.codeforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing community discussions, threads, upvoting mechanics, and replies.
 */
@Service
@RequiredArgsConstructor
public class DiscussionService {

    private final DiscussionRepository discussionRepository;
    private final DiscussionReplyRepository discussionReplyRepository;
    private final DiscussionUpvoteRepository discussionUpvoteRepository;
    private final UserRepository userRepository;

    private User resolveUser(String username) {
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            throw ApiException.unauthorized("Authentication required");
        }
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> ApiException.unauthorized("User account not found"));
    }

    private User resolveUserOptional(String username) {
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            return null;
        }
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElse(null);
    }

    /**
     * Retrieves all community discussions, optionally filtered by category.
     */
    @Transactional(readOnly = true)
    public List<DiscussionDto.Response> getAllDiscussions(String category, String username) {
        User currentUser = resolveUserOptional(username);

        List<Discussion> discussions;
        if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            discussions = discussionRepository.findByCategoryOrderByCreatedAtDesc(category);
        } else {
            discussions = discussionRepository.findAllByOrderByCreatedAtDesc();
        }

        return discussions.stream()
                .filter(d -> {
                    boolean isApproved = d.getModerationStatus() == null || "APPROVED".equalsIgnoreCase(d.getModerationStatus());
                    if (isApproved) {
                        return true;
                    }
                    // Non-approved posts (PENDING / REJECTED) are only visible to the author
                    return currentUser != null && d.getAuthor() != null && d.getAuthor().getId().equals(currentUser.getId());
                })
                .map(d -> toDto(d, currentUser))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single discussion topic by ID.
     */
    @Transactional(readOnly = true)
    public DiscussionDto.Response getDiscussionById(Long id, String username) {
        User currentUser = resolveUserOptional(username);
        Discussion discussion = discussionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Discussion not found: " + id));

        return toDto(discussion, currentUser);
    }

    /**
     * Creates a new community discussion topic authored by the current user.
     */
    @Transactional
    public DiscussionDto.Response createDiscussion(DiscussionDto.CreateDiscussionRequest request, String username) {
        User author = resolveUser(username);
        boolean isAdmin = author.getRole() != null && "ADMIN".equalsIgnoreCase(author.getRole());
        String modStatus = isAdmin ? "APPROVED" : "PENDING";

        Discussion discussion = Discussion.builder()
                .author(author)
                .title(request.getTitle())
                .category(request.getCategory())
                .problemId(request.getProblemId())
                .problemTitle(request.getProblemTitle())
                .content(request.getContent())
                .upvotes(1)
                .moderationStatus(modStatus)
                .createdAt(LocalDateTime.now())
                .build();

        discussion = discussionRepository.save(discussion);

        // Record initial author upvote
        DiscussionUpvote initialUpvote = DiscussionUpvote.builder()
                .discussion(discussion)
                .user(author)
                .createdAt(LocalDateTime.now())
                .build();
        discussionUpvoteRepository.save(initialUpvote);

        return toDto(discussion, author);
    }

    /**
     * Toggles an upvote on a discussion thread.
     */
    @Transactional
    public Map<String, Object> toggleUpvote(Long id, String username) {
        User user = resolveUser(username);
        Discussion discussion = discussionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Discussion not found: " + id));

        Optional<DiscussionUpvote> existing = discussionUpvoteRepository.findByDiscussionIdAndUserId(id, user.getId());

        boolean isUpvoted;
        if (existing.isPresent()) {
            discussionUpvoteRepository.delete(existing.get());
            discussion.setUpvotes(Math.max(0, discussion.getUpvotes() - 1));
            isUpvoted = false;
        } else {
            DiscussionUpvote upvote = DiscussionUpvote.builder()
                    .discussion(discussion)
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .build();
            discussionUpvoteRepository.save(upvote);
            discussion.setUpvotes(discussion.getUpvotes() + 1);
            isUpvoted = true;
        }

        discussionRepository.save(discussion);
        return Map.of("upvotes", discussion.getUpvotes(), "isUpvoted", isUpvoted);
    }

    /**
     * Submits a reply to an existing discussion thread.
     */
    @Transactional
    public DiscussionDto.ReplyResponse addReply(Long discussionId, DiscussionDto.CreateReplyRequest request, String username) {
        User author = resolveUser(username);
        Discussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> ApiException.notFound("Discussion not found: " + discussionId));

        DiscussionReply reply = DiscussionReply.builder()
                .discussion(discussion)
                .author(author)
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        reply = discussionReplyRepository.save(reply);
        return toReplyDto(reply);
    }

    private DiscussionDto.Response toDto(Discussion d, User currentUser) {
        boolean isUpvoted = false;
        if (currentUser != null) {
            isUpvoted = discussionUpvoteRepository.existsByDiscussionIdAndUserId(d.getId(), currentUser.getId());
        }

        List<DiscussionReply> replies = d.getReplies();
        List<DiscussionDto.ReplyResponse> replyDtos = (replies != null ? replies : List.<DiscussionReply>of())
                .stream()
                .map(this::toReplyDto)
                .collect(Collectors.toList());

        String authorName = d.getAuthor() != null ? d.getAuthor().getFullName() : "Anonymous";
        String authorRole = d.getAuthor() != null ? d.getAuthor().getRole() : "Learner";

        return DiscussionDto.Response.builder()
                .id(d.getId())
                .title(d.getTitle())
                .authorName(authorName)
                .authorInitials(DiscussionDto.getInitials(authorName))
                .authorRole(authorRole)
                .category(d.getCategory())
                .problemId(d.getProblemId())
                .problemTitle(d.getProblemTitle())
                .content(d.getContent())
                .upvotes(d.getUpvotes())
                .isUpvoted(isUpvoted)
                .repliesCount(replyDtos.size())
                .createdAt(DiscussionDto.formatDate(d.getCreatedAt()))
                .moderationStatus(d.getModerationStatus() != null ? d.getModerationStatus() : "APPROVED")
                .rejectionReason(d.getRejectionReason())
                .replies(replyDtos)
                .build();
    }

    private DiscussionDto.ReplyResponse toReplyDto(DiscussionReply r) {
        String authorName = r.getAuthor() != null ? r.getAuthor().getFullName() : "Anonymous";
        String authorRole = r.getAuthor() != null ? r.getAuthor().getRole() : "Learner";

        return DiscussionDto.ReplyResponse.builder()
                .id(r.getId())
                .authorName(authorName)
                .authorInitials(DiscussionDto.getInitials(authorName))
                .authorRole(authorRole)
                .content(r.getContent())
                .createdAt(DiscussionDto.formatDate(r.getCreatedAt()))
                .build();
    }
}

