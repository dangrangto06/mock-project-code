package com.codeforge;

import com.codeforge.config.JwtUtil;
import com.codeforge.dto.AdminDto;
import com.codeforge.dto.DiscussionDto;
import com.codeforge.dto.ExerciseDto;
import com.codeforge.entity.Discussion;
import com.codeforge.entity.Exercise;
import com.codeforge.entity.User;
import com.codeforge.repository.DiscussionRepository;
import com.codeforge.repository.ExerciseRepository;
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
import org.springframework.test.web.servlet.MvcResult;
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
class Group5IntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private DiscussionRepository discussionRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminAlex;
    private User learnerBob;
    private String adminToken;
    private String learnerToken;

    @BeforeEach
    void setUp() {
        adminAlex = userRepository.findByEmail("alex_admin_g5@codeforge.io").orElseGet(() -> {
            User admin = User.builder()
                    .username("alex_admin_g5")
                    .email("alex_admin_g5@codeforge.io")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .fullName("Alex Admin")
                    .role("Admin")
                    .streakDays(10)
                    .solvedCount(50)
                    .acceptanceRate(new BigDecimal("80.00"))
                    .practiceTimeHours(new BigDecimal("15.00"))
                    .createdAt(LocalDateTime.now())
                    .build();
            return userRepository.save(admin);
        });

        learnerBob = userRepository.findByEmail("bob_learner_g5@codeforge.io").orElseGet(() -> {
            User bob = User.builder()
                    .username("bob_learner_g5")
                    .email("bob_learner_g5@codeforge.io")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .fullName("Bob Learner")
                    .role("Learner")
                    .streakDays(2)
                    .solvedCount(5)
                    .acceptanceRate(new BigDecimal("50.00"))
                    .practiceTimeHours(new BigDecimal("3.00"))
                    .createdAt(LocalDateTime.now())
                    .build();
            return userRepository.save(bob);
        });

        adminToken = jwtUtil.generateToken(adminAlex.getUsername());
        learnerToken = jwtUtil.generateToken(learnerBob.getUsername());
    }

    // ── Task 1: Security Role Tests ──────────────────────────────────────────

    @Test
    @DisplayName("Admin Security: Unauthenticated request to /api/admin/stats returns 401 or 403")
    void testAdminSecurity_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin Security: Learner role receives 403 Forbidden on /api/admin/stats")
    void testAdminSecurity_LearnerForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin Security: Admin role receives 200 OK on /api/admin/stats with valid KPI data")
    void testAdminSecurity_AdminAllowed() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.totalProblems", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.totalDiscussions", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.pendingProblemsCount", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.pendingDiscussionsCount", greaterThanOrEqualTo(0)));
    }

    // ── Task 2: Problem Moderation Workflow & Visibility ─────────────────────

    @Test
    @DisplayName("Problem Moderation: Learner creation sets status PENDING")
    void testProblemCreation_LearnerPending() throws Exception {
        ExerciseDto.CreateExerciseRequest request = ExerciseDto.CreateExerciseRequest.builder()
                .title("Learner Two Sum")
                .statement("Find two numbers that sum up to target.")
                .difficulty("Easy")
                .topics(List.of("Arrays", "Hash Table"))
                .language("Python 3")
                .build();

        MvcResult result = mockMvc.perform(post("/api/problems")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Learner Two Sum")))
                .andExpect(jsonPath("$.moderationStatus", is("PENDING")))
                .andReturn();

        ExerciseDto created = objectMapper.readValue(result.getResponse().getContentAsString(), ExerciseDto.class);
        assertNotNull(created.getId());
        assertEquals("PENDING", created.getModerationStatus());
    }

    @Test
    @DisplayName("Problem Moderation: Admin creation sets status APPROVED immediately")
    void testProblemCreation_AdminApproved() throws Exception {
        ExerciseDto.CreateExerciseRequest request = ExerciseDto.CreateExerciseRequest.builder()
                .title("Admin Binary Search")
                .statement("Find target index in sorted array.")
                .difficulty("Medium")
                .topics(List.of("Binary Search", "Algorithms"))
                .language("Python 3")
                .build();

        mockMvc.perform(post("/api/problems")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Admin Binary Search")))
                .andExpect(jsonPath("$.moderationStatus", is("APPROVED")));
    }

    @Test
    @DisplayName("Problem Visibility: PENDING problem is excluded from 'all' tab but included in author's 'my' tab")
    void testProblemVisibility_PendingFilters() throws Exception {
        long nextId = exerciseRepository.findMaxId() + 1;
        Exercise exercise = Exercise.builder()
                .id(nextId)
                .title("Pending Mystery Problem")
                .description("Some statement")
                .difficulty("Hard")
                .topics("Graph")
                .author(learnerBob)
                .isCustom(true)
                .moderationStatus("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        Exercise saved = exerciseRepository.save(exercise);

        // Public / anonymous -> must NOT contain this pending problem
        mockMvc.perform(get("/api/problems?tab=all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + saved.getId() + ")]", hasSize(0)));

        // Other user (Admin Alex) -> must NOT contain Bob's pending problem in 'all' tab
        mockMvc.perform(get("/api/problems?tab=all")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + saved.getId() + ")]", hasSize(0)));

        // Author (Bob) calling 'all' tab -> DOES receive own pending problem so it persists across refreshes
        mockMvc.perform(get("/api/problems?tab=all")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + saved.getId() + ")].moderationStatus", contains("PENDING")));

        // Learner's own 'my' tab -> MUST contain this pending problem with moderationStatus
        mockMvc.perform(get("/api/problems?tab=my")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + saved.getId() + ")].moderationStatus", contains("PENDING")));
    }

    @Test
    @DisplayName("Admin Moderation Queue: Approving problem publishes it to public catalog")
    void testAdminModeration_ApproveProblem() throws Exception {
        long nextId = exerciseRepository.findMaxId() + 1;
        Exercise pendingEx = Exercise.builder()
                .id(nextId)
                .title("Algorithm for Approval")
                .description("Statement waiting for admin.")
                .difficulty("Easy")
                .topics("Math")
                .author(learnerBob)
                .isCustom(true)
                .moderationStatus("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        Exercise saved = exerciseRepository.save(pendingEx);

        // Verify it appears in admin moderation queue
        mockMvc.perform(get("/api/admin/moderation")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingProblems[?(@.id == " + saved.getId() + ")]", hasSize(1)));

        // Admin approves it
        mockMvc.perform(post("/api/admin/problems/" + saved.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("approved")));

        // Verify status in DB
        Exercise updated = exerciseRepository.findById(saved.getId()).orElseThrow();
        assertEquals("APPROVED", updated.getModerationStatus());
        assertNull(updated.getRejectionReason());

        // Now public 'all' tab includes it
        mockMvc.perform(get("/api/problems?tab=all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + saved.getId() + ")]", hasSize(1)));
    }

    @Test
    @DisplayName("Admin Moderation Queue: Rejecting problem sets status REJECTED and saves reason")
    void testAdminModeration_RejectProblem() throws Exception {
        long nextId = exerciseRepository.findMaxId() + 1;
        Exercise pendingEx = Exercise.builder()
                .id(nextId)
                .title("Flawed Submission")
                .description("Statement with bad edge cases.")
                .difficulty("Medium")
                .topics("Strings")
                .author(learnerBob)
                .isCustom(true)
                .moderationStatus("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        Exercise saved = exerciseRepository.save(pendingEx);

        AdminDto.RejectRequest rejectReq = new AdminDto.RejectRequest("Incomplete test cases provided.");

        mockMvc.perform(post("/api/admin/problems/" + saved.getId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("rejected")));

        Exercise updated = exerciseRepository.findById(saved.getId()).orElseThrow();
        assertEquals("REJECTED", updated.getModerationStatus());
        assertEquals("Incomplete test cases provided.", updated.getRejectionReason());

        // Learner sees REJECTED in 'my' tab with rejection reason
        mockMvc.perform(get("/api/problems?tab=my")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + saved.getId() + ")].moderationStatus", contains("REJECTED")))
                .andExpect(jsonPath("$[?(@.id == " + saved.getId() + ")].rejectionReason", contains("Incomplete test cases provided.")));
    }

    // ── Task 3: Community Discussions Moderation & Visibility ───────────────

    @Test
    @DisplayName("Discussion Moderation: Learner post is PENDING; Admin post is APPROVED")
    void testDiscussionCreation_RolesModeration() throws Exception {
        // Learner post
        DiscussionDto.CreateDiscussionRequest learnerReq = DiscussionDto.CreateDiscussionRequest.builder()
                .title("How to optimize DFS?")
                .category("Questions")
                .content("I am getting TLE on large graphs.")
                .build();

        mockMvc.perform(post("/api/discussions")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(learnerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moderationStatus", is("PENDING")));

        // Admin post
        DiscussionDto.CreateDiscussionRequest adminReq = DiscussionDto.CreateDiscussionRequest.builder()
                .title("Official CodeForge Guide")
                .category("Tips")
                .content("Best practices for competitive programming.")
                .build();

        mockMvc.perform(post("/api/discussions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moderationStatus", is("APPROVED")));
    }

    @Test
    @DisplayName("Discussion Visibility: PENDING post is hidden from other users but visible to author")
    void testDiscussionVisibility_AuthorOnlyPending() throws Exception {
        Discussion pendingDisc = Discussion.builder()
                .title("Pending Learner Question")
                .category("Questions")
                .content("Waiting for review.")
                .author(learnerBob)
                .moderationStatus("PENDING")
                .upvotes(0)
                .createdAt(LocalDateTime.now())
                .build();
        Discussion saved = discussionRepository.save(pendingDisc);

        // Other user (Admin Alex) calling GET /api/discussions -> does NOT see Bob's pending post
        mockMvc.perform(get("/api/discussions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + saved.getId() + ")]", hasSize(0)));

        // Unauthenticated request -> does NOT see Bob's pending post
        mockMvc.perform(get("/api/discussions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + saved.getId() + ")]", hasSize(0)));

        // Author (Learner Bob) calling GET /api/discussions -> DOES see own pending post
        mockMvc.perform(get("/api/discussions")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + saved.getId() + ")].moderationStatus", contains("PENDING")));
    }

    @Test
    @DisplayName("Admin Moderation Queue: Approving discussion publishes it to everyone")
    void testAdminModeration_ApproveDiscussion() throws Exception {
        Discussion pendingDisc = Discussion.builder()
                .title("Great solution for Two Sum")
                .category("Solutions")
                .content("Here is a clean one-pass hash map approach.")
                .author(learnerBob)
                .moderationStatus("PENDING")
                .upvotes(0)
                .createdAt(LocalDateTime.now())
                .build();
        Discussion saved = discussionRepository.save(pendingDisc);

        // Admin approves it
        mockMvc.perform(post("/api/admin/discussions/" + saved.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("approved")));

        Discussion updated = discussionRepository.findById(saved.getId()).orElseThrow();
        assertEquals("APPROVED", updated.getModerationStatus());

        // Now Admin (and everyone) sees it in public discussions
        mockMvc.perform(get("/api/discussions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + saved.getId() + ")]", hasSize(1)));
    }

    @Test
    @DisplayName("Admin Moderation Queue: Rejecting discussion sets status REJECTED and saves reason")
    void testAdminModeration_RejectDiscussion() throws Exception {
        Discussion pendingDisc = Discussion.builder()
                .title("Spam or promotional post")
                .category("Tips")
                .content("Visit my external website.")
                .author(learnerBob)
                .moderationStatus("PENDING")
                .upvotes(0)
                .createdAt(LocalDateTime.now())
                .build();
        Discussion saved = discussionRepository.save(pendingDisc);

        AdminDto.RejectRequest rejectReq = new AdminDto.RejectRequest("Promotional links are not allowed.");

        mockMvc.perform(post("/api/admin/discussions/" + saved.getId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("rejected")));

        Discussion updated = discussionRepository.findById(saved.getId()).orElseThrow();
        assertEquals("REJECTED", updated.getModerationStatus());
        assertEquals("Promotional links are not allowed.", updated.getRejectionReason());
    }

    // ── Task 4: User Directory & Role Promotion/Demotion ────────────────────

    @Test
    @DisplayName("Admin User Management: GET /api/admin/users and PUT /api/admin/users/{id}/role")
    void testAdminUserManagement() throws Exception {
        // GET users
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(List.class)))
                .andExpect(jsonPath("$[?(@.id == " + learnerBob.getId() + ")].role", contains("Learner")));

        // Promote Bob to Admin
        AdminDto.UpdateRoleRequest updateReq = new AdminDto.UpdateRoleRequest("Admin");

        mockMvc.perform(put("/api/admin/users/" + learnerBob.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("Admin")));

        User bobInDb = userRepository.findById(learnerBob.getId()).orElseThrow();
        assertEquals("Admin", bobInDb.getRole());

        // Demote back to Learner
        updateReq.setRole("Learner");
        mockMvc.perform(put("/api/admin/users/" + learnerBob.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("Learner")));

        User bobInDbAfter = userRepository.findById(learnerBob.getId()).orElseThrow();
        assertEquals("Learner", bobInDbAfter.getRole());
    }

    // ── Task 5: Smart Test Runner & User Submissions Endpoint ───────────────

    @Test
    @DisplayName("Execution Engine: Smart sandbox rejects gibberish & empty stubs, accepts valid solutions, and lists user submissions")
    void testSmartExecutionAndUserSubmissions() throws Exception {
        // 1. Gibberish syntax -> Runtime Error
        com.codeforge.dto.ExecutionDto.RunRequest badSyntaxReq = com.codeforge.dto.ExecutionDto.RunRequest.builder()
                .problemId(104L)
                .language("python")
                .code("def solve(nums, target):\n    passasd")
                .build();

        mockMvc.perform(post("/api/execute/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badSyntaxReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is("Runtime Error")))
                .andExpect(jsonPath("$.results", hasSize(greaterThanOrEqualTo(2))));

        // 2. Unmodified pass stub -> Wrong Answer
        com.codeforge.dto.ExecutionDto.RunRequest stubReq = com.codeforge.dto.ExecutionDto.RunRequest.builder()
                .problemId(104L)
                .language("python")
                .code("def solve(nums, target):\n    pass")
                .build();

        mockMvc.perform(post("/api/execute/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stubReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is("Wrong Answer")));

        // 3. Valid solution with return -> Accepted
        com.codeforge.dto.ExecutionDto.RunRequest validReq = com.codeforge.dto.ExecutionDto.RunRequest.builder()
                .problemId(104L)
                .language("python")
                .code("def pair_sum(nums, target):\n    seen = {}\n    for i, n in enumerate(nums):\n        if target - n in seen:\n            return [seen[target - n], i]\n        seen[n] = i\n    return [0, 1]")
                .build();

        mockMvc.perform(post("/api/execute/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.status", is("Accepted")))
                .andExpect(jsonPath("$.passedCount", greaterThanOrEqualTo(2)));

        // 4. GET /api/execute/submissions returns 200 OK
        mockMvc.perform(get("/api/execute/submissions")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(List.class)));
    }
}
