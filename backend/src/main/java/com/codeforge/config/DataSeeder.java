package com.codeforge.config;

import com.codeforge.entity.Exercise;
import com.codeforge.entity.TestCase;
import com.codeforge.entity.User;
import com.codeforge.repository.ExerciseRepository;
import com.codeforge.repository.TestCaseRepository;
import com.codeforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Database seeding component executed upon Spring Boot application initialization.
 *
 * <p>Implements {@link ApplicationRunner} with an execution {@link Order} of 1 to ensure standard
 * mock data, administrative test accounts, coding challenges, and sample test cases are available
 * immediately after the application context starts up.</p>
 *
 * <p><strong>Key Design Considerations:</strong></p>
 * <ul>
 *   <li><b>Idempotent Seeding:</b> Utilizes "insert-if-absent" checks using primary key and unique constraint
 *       existence queries, making repeated container boots, application restarts, or dev-profile reloads safe
 *       against duplicate key errors.</li>
 *   <li><b>Transactional Integrity:</b> Wrapped in {@link Transactional} to ensure atomicity across relational
 *       entities (Users &rarr; Exercises &rarr; TestCases).</li>
 *   <li><b>Pre-Configured Algorithm Challenges:</b> Seeds standard LeetCode-style algorithm problems across
 *       Easy and Medium difficulties, including starter code templates for Python, JavaScript, C++, and Java.</li>
 * </ul>
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
                .passwordHash("$2a$10$7EqJtq98hPqEX7fNZaFWoOeV2d6m8C6sX1w/2WjH4Uf31hB7uRfa2")
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
}
