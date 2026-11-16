package com.codeforge;

import com.codeforge.config.JwtUtil;
import com.codeforge.dto.ExecutionDto;
import com.codeforge.dto.ExerciseDto;
import com.codeforge.dto.OverviewResponse;
import com.codeforge.dto.SubmissionDto;
import com.codeforge.entity.Exercise;
import com.codeforge.entity.Submission;
import com.codeforge.entity.User;
import com.codeforge.repository.ExerciseRepository;
import com.codeforge.repository.SavedExerciseRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.repository.UserRepository;
import com.codeforge.service.ExecutionService;
import com.codeforge.service.ExerciseService;
import com.codeforge.service.OverviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class Group2IntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private SavedExerciseRepository savedExerciseRepository;

    @Autowired
    private ExecutionService executionService;

    @Autowired
    private OverviewService overviewService;

    @Autowired
    private ExerciseService exerciseService;

    private User testUser;
    private Exercise testExercise;

    @BeforeEach
    void setUp() {
        savedExerciseRepository.deleteAll();
        submissionRepository.deleteAll();

        testUser = userRepository.findByUsername("test_user_group2").orElseGet(() -> {
            User u = User.builder()
                    .username("test_user_group2")
                    .email("test_group2@codeforge.io")
                    .fullName("Test Group2")
                    .passwordHash("hashed")
                    .solvedCount(0)
                    .streakDays(3)
                    .acceptanceRate(BigDecimal.ZERO)
                    .build();
            return userRepository.save(u);
        });

        testUser.setSolvedCount(0);
        testUser.setAcceptanceRate(BigDecimal.ZERO);
        userRepository.save(testUser);

        testExercise = exerciseRepository.findById(104L).orElseThrow();
    }

    @Test
    @DisplayName("Task 1: Update User Stats on Submit (first accepted increments solvedCount and updates acceptanceRate)")
    void testUpdateUserStatsOnSubmit() {
        ExecutionDto.SubmitRequest req = ExecutionDto.SubmitRequest.builder()
                .problemId(testExercise.getId())
                .language("python")
                .code("def solution_code(): pass")
                .build();

        // 1st submission -> should be Accepted (simulated)
        ExecutionDto.SubmitResponse res1 = executionService.submit(req, testUser.getUsername());
        assertTrue(Boolean.TRUE.equals(res1.getSuccess()));

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(1, updatedUser.getSolvedCount());
        assertEquals(new BigDecimal("100.00"), updatedUser.getAcceptanceRate());
        assertEquals(1, submissionRepository.countByUserId(testUser.getId()));
        assertEquals(1, submissionRepository.countByUserIdAndStatus(testUser.getId(), "Accepted"));
        assertTrue(submissionRepository.existsByUserIdAndExerciseIdAndStatus(testUser.getId(), testExercise.getId(), "Accepted"));

        // 2nd submission for the same exercise -> solvedCount should remain 1, rate remains 100.00
        ExecutionDto.SubmitResponse res2 = executionService.submit(req, testUser.getUsername());
        assertTrue(Boolean.TRUE.equals(res2.getSuccess()));

        updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(1, updatedUser.getSolvedCount(), "Solved count should not double-count the same exercise");
        assertEquals(2, submissionRepository.countByUserId(testUser.getId()));
        assertEquals(2, submissionRepository.countByUserIdAndStatus(testUser.getId(), "Accepted"));
        assertEquals(new BigDecimal("100.00"), updatedUser.getAcceptanceRate());
    }

    @Test
    @DisplayName("Task 2: Real Dashboard Metrics - weeklyGoal, nextChallenge and recentActivity")
    void testDashboardMetrics() {
        Submission s = Submission.builder()
                .user(testUser)
                .exercise(testExercise)
                .language("python")
                .sourceCode("test code")
                .status("Accepted")
                .runtimeMs(35)
                .memoryMb(new BigDecimal("15.5"))
                .submittedAt(LocalDateTime.now())
                .build();
        submissionRepository.save(s);

        OverviewResponse overview = overviewService.getOverview(testUser.getUsername());
        assertNotNull(overview);
        assertNotNull(overview.getUser());
        assertEquals("Test Group2", overview.getUser().getName());
        assertEquals(3, overview.getUser().getStreakDays());

        // Weekly goal should have done >= 1
        assertNotNull(overview.getWeeklyGoal());
        assertTrue(overview.getWeeklyGoal().getDone() >= 1);
        assertEquals(7, overview.getWeeklyGoal().getDays().size());

        // Next challenge should prefer an unsolved problem (not 104 which was submitted)
        assertNotNull(overview.getNextChallenge());
        assertNotEquals(104L, overview.getNextChallenge().getId(), "Next challenge should prefer an unsolved problem");

        // Recent activity should have the submission
        assertNotNull(overview.getRecentActivity());
        assertEquals(1, overview.getRecentActivity().size());
        assertEquals(testExercise.getTitle(), overview.getRecentActivity().get(0).getTitle());

        // Anonymous overview
        OverviewResponse anonOverview = overviewService.getOverview("anonymous");
        assertNotNull(anonOverview);
        assertEquals(0, anonOverview.getWeeklyGoal().getDone());
        assertTrue(anonOverview.getWeeklyGoal().getDays().stream().noneMatch(OverviewResponse.DayStatus::getDone));
    }

    @Test
    @DisplayName("Task 3: Bookmarks toggling, repository methods and tab filtering")
    void testBookmarksAndTabFiltering() {
        // Initially not saved
        assertFalse(savedExerciseRepository.existsByUserIdAndExerciseId(testUser.getId(), testExercise.getId()));

        // Toggle bookmark ON
        boolean state1 = exerciseService.toggleBookmark(testExercise.getId(), testUser.getUsername());
        assertTrue(state1, "Toggle 1 should save the exercise");
        assertTrue(savedExerciseRepository.existsByUserIdAndExerciseId(testUser.getId(), testExercise.getId()));

        // Verify toDto reflects saved = true
        List<ExerciseDto> savedList = exerciseService.getAll(null, null, null, "saved", testUser.getUsername());
        assertEquals(1, savedList.size());
        assertEquals(testExercise.getId(), savedList.get(0).getId());
        assertTrue(savedList.get(0).getSaved());

        // Toggle bookmark OFF
        boolean state2 = exerciseService.toggleBookmark(testExercise.getId(), testUser.getUsername());
        assertFalse(state2, "Toggle 2 should unsave the exercise");
        assertFalse(savedExerciseRepository.existsByUserIdAndExerciseId(testUser.getId(), testExercise.getId()));

        // tab=saved should now be empty
        List<ExerciseDto> savedEmptyList = exerciseService.getAll(null, null, null, "saved", testUser.getUsername());
        assertTrue(savedEmptyList.isEmpty());

        // tab=my filtering
        List<ExerciseDto> myList = exerciseService.getAll(null, null, null, "my", testUser.getUsername());
        assertTrue(myList.isEmpty());
    }

    @Test
    @DisplayName("Task 4: Problem Submissions History")
    void testProblemSubmissionsHistory() {
        Submission s1 = Submission.builder()
                .user(testUser)
                .exercise(testExercise)
                .language("python")
                .sourceCode("print('first')")
                .status("Accepted")
                .runtimeMs(20)
                .memoryMb(new BigDecimal("12.0"))
                .submittedAt(LocalDateTime.now().minusMinutes(5))
                .build();
        submissionRepository.save(s1);

        Submission s2 = Submission.builder()
                .user(testUser)
                .exercise(testExercise)
                .language("java")
                .sourceCode("class Solution {}")
                .status("Wrong Answer")
                .runtimeMs(25)
                .memoryMb(new BigDecimal("14.0"))
                .submittedAt(LocalDateTime.now())
                .build();
        submissionRepository.save(s2);

        List<SubmissionDto> subs = exerciseService.getSubmissions(testExercise.getId(), testUser.getUsername());
        assertEquals(2, subs.size());
        assertEquals("Wrong Answer", subs.get(0).getStatus(), "Should be ordered chronologically descending");
        assertEquals("Accepted", subs.get(1).getStatus());

        // Anonymous user should get empty list
        List<SubmissionDto> anonSubs = exerciseService.getSubmissions(testExercise.getId(), null);
        assertTrue(anonSubs.isEmpty());
    }

    @Test
    @DisplayName("Task 3 & 4: ExerciseController endpoints (bookmark toggle & submissions history)")
    void testControllerEndpointsAuthenticated() throws Exception {
        String token = jwtUtil.generateToken(testUser.getUsername());

        // Test POST /api/problems/{id}/bookmark
        mockMvc.perform(post("/api/problems/" + testExercise.getId() + "/bookmark")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true));

        mockMvc.perform(post("/api/problems/" + testExercise.getId() + "/bookmark")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(false));

        // Test GET /api/problems/{id}/submissions
        mockMvc.perform(get("/api/problems/" + testExercise.getId() + "/submissions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Task 3 & 4: ExerciseController endpoints unauthenticated behavior")
    void testControllerEndpointsUnauthenticated() throws Exception {
        // Bookmark toggle without auth should return 401 Unauthorized
        mockMvc.perform(post("/api/problems/" + testExercise.getId() + "/bookmark"))
                .andExpect(status().isUnauthorized());

        // Submissions without auth should return 200 with empty list
        mockMvc.perform(get("/api/problems/" + testExercise.getId() + "/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
