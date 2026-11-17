package com.codeforge.config;

import com.codeforge.entity.*;
import com.codeforge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Database seeding component executed upon Spring Boot application initialization.
 *
 * <p>Implements {@link ApplicationRunner} with an execution {@link Order} of 1 to ensure standard
 * mock data, administrative test accounts, coding challenges, sample test cases, learner notes,
 * and community discussion threads are available immediately after the application context starts up.</p>
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    /** Repository for managing user entity persistence and verification. */
    private final UserRepository userRepository;

    /** Repository for managing coding exercise entities and starter codes. */
    private final ExerciseRepository exerciseRepository;

    /** Repository for managing exercise test cases and sample verification data. */
    private final TestCaseRepository testCaseRepository;

    private final NoteRepository noteRepository;

    private final DiscussionRepository discussionRepository;

    private final DiscussionReplyRepository discussionReplyRepository;

    private final DiscussionUpvoteRepository discussionUpvoteRepository;

    private final PasswordEncoder passwordEncoder;

    /**
     * Executes the database seeding logic upon application startup.
     *
     * @param args Command-line arguments passed to the application.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUsers();
        seedExercises();
        seedTestCases();
        seedNotes();
        seedDiscussions();
        log.info("✅ DataSeeder completed — seed data is ready.");
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    /**
     * Seeds default user accounts (e.g. Alex Morgan) if not already present.
     *
     * <p>Password hash corresponds to the BCrypt encoding of {@code password123}.
     * Configures pre-populated telemetry metrics (streak, solved count, acceptance rate)
     * for dashboard testing.</p>
     */
    private void seedUsers() {
        if (userRepository.existsByEmail("alex@codeforge.io")) {
            log.info("User alex@codeforge.io already exists — skipping.");
            return;
        }

        // Build default administrative user with populated practice statistics
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

        userRepository.save(alex);
        log.info("Seeded user: alex@codeforge.io");
    }

    // ── Exercises ─────────────────────────────────────────────────────────────

    /**
     * Seeds foundational algorithm challenges into the catalog.
     *
     * <p>Populates Exercise 104 ("Pair Sum", equivalent to Two Sum) with multi-language starter templates,
     * description markdown, constraints, and runtime limits, along with popular problems 105 through 110.</p>
     */
    private void seedExercises() {
        // Exercise 104: Pair Sum (Two Sum equivalent) with starter templates in 4 major languages
        seedExerciseIfAbsent(Exercise.builder()
                .id(104L)
                .title("Pair Sum")
                .difficulty("Easy")
                .topics("Arrays, Hash map")
                .acceptanceRate(new BigDecimal("78.40"))
                .description("Given an integer array `nums` and an integer `target`, return the indices of the two numbers that add up to `target`.\n\nYou may assume that each input has exactly one solution. You may not use the same element twice.\n\nReturn the answer in any order.")
                .constraints("2 <= nums.length <= 10^4\n-10^9 <= nums[i], target <= 10^9\nExactly one valid answer exists.")
                .starterCodePython("def pair_sum(nums: list[int], target: int) -> list[int]:\n    # Remember each value and its index.\n    seen = {}\n\n    for i, num in enumerate(nums):\n        complement = target - num\n\n        if complement in seen:\n            return [seen[complement], i]\n\n        seen[num] = i\n\n    return []")
                .starterCodeJavascript("function pairSum(nums, target) {\n    const seen = new Map();\n    for (let i = 0; i < nums.length; i++) {\n        const complement = target - nums[i];\n        if (seen.has(complement)) {\n            return [seen.get(complement), i];\n        }\n        seen.set(nums[i], i);\n    }\n    return [];\n}")
                .starterCodeCpp("class Solution {\npublic:\n    vector<int> pairSum(vector<int>& nums, int target) {\n        unordered_map<int, int> seen;\n        for (int i = 0; i < nums.size(); ++i) {\n            int complement = target - nums[i];\n            if (seen.find(complement) != seen.end()) {\n                return {seen[complement], i};\n            }\n            seen[nums[i]] = i;\n        }\n        return {};\n    }\n};")
                .starterCodeJava("class Solution {\n    public int[] pairSum(int[] nums, int target) {\n        Map<Integer, Integer> seen = new HashMap<>();\n        for (int i = 0; i < nums.length; i++) {\n            int complement = target - nums[i];\n            if (seen.containsKey(complement)) {\n                return new int[] { seen.get(complement), i };\n            }\n            seen.put(nums[i], i);\n        }\n        return new int[0];\n    }\n}")
                .timeLimitMs(1000)
                .memoryLimitMb(256)
                .isCustom(true)
                .createdAt(LocalDateTime.now())
                .build());

        // Exercise 105: Contains Duplicate
        seedExerciseIfAbsent(Exercise.builder()
                .id(105L).title("Contains Duplicate").difficulty("Easy")
                .topics("Arrays, Hash set").acceptanceRate(new BigDecimal("82.10"))
                .description("Given an integer array nums, return true if any value appears at least twice in the array, and return false if every element is distinct.")
                .timeLimitMs(1000).memoryLimitMb(256).isCustom(false).createdAt(LocalDateTime.now())
                .build());

        // Exercise 106: Valid Anagram
        seedExerciseIfAbsent(Exercise.builder()
                .id(106L).title("Valid Anagram").difficulty("Easy")
                .topics("Strings, Hash map").acceptanceRate(new BigDecimal("76.80"))
                .description("Given two strings s and t, return true if t is an anagram of s, and false otherwise.")
                .timeLimitMs(1000).memoryLimitMb(256).isCustom(false).createdAt(LocalDateTime.now())
                .build());

        // Exercise 107: Group Anagrams
        seedExerciseIfAbsent(Exercise.builder()
                .id(107L).title("Group Anagrams").difficulty("Medium")
                .topics("Strings, Sorting").acceptanceRate(new BigDecimal("64.20"))
                .description("Given an array of strings strs, group the anagrams together. You can return the answer in any order.")
                .timeLimitMs(1000).memoryLimitMb(256).isCustom(false).createdAt(LocalDateTime.now())
                .build());

        // Exercise 108: Top K Frequent Elements
        seedExerciseIfAbsent(Exercise.builder()
                .id(108L).title("Top K Frequent Elements").difficulty("Medium")
                .topics("Arrays, Heap").acceptanceRate(new BigDecimal("61.50"))
                .description("Given an integer array nums and an integer k, return the k most frequent elements. You may return the answer in any order.")
                .timeLimitMs(1000).memoryLimitMb(256).isCustom(false).createdAt(LocalDateTime.now())
                .build());

        // Exercise 109: Product of Array Except Self
        seedExerciseIfAbsent(Exercise.builder()
                .id(109L).title("Product of Array Except Self").difficulty("Medium")
                .topics("Arrays, Prefix sum").acceptanceRate(new BigDecimal("67.30"))
                .description("Given an integer array nums, return an array answer such that answer[i] is equal to the product of all the elements of nums except nums[i].")
                .timeLimitMs(1000).memoryLimitMb(256).isCustom(false).createdAt(LocalDateTime.now())
                .build());

        // Exercise 110: Valid Parentheses
        seedExerciseIfAbsent(Exercise.builder()
                .id(110L).title("Valid Parentheses").difficulty("Easy")
                .topics("Strings, Stacks").acceptanceRate(new BigDecimal("74.60"))
                .description("Given a string s containing just the characters '(', ')', '{', '}', '[' and ']', determine if the input string is valid.")
                .timeLimitMs(1000).memoryLimitMb(256).isCustom(false).createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * Helper method to insert an exercise record only if an exercise with that ID does not exist.
     *
     * @param exercise The {@link Exercise} entity candidate to persist.
     */
    private void seedExerciseIfAbsent(Exercise exercise) {
        if (exerciseRepository.existsById(exercise.getId())) {
            log.info("Exercise {} '{}' already exists — skipping.", exercise.getId(), exercise.getTitle());
            return;
        }
        exerciseRepository.save(exercise);
        log.info("Seeded exercise: {} '{}'", exercise.getId(), exercise.getTitle());
    }

    // ── Test Cases ────────────────────────────────────────────────────────────

    /**
     * Seeds sample test cases for initial problems (specifically Exercise 104).
     *
     * <p>Ensures that when a user opens the code editor for Exercise 104, sample test cases
     * are immediately rendered in the console tab for local verification.</p>
     */
    private void seedTestCases() {
        // Guard against duplicate test case insertion if already present
        List<TestCase> existing = testCaseRepository.findByExerciseId(104L);
        if (!existing.isEmpty()) {
            log.info("Test cases for exercise 104 already exist — skipping.");
            return;
        }

        // Locate Exercise 104
        Exercise ex104 = exerciseRepository.findById(104L).orElse(null);
        if (ex104 == null) {
            log.warn("Exercise 104 not found — cannot seed test cases.");
            return;
        }

        // Test Case 1: Standard positive case
        testCaseRepository.save(TestCase.builder()
                .exercise(ex104)
                .caseName("Case 1")
                .inputData("nums = [2,7,11,15], target = 9")
                .expectedOutput("[0, 1]")
                .isSample(true)
                .build());

        // Test Case 2: Unsorted array requiring distinct indices
        testCaseRepository.save(TestCase.builder()
                .exercise(ex104)
                .caseName("Case 2")
                .inputData("nums = [3,2,4], target = 6")
                .expectedOutput("[1, 2]")
                .isSample(true)
                .build());

        log.info("Seeded 2 test cases for exercise 104.");
    }

    /**
     * Seeds sample personal notes for user alex@codeforge.io if note repository is empty.
     */
    private void seedNotes() {
        if (noteRepository.count() > 0) {
            log.info("Notes already present — skipping note seeding.");
            return;
        }

        User alex = userRepository.findByEmail("alex@codeforge.io").orElse(null);
        if (alex == null) {
            log.warn("User alex@codeforge.io not found — skipping note seeding.");
            return;
        }

        Note note1 = Note.builder()
                .user(alex)
                .title("Two Pointers Technique & Edge Cases")
                .problemId(104L)
                .problemTitle("Pair Sum")
                .tags("Two Pointers, Arrays, Patterns")
                .content("Always sort before applying two pointers if order doesn't matter. Watch out for duplicates and 0-indexed boundaries. If sum < target, advance left; if sum > target, decrement right.")
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build();

        Note note2 = Note.builder()
                .user(alex)
                .title("Dynamic Programming State Transitions")
                .problemId(null)
                .problemTitle(null)
                .tags("DP, Memoization, General")
                .content("1. Define state `dp[i]`.\n2. Formulate recurrence relation.\n3. Identify base cases.\n4. Optimize space if only previous `k` states are needed.")
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build();

        noteRepository.saveAll(List.of(note1, note2));
        log.info("Seeded 2 sample learner notes for alex@codeforge.io");
    }

    /**
     * Seeds sample community discussions and replies if discussion repository is empty.
     */
    private void seedDiscussions() {
        if (discussionRepository.count() > 0) {
            log.info("Discussions already present — skipping discussion seeding.");
            return;
        }

        User alex = userRepository.findByEmail("alex@codeforge.io").orElse(null);
        if (alex == null) {
            log.warn("User alex@codeforge.io not found — skipping discussion seeding.");
            return;
        }

        Discussion disc1 = Discussion.builder()
                .author(alex)
                .title("Optimal O(n) Hash Map approach for Pair Sum / Two Sum")
                .category("Solutions")
                .problemId(104L)
                .problemTitle("Pair Sum")
                .content("Instead of a brute-force O(n²) nested loop or O(n log n) two-pointer after sorting, you can achieve O(n) time and O(n) space using a single-pass hash map.\n\nStore the complement `target - num` as you iterate!")
                .upvotes(12)
                .createdAt(LocalDateTime.now().minusDays(3))
                .build();
        disc1 = discussionRepository.save(disc1);

        DiscussionReply reply1 = DiscussionReply.builder()
                .discussion(disc1)
                .author(alex)
                .content("Great tip! Make sure to check if the current element equals complement when dealing with negative values.")
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();
        discussionReplyRepository.save(reply1);

        Discussion disc2 = Discussion.builder()
                .author(alex)
                .title("How to prepare for System Design & Algorithmic coding rounds concurrently?")
                .category("Interview Prep")
                .problemId(null)
                .problemTitle(null)
                .content("Balancing LeetCode algorithm practice and high-level system design can be overwhelming. What schedule or balance works best for full-time engineers?")
                .upvotes(8)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
        disc2 = discussionRepository.save(disc2);

        DiscussionReply reply2 = DiscussionReply.builder()
                .discussion(disc2)
                .author(alex)
                .content("I recommend 1-2 algorithm problems every morning, and reading one system design case study (e.g. Rate Limiter, TinyURL) on weekends.")
                .createdAt(LocalDateTime.now().minusHours(12))
                .build();
        discussionReplyRepository.save(reply2);

        DiscussionUpvote upvote1 = DiscussionUpvote.builder()
                .discussion(disc1)
                .user(alex)
                .createdAt(LocalDateTime.now().minusDays(3))
                .build();
        DiscussionUpvote upvote2 = DiscussionUpvote.builder()
                .discussion(disc2)
                .user(alex)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
        discussionUpvoteRepository.saveAll(List.of(upvote1, upvote2));

        log.info("Seeded 2 sample community discussions with replies");
    }
}
