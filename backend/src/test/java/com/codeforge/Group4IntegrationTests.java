package com.codeforge;

import com.codeforge.config.JwtUtil;
import com.codeforge.dto.DiscussionDto;
import com.codeforge.dto.NoteDto;
import com.codeforge.entity.Discussion;
import com.codeforge.entity.Note;
import com.codeforge.entity.User;
import com.codeforge.repository.DiscussionReplyRepository;
import com.codeforge.repository.DiscussionRepository;
import com.codeforge.repository.NoteRepository;
import com.codeforge.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class Group4IntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private DiscussionRepository discussionRepository;

    @Autowired
    private DiscussionReplyRepository discussionReplyRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User userAlex;
    private User userBob;
    private String tokenAlex;
    private String tokenBob;

    @BeforeEach
    void setUp() {
        userAlex = userRepository.findByEmail("alex@codeforge.io").orElseGet(() -> {
            User alex = User.builder()
                    .username("alex_morgan")
                    .email("alex@codeforge.io")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .fullName("Alex Morgan")
                    .role("Admin")
                    .streakDays(7)
                    .solvedCount(48)
                    .acceptanceRate(new BigDecimal("76.20"))
                    .practiceTimeHours(new BigDecimal("12.50"))
                    .createdAt(LocalDateTime.now())
                    .build();
            return userRepository.save(alex);
        });

        userBob = userRepository.findByEmail("bob@codeforge.io").orElseGet(() -> {
            User bob = User.builder()
                    .username("bob_learner")
                    .email("bob@codeforge.io")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .fullName("Bob Learner")
                    .role("Learner")
                    .streakDays(1)
                    .solvedCount(5)
                    .acceptanceRate(new BigDecimal("50.00"))
                    .practiceTimeHours(new BigDecimal("2.00"))
                    .createdAt(LocalDateTime.now())
                    .build();
            return userRepository.save(bob);
        });

        tokenAlex = jwtUtil.generateToken(userAlex.getUsername());
        tokenBob = jwtUtil.generateToken(userBob.getUsername());
    }

    // ── Task 1: Learner Notes Tests ──────────────────────────────────────────

    @Test
    @DisplayName("Task 1: GET /api/notes without authentication returns 401 or 403")
    void testGetNotes_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/notes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Task 1: GET /api/notes returns user's notes ordered by updatedAt descending")
    void testGetNotes_Authenticated() throws Exception {
        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + tokenAlex))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("Task 1: POST /api/notes creates note for current user and returns DTO")
    void testCreateNote() throws Exception {
        NoteDto.Request request = NoteDto.Request.builder()
                .title("Sliding Window Template")
                .problemId(104L)
                .problemTitle("Pair Sum")
                .tags(List.of("Sliding Window", "Arrays"))
                .content("Maintain left and right pointer. Expand right until valid, contract left while invalid.")
                .build();

        mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Sliding Window Template"))
                .andExpect(jsonPath("$.tags", hasItem("Sliding Window")))
                .andExpect(jsonPath("$.problemId").value(104));

        List<Note> bobNotes = noteRepository.findByUserIdOrderByUpdatedAtDesc(userBob.getId());
        assertEquals(1, bobNotes.size());
        assertEquals("Sliding Window Template", bobNotes.get(0).getTitle());
    }

    @Test
    @DisplayName("Task 1: PUT /api/notes/{id} updates note when caller is owner")
    void testUpdateNote_OwnerSuccess() throws Exception {
        Note note = Note.builder()
                .user(userBob)
                .title("Original Title")
                .content("Original content")
                .tags("Original")
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
        note = noteRepository.save(note);

        NoteDto.Request updateReq = NoteDto.Request.builder()
                .title("Updated Title")
                .content("Updated content")
                .tags(List.of("Updated", "Tag2"))
                .build();

        mockMvc.perform(put("/api/notes/" + note.getId())
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.tags", hasItem("Tag2")));

        Note reloaded = noteRepository.findById(note.getId()).orElseThrow();
        assertEquals("Updated Title", reloaded.getTitle());
    }

    @Test
    @DisplayName("Task 1: PUT /api/notes/{id} fails with 403 Forbidden if non-owner attempts update")
    void testUpdateNote_NonOwnerForbidden() throws Exception {
        Note note = Note.builder()
                .user(userAlex)
                .title("Alex Note")
                .content("Alex content")
                .updatedAt(LocalDateTime.now())
                .build();
        note = noteRepository.save(note);

        NoteDto.Request updateReq = NoteDto.Request.builder()
                .title("Hacked Title")
                .build();

        mockMvc.perform(put("/api/notes/" + note.getId())
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Task 1: DELETE /api/notes/{id} deletes note when caller is owner and rejects non-owner")
    void testDeleteNote_OwnerAndNonOwner() throws Exception {
        Note note = Note.builder()
                .user(userBob)
                .title("To be deleted")
                .content("Content")
                .updatedAt(LocalDateTime.now())
                .build();
        note = noteRepository.save(note);

        // Non-owner attempts delete -> 403
        mockMvc.perform(delete("/api/notes/" + note.getId())
                        .header("Authorization", "Bearer " + tokenAlex))
                .andExpect(status().isForbidden());

        // Owner deletes -> 200
        mockMvc.perform(delete("/api/notes/" + note.getId())
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isOk());

        assertFalse(noteRepository.existsById(note.getId()));
    }

    // ── Task 2: Community Discussions Tests ──────────────────────────────────

    @Test
    @DisplayName("Task 2: GET /api/discussions is public (permitAll) and returns discussion threads")
    void testGetDiscussions_Public() throws Exception {
        mockMvc.perform(get("/api/discussions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[0].authorName").isString())
                .andExpect(jsonPath("$[0].authorInitials").isString());
    }

    @Test
    @DisplayName("Task 2: GET /api/discussions?category=Solutions filters correctly")
    void testGetDiscussions_FilterCategory() throws Exception {
        mockMvc.perform(get("/api/discussions?category=Solutions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].category").value("Solutions"));
    }

    @Test
    @DisplayName("Task 2: POST /api/discussions creates new discussion and grants author upvote")
    void testCreateDiscussion() throws Exception {
        DiscussionDto.CreateDiscussionRequest request = DiscussionDto.CreateDiscussionRequest.builder()
                .title("How to effectively solve Monotonic Stack problems?")
                .category("Questions")
                .problemId(104L)
                .problemTitle("Pair Sum")
                .content("I keep getting stuck on whether to maintain an increasing or decreasing stack.")
                .build();

        mockMvc.perform(post("/api/discussions")
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("How to effectively solve Monotonic Stack problems?"))
                .andExpect(jsonPath("$.authorName").value("Bob Learner"))
                .andExpect(jsonPath("$.upvotes").value(1))
                .andExpect(jsonPath("$.isUpvoted").value(true));
    }

    @Test
    @DisplayName("Task 2: POST /api/discussions/{id}/upvote toggles upvote status")
    void testToggleDiscussionUpvote() throws Exception {
        List<Discussion> all = discussionRepository.findAll();
        assertFalse(all.isEmpty());
        Long discussionId = all.get(0).getId();
        int initialVotes = all.get(0).getUpvotes();

        // 1. Bob upvotes -> upvotes incremented, isUpvoted = true
        mockMvc.perform(post("/api/discussions/" + discussionId + "/upvote")
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isUpvoted").value(true))
                .andExpect(jsonPath("$.upvotes").value(initialVotes + 1));

        // 2. Bob upvotes again -> toggle off: upvotes decremented, isUpvoted = false
        mockMvc.perform(post("/api/discussions/" + discussionId + "/upvote")
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isUpvoted").value(false))
                .andExpect(jsonPath("$.upvotes").value(initialVotes));
    }

    @Test
    @DisplayName("Task 2: POST /api/discussions/{id}/replies adds reply to thread")
    void testAddDiscussionReply() throws Exception {
        List<Discussion> all = discussionRepository.findAll();
        assertFalse(all.isEmpty());
        Long discussionId = all.get(0).getId();

        DiscussionDto.CreateReplyRequest replyReq = DiscussionDto.CreateReplyRequest.builder()
                .content("Thanks for sharing this, really clear explanation!")
                .build();

        mockMvc.perform(post("/api/discussions/" + discussionId + "/replies")
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.authorName").value("Bob Learner"))
                .andExpect(jsonPath("$.content").value("Thanks for sharing this, really clear explanation!"));
    }
}

