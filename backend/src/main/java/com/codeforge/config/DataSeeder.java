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
 * Seeds initial data into the database on application startup.
 * Uses "insert-if-not-exists" logic so re-runs are safe — no duplicate-key crashes.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final TestCaseRepository testCaseRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUsers();
        seedExercises();
        seedTestCases();
        log.info("✅ DataSeeder completed — seed data is ready.");
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    private void seedUsers() {
        if (userRepository.existsByEmail("alex@codeforge.io")) {
            log.info("User alex@codeforge.io already exists — skipping.");
            return;
        }

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

    private void seedExercises() {
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

        seedExerciseIfAbsent(Exercise.builder()
                .id(105L).title("Contains Duplicate").difficulty("Easy")
                .topics("Arrays, Hash set").acceptanceRate(new BigDecimal("82.10"))
                .description("Given an integer array nums, return true if any value appears at least twice in the array, and return false if every element is distinct.")
                .timeLimitMs(1000).memoryLimitMb(256).isCustom(false).createdAt(LocalDateTime.now())
                .build());

        seedExerciseIfAbsent(Exercise.builder()
                .id(106L).title("Valid Anagram").difficulty("Easy")
                .topics("Strings, Hash map").acceptanceRate(new BigDecimal("76.80"))
                .description("Given two strings s and t, return true if t is an anagram of s, and false otherwise.")
                .timeLimitMs(1000).memoryLimitMb(256).isCustom(false).createdAt(LocalDateTime.now())
                .build());

        seedExerciseIfAbsent(Exercise.builder()
                .id(107L).title("Group Anagrams").difficulty("Medium")
                .topics("Strings, Sorting").acceptanceRate(new BigDecimal("64.20"))
                .description("Given an array of strings strs, group the anagrams together. You can return the answer in any order.")
                .timeLimitMs(1000).memoryLimitMb(256).isCustom(false).createdAt(LocalDateTime.now())
                .build());

        seedExerciseIfAbsent(Exercise.builder()
                .id(108L).title("Top K Frequent Elements").difficulty("Medium")
                .topics("Arrays, Heap").acceptanceRate(new BigDecimal("61.50"))
                .description("Given an integer array nums and an integer k, return the k most frequent elements. You may return the answer in any order.")
                .timeLimitMs(1000).memoryLimitMb(256).isCustom(false).createdAt(LocalDateTime.now())
                .build());

        seedExerciseIfAbsent(Exercise.builder()
                .id(109L).title("Product of Array Except Self").difficulty("Medium")
                .topics("Arrays, Prefix sum").acceptanceRate(new BigDecimal("67.30"))
                .description("Given an integer array nums, return an array answer such that answer[i] is equal to the product of all the elements of nums except nums[i].")
                .timeLimitMs(1000).memoryLimitMb(256).isCustom(false).createdAt(LocalDateTime.now())
                .build());

        seedExerciseIfAbsent(Exercise.builder()
                .id(110L).title("Valid Parentheses").difficulty("Easy")
                .topics("Strings, Stacks").acceptanceRate(new BigDecimal("74.60"))
                .description("Given a string s containing just the characters '(', ')', '{', '}', '[' and ']', determine if the input string is valid.")
                .timeLimitMs(1000).memoryLimitMb(256).isCustom(false).createdAt(LocalDateTime.now())
                .build());
    }

    private void seedExerciseIfAbsent(Exercise exercise) {
        if (exerciseRepository.existsById(exercise.getId())) {
            log.info("Exercise {} '{}' already exists — skipping.", exercise.getId(), exercise.getTitle());
            return;
        }
        exerciseRepository.save(exercise);
        log.info("Seeded exercise: {} '{}'", exercise.getId(), exercise.getTitle());
    }

    // ── Test Cases ────────────────────────────────────────────────────────────

    private void seedTestCases() {
        // Only seed test cases for exercise 104 if it has none yet
        List<TestCase> existing = testCaseRepository.findByExerciseId(104L);
        if (!existing.isEmpty()) {
            log.info("Test cases for exercise 104 already exist — skipping.");
            return;
        }

        Exercise ex104 = exerciseRepository.findById(104L).orElse(null);
        if (ex104 == null) {
            log.warn("Exercise 104 not found — cannot seed test cases.");
            return;
        }

        testCaseRepository.save(TestCase.builder()
                .exercise(ex104)
                .caseName("Case 1")
                .inputData("nums = [2,7,11,15], target = 9")
                .expectedOutput("[0, 1]")
                .isSample(true)
                .build());

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

