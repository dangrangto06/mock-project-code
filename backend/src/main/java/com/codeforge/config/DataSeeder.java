package com.codeforge.config;

import com.codeforge.entity.*;
import com.codeforge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Database seeding component executed upon Spring Boot application
 * initialization.
 *
 * <p>
 * Implements {@link ApplicationRunner} with an execution {@link Order} of 1 to
 * ensure standard
 * mock data, administrative test accounts, coding challenges, sample test
 * cases, learner notes,
 * and community discussion threads are available immediately after the
 * application context starts up.
 * </p>
 *
 * <p>
 * <b>Only active on the "dev" profile</b> (the in-memory H2 database used by
 * {@code run-dev.bat}
 * and the integration test suite). The production profile (real SQL Server
 * "CodeForgeDb") is seeded
 * once, manually, via {@code CodeForgeDB.sql} — this runner must stay off
 * there, otherwise it would
 * silently re-insert the old hard-coded exercises 104-110 on every application
 * restart and collide
 * with the real 1-100 practice catalog.
 * </p>
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
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

        private final FeedbackRepository feedbackRepository;

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
                seedLearnerUsers();
                seedNotes();
                seedDiscussions();
                seedCommunityDiscussions();
                seedFeedbacks();
                log.info("✅ DataSeeder completed — seed data is ready.");
        }

        // ── Users ─────────────────────────────────────────────────────────────────

        /**
         * Seeds default user accounts (e.g. Alex Morgan) if not already present.
         *
         * <p>
         * Password hash corresponds to the BCrypt encoding of {@code password123}.
         * Configures pre-populated telemetry metrics (streak, solved count, acceptance
         * rate)
         * for dashboard testing.
         * </p>
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
         * <p>
         * Populates Exercise 104 ("Pair Sum", equivalent to Two Sum) with
         * multi-language starter templates,
         * description markdown, constraints, and runtime limits, along with popular
         * problems 105 through 110.
         * </p>
         */
        private void seedExercises() {
                // Exercise 104: Pair Sum (Two Sum equivalent) with starter templates in 4 major
                // languages
                seedExerciseIfAbsent(Exercise.builder()
                                .id(104L)
                                .title("Pair Sum")
                                .difficulty("Easy")
                                .topics("Arrays, Hash map")
                                .acceptanceRate(new BigDecimal("78.40"))
                                .description("Given an integer array `nums` and an integer `target`, return the indices of the two numbers that add up to `target`.\n\nYou may assume that each input has exactly one solution. You may not use the same element twice.\n\nReturn the answer in any order.")
                                .constraints("2 <= nums.length <= 10^4\n-10^9 <= nums[i], target <= 10^9\nExactly one valid answer exists.")
                                .starterCodePython(
                                                "def pair_sum(nums: list[int], target: int) -> list[int]:\n    # Remember each value and its index.\n    seen = {}\n\n    for i, num in enumerate(nums):\n        complement = target - num\n\n        if complement in seen:\n            return [seen[complement], i]\n\n        seen[num] = i\n\n    return []")
                                .starterCodeJavascript(
                                                "function pairSum(nums, target) {\n    const seen = new Map();\n    for (let i = 0; i < nums.length; i++) {\n        const complement = target - nums[i];\n        if (seen.has(complement)) {\n            return [seen.get(complement), i];\n        }\n        seen.set(nums[i], i);\n    }\n    return [];\n}")
                                .starterCodeCpp("class Solution {\npublic:\n    vector<int> pairSum(vector<int>& nums, int target) {\n        unordered_map<int, int> seen;\n        for (int i = 0; i < nums.size(); ++i) {\n            int complement = target - nums[i];\n            if (seen.find(complement) != seen.end()) {\n                return {seen[complement], i};\n            }\n            seen[nums[i]] = i;\n        }\n        return {};\n    }\n};")
                                .starterCodeJava(
                                                "class Solution {\n    public int[] pairSum(int[] nums, int target) {\n        Map<Integer, Integer> seen = new HashMap<>();\n        for (int i = 0; i < nums.length; i++) {\n            int complement = target - nums[i];\n            if (seen.containsKey(complement)) {\n                return new int[] { seen.get(complement), i };\n            }\n            seen.put(nums[i], i);\n        }\n        return new int[0];\n    }\n}")
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
         * Helper method to insert an exercise record only if an exercise with that ID
         * does not exist.
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
         * <p>
         * Ensures that when a user opens the code editor for Exercise 104, sample test
         * cases
         * are immediately rendered in the console tab for local verification.
         * </p>
         */
        private void seedTestCases() {
                // Guard against duplicate test case insertion if already present
                List<TestCase> existing = testCaseRepository.findByExerciseId(104L);
                if (!existing.isEmpty()) {
                        log.info("Test cases for exercise 104 already exist — skipping.");
                        return;
                }

                // Locate Exercise 104 (guaranteed present: seedExercises() always runs first in
                // run()).
                Exercise ex104 = exerciseRepository.findById(104L)
                                .orElseThrow(() -> new IllegalStateException(
                                                "Exercise 104 not found — cannot seed test cases."));

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
         * Seeds sample personal notes for user alex@codeforge.io if note repository is
         * empty.
         */
        private void seedNotes() {
                if (noteRepository.count() > 0) {
                        log.info("Notes already present — skipping note seeding.");
                        return;
                }

                User alex = userRepository.findByEmail("alex@codeforge.io")
                                .orElseThrow(() -> new IllegalStateException(
                                                "User alex@codeforge.io not found — cannot seed notes."));

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
         * Seeds sample community discussions and replies if discussion repository is
         * empty.
         */
        private void seedDiscussions() {
                if (discussionRepository.count() > 0) {
                        log.info("Discussions already present — skipping discussion seeding.");
                        return;
                }

                User alex = userRepository.findByEmail("alex@codeforge.io")
                                .orElseThrow(() -> new IllegalStateException(
                                                "User alex@codeforge.io not found — cannot seed discussions."));

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


        private void seedLearnerUsers() {
                String defaultPasswordHash = passwordEncoder.encode("Learner@123");
                List<User> learners = List.of(
                        User.builder()
                                .username("david_kim")
                                .email("david_kim@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("David Kim")
                                .role("Learner")
                                .streakDays(14)
                                .solvedCount(69)
                                .acceptanceRate(new BigDecimal("88.57"))
                                .practiceTimeHours(new BigDecimal("15.04"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("sarah_jenkins")
                                .email("sarah_jenkins@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Sarah Jenkins")
                                .role("Learner")
                                .streakDays(6)
                                .solvedCount(36)
                                .acceptanceRate(new BigDecimal("77.55"))
                                .practiceTimeHours(new BigDecimal("19.20"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("marcus_vance")
                                .email("marcus_vance@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Marcus Vance")
                                .role("Learner")
                                .streakDays(15)
                                .solvedCount(58)
                                .acceptanceRate(new BigDecimal("71.22"))
                                .practiceTimeHours(new BigDecimal("36.38"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("priya_natarajan")
                                .email("priya_natarajan@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Priya Natarajan")
                                .role("Learner")
                                .streakDays(9)
                                .solvedCount(43)
                                .acceptanceRate(new BigDecimal("66.51"))
                                .practiceTimeHours(new BigDecimal("32.66"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("liam_oconnor")
                                .email("liam_oconnor@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Liam O'Connor")
                                .role("Learner")
                                .streakDays(14)
                                .solvedCount(68)
                                .acceptanceRate(new BigDecimal("57.17"))
                                .practiceTimeHours(new BigDecimal("36.96"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("mia_thompson")
                                .email("mia_thompson@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Mia Thompson")
                                .role("Learner")
                                .streakDays(5)
                                .solvedCount(57)
                                .acceptanceRate(new BigDecimal("84.59"))
                                .practiceTimeHours(new BigDecimal("19.69"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("daniel_alves")
                                .email("daniel_alves@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Daniel Alves")
                                .role("Learner")
                                .streakDays(2)
                                .solvedCount(37)
                                .acceptanceRate(new BigDecimal("87.45"))
                                .practiceTimeHours(new BigDecimal("15.44"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("hana_suzuki")
                                .email("hana_suzuki@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Hana Suzuki")
                                .role("Learner")
                                .streakDays(6)
                                .solvedCount(55)
                                .acceptanceRate(new BigDecimal("51.88"))
                                .practiceTimeHours(new BigDecimal("36.74"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("omar_haddad")
                                .email("omar_haddad@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Omar Haddad")
                                .role("Learner")
                                .streakDays(2)
                                .solvedCount(3)
                                .acceptanceRate(new BigDecimal("66.21"))
                                .practiceTimeHours(new BigDecimal("22.32"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("isabella_rossi")
                                .email("isabella_rossi@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Isabella Rossi")
                                .role("Learner")
                                .streakDays(2)
                                .solvedCount(28)
                                .acceptanceRate(new BigDecimal("56.65"))
                                .practiceTimeHours(new BigDecimal("25.22"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("ethan_walker")
                                .email("ethan_walker@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Ethan Walker")
                                .role("Learner")
                                .streakDays(17)
                                .solvedCount(37)
                                .acceptanceRate(new BigDecimal("76.42"))
                                .practiceTimeHours(new BigDecimal("25.78"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("chloe_martin")
                                .email("chloe_martin@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Chloe Martin")
                                .role("Learner")
                                .streakDays(4)
                                .solvedCount(3)
                                .acceptanceRate(new BigDecimal("59.73"))
                                .practiceTimeHours(new BigDecimal("23.74"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("noah_becker")
                                .email("noah_becker@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Noah Becker")
                                .role("Learner")
                                .streakDays(15)
                                .solvedCount(20)
                                .acceptanceRate(new BigDecimal("70.65"))
                                .practiceTimeHours(new BigDecimal("19.45"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("grace_lee")
                                .email("grace_lee@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Grace Lee")
                                .role("Learner")
                                .streakDays(4)
                                .solvedCount(67)
                                .acceptanceRate(new BigDecimal("58.66"))
                                .practiceTimeHours(new BigDecimal("17.29"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("lucas_ferreira")
                                .email("lucas_ferreira@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Lucas Ferreira")
                                .role("Learner")
                                .streakDays(5)
                                .solvedCount(20)
                                .acceptanceRate(new BigDecimal("81.82"))
                                .practiceTimeHours(new BigDecimal("36.69"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("emma_novak")
                                .email("emma_novak@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Emma Novak")
                                .role("Learner")
                                .streakDays(9)
                                .solvedCount(67)
                                .acceptanceRate(new BigDecimal("64.37"))
                                .practiceTimeHours(new BigDecimal("32.51"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("ryan_bennett")
                                .email("ryan_bennett@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Ryan Bennett")
                                .role("Learner")
                                .streakDays(16)
                                .solvedCount(28)
                                .acceptanceRate(new BigDecimal("62.67"))
                                .practiceTimeHours(new BigDecimal("38.39"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("sofia_petrova")
                                .email("sofia_petrova@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Sofia Petrova")
                                .role("Learner")
                                .streakDays(12)
                                .solvedCount(12)
                                .acceptanceRate(new BigDecimal("47.41"))
                                .practiceTimeHours(new BigDecimal("14.53"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("aiden_murphy")
                                .email("aiden_murphy@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Aiden Murphy")
                                .role("Learner")
                                .streakDays(11)
                                .solvedCount(6)
                                .acceptanceRate(new BigDecimal("46.20"))
                                .practiceTimeHours(new BigDecimal("19.56"))
                                .createdAt(LocalDateTime.now())
                                .build(),
                        User.builder()
                                .username("zoe_carter")
                                .email("zoe_carter@codeforge.io")
                                .passwordHash(defaultPasswordHash)
                                .fullName("Zoe Carter")
                                .role("Learner")
                                .streakDays(21)
                                .solvedCount(41)
                                .acceptanceRate(new BigDecimal("66.16"))
                                .practiceTimeHours(new BigDecimal("36.64"))
                                .createdAt(LocalDateTime.now())
                                .build()
                );
                for (User learner : learners) {
                        if (!userRepository.existsByEmail(learner.getEmail())) {
                                userRepository.save(learner);
                        }
                }
                log.info("Seeded 20 virtual learner accounts.");
        }

        private void seedCommunityDiscussions() {
                if (discussionRepository.count() > 2) {
                        return;
                }
                Map<String, User> userCache = new HashMap<>();
                for (User u : userRepository.findAll()) {
                        userCache.put(u.getEmail(), u);
                }

                User author1 = userCache.get("david_kim@codeforge.io");
                if (author1 != null) {
                        Discussion disc1 = Discussion.builder()
                                        .author(author1)
                                        .title("Optimal O(N) Hash Map approach vs Sorting Two-Pointer for Pair Sum")
                                        .category("Solutions")
                                        .problemId(1L)
                                        .problemTitle("Pair Sum")
                                        .content("While sorting takes O(N log N) time and alters element indices, a single-pass hash map achieves optimal O(N) time and O(N) space. Store the complement `target - num` as you iterate.\n\nWatch out for edge cases with negative integers and when `complement == num`!")
                                        .upvotes(24)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(24))
                                        .build();
                        disc1 = discussionRepository.save(disc1);

                        User repUser_1_1 = userCache.get("sarah_jenkins@codeforge.io");
                        if (repUser_1_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc1)
                                                .author(repUser_1_1)
                                                .content("Great write-up David! Did you measure the memory overhead of the Map in Java? Sometimes Primitive IntIntMap helps on huge inputs.")
                                                .createdAt(LocalDateTime.now().minusHours(118))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_1_2 = userCache.get("priya_natarajan@codeforge.io");
                        if (repUser_1_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc1)
                                                .author(repUser_1_2)
                                                .content("Sorting + Two pointers is still super valuable when the question asks to return the values instead of indices and space is constrained to O(1)!")
                                                .createdAt(LocalDateTime.now().minusHours(117))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_1_3 = userCache.get("daniel_alves@codeforge.io");
                        if (repUser_1_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc1)
                                                .author(repUser_1_3)
                                                .content("100% agreed. In a live interview, proposing brute force O(N^2) first, then optimizing to Hash Map O(N) shows clear progression.")
                                                .createdAt(LocalDateTime.now().minusHours(116))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_1_4 = userCache.get("ethan_walker@codeforge.io");
                        if (repUser_1_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc1)
                                                .author(repUser_1_4)
                                                .content("Don't forget to handle integer overflow if target and numbers can be close to Integer.MAX_VALUE in other languages.")
                                                .createdAt(LocalDateTime.now().minusHours(115))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_1_5 = userCache.get("emma_novak@codeforge.io");
                        if (repUser_1_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc1)
                                                .author(repUser_1_5)
                                                .content("Clean and straightforward explanation! The single pass check `seen.containsKey(complement)` before inserting the current element is key.")
                                                .createdAt(LocalDateTime.now().minusHours(114))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_1_6 = userCache.get("zoe_carter@codeforge.io");
                        if (repUser_1_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc1)
                                                .author(repUser_1_6)
                                                .content("Bookmarked! This is the canonical problem for understanding Hash Map complement lookup patterns.")
                                                .createdAt(LocalDateTime.now().minusHours(113))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author2 = userCache.get("sarah_jenkins@codeforge.io");
                if (author2 != null) {
                        Discussion disc2 = Discussion.builder()
                                        .author(author2)
                                        .title("How to properly shrink the sliding window without redundant scans?")
                                        .category("Questions")
                                        .problemId(3L)
                                        .problemTitle("Longest Substring Without Repeating Characters")
                                        .content("When encountering a duplicate character in the sliding window, is it cleaner to advance the `left` pointer one-by-one in a while loop, or store character indices in a map and jump `left = max(left, lastSeenIndex + 1)` directly?")
                                        .upvotes(18)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(23))
                                        .build();
                        disc2 = discussionRepository.save(disc2);

                        User repUser_2_1 = userCache.get("david_kim@codeforge.io");
                        if (repUser_2_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc2)
                                                .author(repUser_2_1)
                                                .content("Storing the last seen index is strictly fewer iterations! Just remember `left = max(left, lastSeen[c] + 1)` so `left` never moves backward.")
                                                .createdAt(LocalDateTime.now().minusHours(112))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_2_2 = userCache.get("marcus_vance@codeforge.io");
                        if (repUser_2_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc2)
                                                .author(repUser_2_2)
                                                .content("Both run in O(N) amortized time, but the index map approach avoids unnecessary while iterations. Much cleaner in production code.")
                                                .createdAt(LocalDateTime.now().minusHours(111))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_2_3 = userCache.get("liam_oconnor@codeforge.io");
                        if (repUser_2_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc2)
                                                .author(repUser_2_3)
                                                .content("In C++ and Java, using an `int[128]` array for ASCII characters instead of `HashMap` gives an enormous speed boost on LeetCode tests.")
                                                .createdAt(LocalDateTime.now().minusHours(110))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_2_4 = userCache.get("hana_suzuki@codeforge.io");
                        if (repUser_2_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc2)
                                                .author(repUser_2_4)
                                                .content("I used to forget the `Math.max(left, ...)` condition and spent 30 minutes debugging why the pointer moved left. Great question!")
                                                .createdAt(LocalDateTime.now().minusHours(109))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_2_5 = userCache.get("noah_becker@codeforge.io");
                        if (repUser_2_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc2)
                                                .author(repUser_2_5)
                                                .content("The while loop approach with a `HashSet` is often easier to explain verbally to an interviewer under pressure.")
                                                .createdAt(LocalDateTime.now().minusHours(108))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_2_6 = userCache.get("ryan_bennett@codeforge.io");
                        if (repUser_2_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc2)
                                                .author(repUser_2_6)
                                                .content("Both are valid, but interviewers love when you mention that the while loop visits each character at most twice (2N total operations).")
                                                .createdAt(LocalDateTime.now().minusHours(107))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author3 = userCache.get("marcus_vance@codeforge.io");
                if (author3 != null) {
                        Discussion disc3 = Discussion.builder()
                                        .author(author3)
                                        .title("Avoid TLE in Python: collections.deque vs list.pop(0) in BFS traversals")
                                        .category("Tips")
                                        .problemId(null)
                                        .problemTitle(null)
                                        .content("PSA for Python developers: `list.pop(0)` is strictly O(N) because it shifts all remaining elements in memory! In tree and graph BFS traversals, this easily causes Time Limit Exceeded.\n\nAlways use `from collections import deque` and `queue.popleft()` which runs in guaranteed O(1) time.")
                                        .upvotes(35)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(22))
                                        .build();
                        disc3 = discussionRepository.save(disc3);

                        User repUser_3_1 = userCache.get("alex@codeforge.io");
                        if (repUser_3_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc3)
                                                .author(repUser_3_1)
                                                .content("Excellent reminder Marcus. This is one of the top subtle mistakes candidates make in Python live coding rounds.")
                                                .createdAt(LocalDateTime.now().minusHours(106))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_3_2 = userCache.get("priya_natarajan@codeforge.io");
                        if (repUser_3_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc3)
                                                .author(repUser_3_2)
                                                .content("I learned this the hard way on Word Ladder! Switched to `deque` and runtime dropped from 1800ms to 95ms.")
                                                .createdAt(LocalDateTime.now().minusHours(105))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_3_3 = userCache.get("mia_thompson@codeforge.io");
                        if (repUser_3_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc3)
                                                .author(repUser_3_3)
                                                .content("Same concept in JavaScript: `Array.prototype.shift()` is O(N). For large queues, use a pointer index or a custom ring buffer.")
                                                .createdAt(LocalDateTime.now().minusHours(104))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_3_4 = userCache.get("omar_haddad@codeforge.io");
                        if (repUser_3_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc3)
                                                .author(repUser_3_4)
                                                .content("In Java, also avoid `java.util.Vector` and `Stack`. Prefer `ArrayDeque` as it is non-synchronized and faster than `LinkedList`.")
                                                .createdAt(LocalDateTime.now().minusHours(103))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_3_5 = userCache.get("chloe_martin@codeforge.io");
                        if (repUser_3_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc3)
                                                .author(repUser_3_5)
                                                .content("Thanks for sharing! Adding this tip to my Python interview cheat sheet.")
                                                .createdAt(LocalDateTime.now().minusHours(102))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_3_6 = userCache.get("lucas_ferreira@codeforge.io");
                        if (repUser_3_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc3)
                                                .author(repUser_3_6)
                                                .content("A great follow-up is explaining why `deque` uses a doubly linked list of fixed-size memory blocks under the hood in CPython.")
                                                .createdAt(LocalDateTime.now().minusHours(101))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author4 = userCache.get("priya_natarajan@codeforge.io");
                if (author4 != null) {
                        Discussion disc4 = Discussion.builder()
                                        .author(author4)
                                        .title("FAANG Onsite Interview Debrief: Top 5 Patterns asked in 2026")
                                        .category("Interview Prep")
                                        .problemId(null)
                                        .problemTitle(null)
                                        .content("Just completed onsite loops at Google and Amazon. Over 5 technical rounds, 80% of algorithmic questions boiled down to these 5 archetypes:\n1. Sliding Window (dynamic & fixed size)\n2. Monotonic Stack (Next Greater Element)\n3. Top-K using Min/Max Heap\n4. Tree DFS with bottom-up path aggregations\n5. Topological Sort (Kahn's BFS algorithm)\n\nMastering these 5 patterns gave me immense confidence!")
                                        .upvotes(42)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(21))
                                        .build();
                        disc4 = discussionRepository.save(disc4);

                        User repUser_4_1 = userCache.get("david_kim@codeforge.io");
                        if (repUser_4_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc4)
                                                .author(repUser_4_1)
                                                .content("Huge congratulations Priya! Did any round ask follow-up questions about distributed scaling or memory limits?")
                                                .createdAt(LocalDateTime.now().minusHours(100))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_4_2 = userCache.get("sarah_jenkins@codeforge.io");
                        if (repUser_4_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc4)
                                                .author(repUser_4_2)
                                                .content("Thanks for the summary! Monotonic stack has been popping up so frequently recently in stock and histogram problems.")
                                                .createdAt(LocalDateTime.now().minusHours(99))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_4_3 = userCache.get("daniel_alves@codeforge.io");
                        if (repUser_4_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc4)
                                                .author(repUser_4_3)
                                                .content("Did you practice mostly LeetCode Mediums or Hard? How much did behavioral (LP / Googleyness) weigh into the loops?")
                                                .createdAt(LocalDateTime.now().minusHours(98))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_4_4 = userCache.get("isabella_rossi@codeforge.io");
                        if (repUser_4_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc4)
                                                .author(repUser_4_4)
                                                .content("Super encouraging post! Did you write tests on the whiteboard / Google Doc or did the interviewers provide test runners?")
                                                .createdAt(LocalDateTime.now().minusHours(97))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_4_5 = userCache.get("grace_lee@codeforge.io");
                        if (repUser_4_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc4)
                                                .author(repUser_4_5)
                                                .content("Topological sort is definitely an Amazon favorite for task scheduler and package dependency problems.")
                                                .createdAt(LocalDateTime.now().minusHours(96))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_4_6 = userCache.get("sofia_petrova@codeforge.io");
                        if (repUser_4_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc4)
                                                .author(repUser_4_6)
                                                .content("Bookmarked! Focusing on patterns rather than memorizing individual problems is 100% the right strategy.")
                                                .createdAt(LocalDateTime.now().minusHours(95))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author5 = userCache.get("liam_oconnor@codeforge.io");
                if (author5 != null) {
                        Discussion disc5 = Discussion.builder()
                                        .author(author5)
                                        .title("Why Greedy Two-Pointer correctly guarantees Maximum Area in Container With Most Water")
                                        .category("Solutions")
                                        .problemId(11L)
                                        .problemTitle("Container With Most Water")
                                        .content("A common doubt: Why are we sure that moving the pointer at the shorter height will never miss a better solution with an inner line?\n\nMathematical proof: Area is bounded by `min(height[l], height[r]) * (r - l)`. If we moved the taller pointer inwards, width decreases AND height can never exceed the shorter pointer. Thus area is strictly smaller! Moving the shorter line is the ONLY move that could possibly increase area.")
                                        .upvotes(29)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(20))
                                        .build();
                        disc5 = discussionRepository.save(disc5);

                        User repUser_5_1 = userCache.get("marcus_vance@codeforge.io");
                        if (repUser_5_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc5)
                                                .author(repUser_5_1)
                                                .content("This mathematical explanation is the clearest I've read. Explaining this proof in an interview shows true mastery.")
                                                .createdAt(LocalDateTime.now().minusHours(94))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_5_2 = userCache.get("mia_thompson@codeforge.io");
                        if (repUser_5_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc5)
                                                .author(repUser_5_2)
                                                .content("Brilliant deduction! The proof by contradiction makes it crystal clear why no potential maximal area is skipped.")
                                                .createdAt(LocalDateTime.now().minusHours(93))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_5_3 = userCache.get("ethan_walker@codeforge.io");
                        if (repUser_5_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc5)
                                                .author(repUser_5_3)
                                                .content("I love problems where the greedy choice has an intuitive mathematical invariant. O(N) time with O(1) space.")
                                                .createdAt(LocalDateTime.now().minusHours(92))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_5_4 = userCache.get("noah_becker@codeforge.io");
                        if (repUser_5_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc5)
                                                .author(repUser_5_4)
                                                .content("Do you have recommendations for similar problems that rely on eliminating sub-optimal search spaces with two pointers?")
                                                .createdAt(LocalDateTime.now().minusHours(91))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_5_5 = userCache.get("emma_novak@codeforge.io");
                        if (repUser_5_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc5)
                                                .author(repUser_5_5)
                                                .content("Trapping Rain Water uses a very similar leftMax / rightMax invariant! Highly recommend trying that next.")
                                                .createdAt(LocalDateTime.now().minusHours(90))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_5_6 = userCache.get("aiden_murphy@codeforge.io");
                        if (repUser_5_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc5)
                                                .author(repUser_5_6)
                                                .content("Crisp and concise. This helped me finally understand why we discard the shorter line.")
                                                .createdAt(LocalDateTime.now().minusHours(89))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author6 = userCache.get("mia_thompson@codeforge.io");
                if (author6 != null) {
                        Discussion disc6 = Discussion.builder()
                                        .author(author6)
                                        .title("Best strategy to avoid duplicate triplets in 3Sum without using an expensive Set?")
                                        .category("Questions")
                                        .problemId(15L)
                                        .problemTitle("3Sum")
                                        .content("I keep generating duplicate triplets in 3Sum when the input contains multiple identical numbers. What is the cleanest condition to skip duplicates for both the outer anchor index and the two inner two-pointer indices after finding a valid sum?")
                                        .upvotes(16)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(19))
                                        .build();
                        disc6 = discussionRepository.save(disc6);

                        User repUser_6_1 = userCache.get("sarah_jenkins@codeforge.io");
                        if (repUser_6_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc6)
                                                .author(repUser_6_1)
                                                .content("After sorting, for outer loop: `if (i > 0 && nums[i] == nums[i-1]) continue;`. That prevents using the same anchor twice!")
                                                .createdAt(LocalDateTime.now().minusHours(88))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_6_2 = userCache.get("priya_natarajan@codeforge.io");
                        if (repUser_6_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc6)
                                                .author(repUser_6_2)
                                                .content("For inner pointers, once `nums[l] + nums[r] == -nums[i]`: increment `l` while `nums[l] == nums[l-1]` and decrement `r` while `nums[r] == nums[r+1]`.")
                                                .createdAt(LocalDateTime.now().minusHours(87))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_6_3 = userCache.get("hana_suzuki@codeforge.io");
                        if (repUser_6_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc6)
                                                .author(repUser_6_3)
                                                .content("Crucial point: always compare with `i-1` and NOT `i+1`, otherwise you would skip valid triplets like `[-1, -1, 2]`!")
                                                .createdAt(LocalDateTime.now().minusHours(86))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_6_4 = userCache.get("chloe_martin@codeforge.io");
                        if (repUser_6_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc6)
                                                .author(repUser_6_4)
                                                .content("Sorting takes O(N log N) which is dominated by the O(N^2) two-pointer search, so sorting is basically free here.")
                                                .createdAt(LocalDateTime.now().minusHours(85))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_6_5 = userCache.get("ryan_bennett@codeforge.io");
                        if (repUser_6_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc6)
                                                .author(repUser_6_5)
                                                .content("Using a `Set<List<Integer>>` causes huge memory allocations and can trigger TLE. The pointer skip technique is strictly better.")
                                                .createdAt(LocalDateTime.now().minusHours(84))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_6_6 = userCache.get("zoe_carter@codeforge.io");
                        if (repUser_6_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc6)
                                                .author(repUser_6_6)
                                                .content("Thanks everyone! This exact question tripped me up in a phone screen last week.")
                                                .createdAt(LocalDateTime.now().minusHours(83))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author7 = userCache.get("daniel_alves@codeforge.io");
                if (author7 != null) {
                        Discussion disc7 = Discussion.builder()
                                        .author(author7)
                                        .title("Clean Stack solution in Java using modern Switch Expressions and Deque")
                                        .category("Solutions")
                                        .problemId(20L)
                                        .problemTitle("Valid Parentheses")
                                        .content("A neat trick for Valid Parentheses: whenever you see an opening bracket `(`, `[`, `{`, push the corresponding closing bracket `)`, `]`, `}` onto the stack!\n\nThen when you encounter a closing character, you only need to check `if (stack.isEmpty() || stack.pop() != c) return false;`. No complicated lookup map needed!")
                                        .upvotes(31)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(18))
                                        .build();
                        disc7 = discussionRepository.save(disc7);

                        User repUser_7_1 = userCache.get("david_kim@codeforge.io");
                        if (repUser_7_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc7)
                                                .author(repUser_7_1)
                                                .content("That trick is so elegant! Pushing the expected closing bracket reduces the matching condition to a single equality check.")
                                                .createdAt(LocalDateTime.now().minusHours(82))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_7_2 = userCache.get("liam_oconnor@codeforge.io");
                        if (repUser_7_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc7)
                                                .author(repUser_7_2)
                                                .content("And at the end, don't forget `return stack.isEmpty();` to catch leftover opening brackets like `(((`.")
                                                .createdAt(LocalDateTime.now().minusHours(81))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_7_3 = userCache.get("omar_haddad@codeforge.io");
                        if (repUser_7_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc7)
                                                .author(repUser_7_3)
                                                .content("In Java, always use `Deque<Character> stack = new ArrayDeque<>();`. Never use `java.util.Stack` because of synchronized lock overhead.")
                                                .createdAt(LocalDateTime.now().minusHours(80))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_7_4 = userCache.get("grace_lee@codeforge.io");
                        if (repUser_7_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc7)
                                                .author(repUser_7_4)
                                                .content("In C++, `std::string` can act as a stack with `push_back` and `pop_back` to avoid heap allocations completely!")
                                                .createdAt(LocalDateTime.now().minusHours(79))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_7_5 = userCache.get("lucas_ferreira@codeforge.io");
                        if (repUser_7_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc7)
                                                .author(repUser_7_5)
                                                .content("What is the time complexity if string length is odd? Quick early return: `if (s.length() % 2 != 0) return false;`!")
                                                .createdAt(LocalDateTime.now().minusHours(78))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_7_6 = userCache.get("sofia_petrova@codeforge.io");
                        if (repUser_7_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc7)
                                                .author(repUser_7_6)
                                                .content("Loved the early exit check for odd lengths. Saves unnecessary stack operations on invalid test cases.")
                                                .createdAt(LocalDateTime.now().minusHours(77))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author8 = userCache.get("hana_suzuki@codeforge.io");
                if (author8 != null) {
                        Discussion disc8 = Discussion.builder()
                                        .author(author8)
                                        .title("System Design for Backend Engineers: Transitioning from DSA to Architecture")
                                        .category("Interview Prep")
                                        .problemId(null)
                                        .problemTitle(null)
                                        .content("Once you feel comfortable with LeetCode Medium algorithms, how do you structure your System Design prep? Which resources helped you understand caching layers (Redis), DB partitioning/sharding, message brokers (Kafka/RabbitMQ), and CAP theorem tradeoffs most intuitively?")
                                        .upvotes(38)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(17))
                                        .build();
                        disc8 = discussionRepository.save(disc8);

                        User repUser_8_1 = userCache.get("alex@codeforge.io");
                        if (repUser_8_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc8)
                                                .author(repUser_8_1)
                                                .content("Start with Alex Xu's System Design Interview Volume 1 & 2. Practice back-of-the-envelope calculations for QPS and storage.")
                                                .createdAt(LocalDateTime.now().minusHours(76))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_8_2 = userCache.get("marcus_vance@codeforge.io");
                        if (repUser_8_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc8)
                                                .author(repUser_8_2)
                                                .content("Martin Kleppmann's 'Designing Data-Intensive Applications' (DDIA) is the gold standard for deep dive into storage engines and replication.")
                                                .createdAt(LocalDateTime.now().minusHours(75))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_8_3 = userCache.get("mia_thompson@codeforge.io");
                        if (repUser_8_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc8)
                                                .author(repUser_8_3)
                                                .content("Focus on real-world scenarios: URL shortener (hashing/collisions), Rate limiter (Token bucket/Redis), and Chat service (WebSockets).")
                                                .createdAt(LocalDateTime.now().minusHours(74))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_8_4 = userCache.get("isabella_rossi@codeforge.io");
                        if (repUser_8_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc8)
                                                .author(repUser_8_4)
                                                .content("Don't just memorize designs. Learn the trade-offs: Why choose NoSQL vs SQL? What happens during network partitions?")
                                                .createdAt(LocalDateTime.now().minusHours(73))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_8_5 = userCache.get("noah_becker@codeforge.io");
                        if (repUser_8_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc8)
                                                .author(repUser_8_5)
                                                .content("Mock interviews with peers are critical. In 45 minutes you have to drive the conversation from requirements to high-level to deep-dive.")
                                                .createdAt(LocalDateTime.now().minusHours(72))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_8_6 = userCache.get("ryan_bennett@codeforge.io");
                        if (repUser_8_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc8)
                                                .author(repUser_8_6)
                                                .content("Always ask clarifying questions in the first 5 minutes: Read-heavy vs Write-heavy ratio, Latency SLA, and Consistency requirements.")
                                                .createdAt(LocalDateTime.now().minusHours(71))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author9 = userCache.get("omar_haddad@codeforge.io");
                if (author9 != null) {
                        Discussion disc9 = Discussion.builder()
                                        .author(author9)
                                        .title("Bit Manipulation Tricks that frequently show up in coding interviews")
                                        .category("Tips")
                                        .problemId(null)
                                        .problemTitle(null)
                                        .content("Quick bit manipulation reference:\n- `n & (n - 1)` : Clears the lowest set bit (Brian Kernighan's algorithm for counting bits in O(k) steps)\n- `n & -n` : Isolates the lowest set bit (due to two's complement arithmetic)\n- `x ^ y` : Finds bits that differ\n- `x ^ x == 0` and `x ^ 0 == x` : Finds the single non-duplicate number in an array\n\nThese turn O(32) iterations into instant O(1) bitwise operations!")
                                        .upvotes(27)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(16))
                                        .build();
                        disc9 = discussionRepository.save(disc9);

                        User repUser_9_1 = userCache.get("priya_natarajan@codeforge.io");
                        if (repUser_9_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc9)
                                                .author(repUser_9_1)
                                                .content("Brian Kernighan's trick `n &= (n - 1)` is pure magic. Used it to solve Counting Bits in optimal O(N) time.")
                                                .createdAt(LocalDateTime.now().minusHours(70))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_9_2 = userCache.get("daniel_alves@codeforge.io");
                        if (repUser_9_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc9)
                                                .author(repUser_9_2)
                                                .content("Also `(n & (n - 1)) == 0 && n > 0` checks if a number is a power of two in exactly one line of code!")
                                                .createdAt(LocalDateTime.now().minusHours(69))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_9_3 = userCache.get("ethan_walker@codeforge.io");
                        if (repUser_9_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc9)
                                                .author(repUser_9_3)
                                                .content("Two's complement `-n = ~n + 1` is why `n & -n` works so cleanly to extract the rightmost 1-bit. Great refresher.")
                                                .createdAt(LocalDateTime.now().minusHours(68))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_9_4 = userCache.get("lucas_ferreira@codeforge.io");
                        if (repUser_9_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc9)
                                                .author(repUser_9_4)
                                                .content("Single Number II (where every element appears 3 times except one) is an awesome follow-up using bit counters.")
                                                .createdAt(LocalDateTime.now().minusHours(67))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_9_5 = userCache.get("emma_novak@codeforge.io");
                        if (repUser_9_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc9)
                                                .author(repUser_9_5)
                                                .content("Watch out for operator precedence in C++ and Java! Bitwise operators like `&` have lower precedence than `==`.")
                                                .createdAt(LocalDateTime.now().minusHours(66))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_9_6 = userCache.get("aiden_murphy@codeforge.io");
                        if (repUser_9_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc9)
                                                .author(repUser_9_6)
                                                .content("Always wrap bitwise expressions in parentheses: `(n & 1) == 0`. Saved me so many compiler bugs!")
                                                .createdAt(LocalDateTime.now().minusHours(65))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author10 = userCache.get("isabella_rossi@codeforge.io");
                if (author10 != null) {
                        Discussion disc10 = Discussion.builder()
                                        .author(author10)
                                        .title("Intuitive logic to decide which half is sorted in Rotated Binary Search")
                                        .category("Solutions")
                                        .problemId(33L)
                                        .problemTitle("Search in Rotated Sorted Array")
                                        .content("The key insight for Search in Rotated Sorted Array:\nAt least one half is ALWAYS sorted!\n- If `nums[left] <= nums[mid]`: The left half is sorted.\n  - Target is in left half if `nums[left] <= target < nums[mid]`.\n  - Otherwise target must be in right half.\n- Else: The right half is sorted.\n  - Target is in right half if `nums[mid] < target <= nums[right]`.\n  - Otherwise target must be in left half.\n\nStrictly O(log N) runtime with O(1) space.")
                                        .upvotes(25)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(15))
                                        .build();
                        disc10 = discussionRepository.save(disc10);

                        User repUser_10_1 = userCache.get("david_kim@codeforge.io");
                        if (repUser_10_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc10)
                                                .author(repUser_10_1)
                                                .content("Breaking it down by 'which half is guaranteed sorted' makes this problem so much easier than trying to locate the pivot first.")
                                                .createdAt(LocalDateTime.now().minusHours(64))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_10_2 = userCache.get("liam_oconnor@codeforge.io");
                        if (repUser_10_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc10)
                                                .author(repUser_10_2)
                                                .content("Be careful with duplicate elements (Search in Rotated Sorted Array II) where `nums[left] == nums[mid] == nums[right]` degrades to O(N).")
                                                .createdAt(LocalDateTime.now().minusHours(63))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_10_3 = userCache.get("hana_suzuki@codeforge.io");
                        if (repUser_10_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc10)
                                                .author(repUser_10_3)
                                                .content("The `<=` condition vs `<` is where most people get off-by-one errors. Checking the closed boundary on `left` or `right` is essential.")
                                                .createdAt(LocalDateTime.now().minusHours(62))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_10_4 = userCache.get("chloe_martin@codeforge.io");
                        if (repUser_10_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc10)
                                                .author(repUser_10_4)
                                                .content("I used to find the min index first with binary search and then do a second binary search. This one-pass method is twice as clean!")
                                                .createdAt(LocalDateTime.now().minusHours(61))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_10_5 = userCache.get("grace_lee@codeforge.io");
                        if (repUser_10_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc10)
                                                .author(repUser_10_5)
                                                .content("Can confirm, this exact question was my first round technical screening at Microsoft. Nailed it thanks to this pattern!")
                                                .createdAt(LocalDateTime.now().minusHours(60))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_10_6 = userCache.get("zoe_carter@codeforge.io");
                        if (repUser_10_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc10)
                                                .author(repUser_10_6)
                                                .content("Saved! Clean logic that eliminates messy edge cases.")
                                                .createdAt(LocalDateTime.now().minusHours(59))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author11 = userCache.get("ethan_walker@codeforge.io");
                if (author11 != null) {
                        Discussion disc11 = Discussion.builder()
                                        .author(author11)
                                        .title("Two Pointers vs Monotonic Stack for Trapping Rain Water: Which is preferred?")
                                        .category("Questions")
                                        .problemId(42L)
                                        .problemTitle("Trapping Rain Water")
                                        .content("I solved Trapping Rain Water using a Monotonic Decreasing Stack in O(N) time and O(N) space. But interviewers often ask to optimize to O(1) space using two pointers (`leftMax` and `rightMax`).\n\nHow do you explain the transition between both techniques smoothly in a live interview?")
                                        .upvotes(22)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(14))
                                        .build();
                        disc11 = discussionRepository.save(disc11);

                        User repUser_11_1 = userCache.get("sarah_jenkins@codeforge.io");
                        if (repUser_11_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc11)
                                                .author(repUser_11_1)
                                                .content("Start by explaining the stack horizontal-layering approach: stack computes water by horizontal bars bounded by left and right walls.")
                                                .createdAt(LocalDateTime.now().minusHours(58))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_11_2 = userCache.get("marcus_vance@codeforge.io");
                        if (repUser_11_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc11)
                                                .author(repUser_11_2)
                                                .content("Then explain: 'We can also calculate water vertically column-by-column: water[i] = min(maxL, maxR) - height[i]'. That naturally motivates two pointers!")
                                                .createdAt(LocalDateTime.now().minusHours(57))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_11_3 = userCache.get("mia_thompson@codeforge.io");
                        if (repUser_11_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc11)
                                                .author(repUser_11_3)
                                                .content("Because we only need `min(maxL, maxR)`, whichever side has the smaller max determines the water level regardless of the other side!")
                                                .createdAt(LocalDateTime.now().minusHours(56))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_11_4 = userCache.get("omar_haddad@codeforge.io");
                        if (repUser_11_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc11)
                                                .author(repUser_11_4)
                                                .content("That insight: 'If leftMax < rightMax, the water at left is determined solely by leftMax' is the eureka moment interviewers look for.")
                                                .createdAt(LocalDateTime.now().minusHours(55))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_11_5 = userCache.get("noah_becker@codeforge.io");
                        if (repUser_11_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc11)
                                                .author(repUser_11_5)
                                                .content("The two-pointer solution is only 15 lines of code and O(1) extra space. Unbeatable.")
                                                .createdAt(LocalDateTime.now().minusHours(54))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_11_6 = userCache.get("sofia_petrova@codeforge.io");
                        if (repUser_11_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc11)
                                                .author(repUser_11_6)
                                                .content("Practicing both is great because Monotonic Stack generalizes better to Largest Rectangle in Histogram.")
                                                .createdAt(LocalDateTime.now().minusHours(53))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author12 = userCache.get("chloe_martin@codeforge.io");
                if (author12 != null) {
                        Discussion disc12 = Discussion.builder()
                                        .author(author12)
                                        .title("Kadane's Algorithm: Understanding dynamic state transition vs Divide & Conquer")
                                        .category("Solutions")
                                        .problemId(53L)
                                        .problemTitle("Maximum Subarray")
                                        .content("Kadane's algorithm is the pinnacle of elegant 1D Dynamic Programming:\n`currentMax = max(nums[i], currentMax + nums[i])`\nAt each index, you make a binary choice: either extend the running contiguous subarray, or abandon it and start fresh from `nums[i]`.\n\nFollow-up question: How do you return the actual starting and ending indices of the maximal subarray?")
                                        .upvotes(33)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(13))
                                        .build();
                        disc12 = discussionRepository.save(disc12);

                        User repUser_12_1 = userCache.get("priya_natarajan@codeforge.io");
                        if (repUser_12_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc12)
                                                .author(repUser_12_1)
                                                .content("To track indices: keep `start = 0`, `end = 0`, `tempStart = 0`. Whenever `currentMax < 0`, set `tempStart = i + 1`!")
                                                .createdAt(LocalDateTime.now().minusHours(52))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_12_2 = userCache.get("daniel_alves@codeforge.io");
                        if (repUser_12_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc12)
                                                .author(repUser_12_2)
                                                .content("When `currentMax + nums[i] > globalMax`, update `globalMax`, `start = tempStart`, and `end = i`.")
                                                .createdAt(LocalDateTime.now().minusHours(51))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_12_3 = userCache.get("isabella_rossi@codeforge.io");
                        if (repUser_12_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc12)
                                                .author(repUser_12_3)
                                                .content("The Divide & Conquer O(N log N) approach using Segment Tree concepts is also a frequent follow-up for streaming/range query updates.")
                                                .createdAt(LocalDateTime.now().minusHours(50))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_12_4 = userCache.get("lucas_ferreira@codeforge.io");
                        if (repUser_12_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc12)
                                                .author(repUser_12_4)
                                                .content("What if all numbers in the array are negative? Kadane's handles this seamlessly by picking the single least-negative number.")
                                                .createdAt(LocalDateTime.now().minusHours(49))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_12_5 = userCache.get("ryan_bennett@codeforge.io");
                        if (repUser_12_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc12)
                                                .author(repUser_12_5)
                                                .content("This is often asked as the base problem before Maximum Product Subarray (which requires tracking both min and max due to negative signs).")
                                                .createdAt(LocalDateTime.now().minusHours(48))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_12_6 = userCache.get("aiden_murphy@codeforge.io");
                        if (repUser_12_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc12)
                                                .author(repUser_12_6)
                                                .content("Short, intuitive, and optimal O(N) time with O(1) auxiliary space.")
                                                .createdAt(LocalDateTime.now().minusHours(47))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author13 = userCache.get("noah_becker@codeforge.io");
                if (author13 != null) {
                        Discussion disc13 = Discussion.builder()
                                        .author(author13)
                                        .title("Handling Recursion Stack Overflow in Deep Trees and Graphs")
                                        .category("Tips")
                                        .problemId(null)
                                        .problemTitle(null)
                                        .content("When traversing deep graphs or skewed trees with depth N >= 10^5, standard recursive DFS will trigger `StackOverflowError` in Java/C++ or exceed `sys.getrecursionlimit()` in Python.\n\nKey mitigation strategies:\n1. Convert recursion to an iterative loop with an explicit `ArrayDeque` allocated on the heap.\n2. In Python, `sys.setrecursionlimit(200000)` works for competitions but is discouraged in production.\n3. Use BFS or Morris Traversal for O(1) space tree walks.")
                                        .upvotes(20)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(12))
                                        .build();
                        disc13 = discussionRepository.save(disc13);

                        User repUser_13_1 = userCache.get("alex@codeforge.io");
                        if (repUser_13_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc13)
                                                .author(repUser_13_1)
                                                .content("Spot on Noah. In production systems, heap space is gigabytes while stack space is typically just 1-2 megabytes.")
                                                .createdAt(LocalDateTime.now().minusHours(46))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_13_2 = userCache.get("liam_oconnor@codeforge.io");
                        if (repUser_13_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc13)
                                                .author(repUser_13_2)
                                                .content("Morris Traversal is mind-blowing when you first see it. Re-wiring temporary right child pointers to predecessors achieves O(1) space!")
                                                .createdAt(LocalDateTime.now().minusHours(45))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_13_3 = userCache.get("hana_suzuki@codeforge.io");
                        if (repUser_13_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc13)
                                                .author(repUser_13_3)
                                                .content("For graph DFS with cycles, iterative DFS also makes it easier to track visited state and path back-tracing.")
                                                .createdAt(LocalDateTime.now().minusHours(44))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_13_4 = userCache.get("ethan_walker@codeforge.io");
                        if (repUser_13_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc13)
                                                .author(repUser_13_4)
                                                .content("Iterative tree traversal with simulated call-stack is a favorite Google interview follow-up.")
                                                .createdAt(LocalDateTime.now().minusHours(43))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_13_5 = userCache.get("emma_novak@codeforge.io");
                        if (repUser_13_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc13)
                                                .author(repUser_13_5)
                                                .content("Good reminder to never rely on `sys.setrecursionlimit` during interviews unless explicitly permitted by the interviewer.")
                                                .createdAt(LocalDateTime.now().minusHours(42))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_13_6 = userCache.get("zoe_carter@codeforge.io");
                        if (repUser_13_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc13)
                                                .author(repUser_13_6)
                                                .content("Always mention stack depth when discussing the space complexity of recursive DFS: O(H) where H can be O(N) in the worst case.")
                                                .createdAt(LocalDateTime.now().minusHours(41))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author14 = userCache.get("grace_lee@codeforge.io");
                if (author14 != null) {
                        Discussion disc14 = Discussion.builder()
                                        .author(author14)
                                        .title("Mock Interview Checklist: What interviewers silently evaluate during the 45 mins")
                                        .category("Interview Prep")
                                        .problemId(null)
                                        .problemTitle(null)
                                        .content("A rubric based on shadowing senior FAANG interviewers:\n1. **Clarification (3-5 mins)**: Do you check empty inputs, duplicates, range of values, and mutable inputs?\n2. **Communication (ongoing)**: Never stay silent for >30 seconds. Think out loud.\n3. **Brute Force first (5 mins)**: State the naive approach and its complexity to set a baseline.\n4. **Optimization & Dry Run**: Manually trace a sample input through your logic before writing code.\n5. **Clean Code**: Meaningful variable names, modular helpers, and zero syntax flubs.\n6. **Complexity**: Stating time AND space complexity accurately without waiting to be prompted.")
                                        .upvotes(45)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(11))
                                        .build();
                        disc14 = discussionRepository.save(disc14);

                        User repUser_14_1 = userCache.get("david_kim@codeforge.io");
                        if (repUser_14_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc14)
                                                .author(repUser_14_1)
                                                .content("Point #4 is massive. Tracing your algorithm with a small input on whiteboard catches 90% of off-by-one errors before running.")
                                                .createdAt(LocalDateTime.now().minusHours(40))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_14_2 = userCache.get("marcus_vance@codeforge.io");
                        if (repUser_14_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc14)
                                                .author(repUser_14_2)
                                                .content("Agree on point #3! Jumping straight to complex code without agreeing on the approach with the interviewer is the #1 rejection reason.")
                                                .createdAt(LocalDateTime.now().minusHours(39))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_14_3 = userCache.get("mia_thompson@codeforge.io");
                        if (repUser_14_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc14)
                                                .author(repUser_14_3)
                                                .content("Also remember: interviewers want to see how you respond to hints. Treat hints as collaborative nudges, not failures.")
                                                .createdAt(LocalDateTime.now().minusHours(38))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_14_4 = userCache.get("omar_haddad@codeforge.io");
                        if (repUser_14_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc14)
                                                .author(repUser_14_4)
                                                .content("Great checklist Grace! Printing this out and reviewing before every mock interview session.")
                                                .createdAt(LocalDateTime.now().minusHours(37))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_14_5 = userCache.get("chloe_martin@codeforge.io");
                        if (repUser_14_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc14)
                                                .author(repUser_14_5)
                                                .content("Time management is everything. If you are not writing code by minute 20, you will likely run out of time to test.")
                                                .createdAt(LocalDateTime.now().minusHours(36))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_14_6 = userCache.get("sofia_petrova@codeforge.io");
                        if (repUser_14_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc14)
                                                .author(repUser_14_6)
                                                .content("Don't forget to test edge cases at the end: null/empty array, single element, all duplicates, and negative numbers.")
                                                .createdAt(LocalDateTime.now().minusHours(35))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author15 = userCache.get("lucas_ferreira@codeforge.io");
                if (author15 != null) {
                        Discussion disc15 = Discussion.builder()
                                        .author(author15)
                                        .title("Single pass O(N) time and O(1) space tracking min price so far in Stock Buy/Sell")
                                        .category("Solutions")
                                        .problemId(121L)
                                        .problemTitle("Best Time to Buy and Sell Stock")
                                        .content("For Best Time to Buy and Sell Stock I:\nYou only need to maintain `minPrice = min(minPrice, price)` and calculate potential profit `maxProfit = max(maxProfit, price - minPrice)` as you iterate.\n\nThis simple problem forms the bedrock for:\n- Stock II: Infinite transactions (accumulate every positive slope `prices[i] - prices[i-1]`)\n- Stock with Cooldown / Fee: State machine DP with `buy` and `sell` states.")
                                        .upvotes(26)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(10))
                                        .build();
                        disc15 = discussionRepository.save(disc15);

                        User repUser_15_1 = userCache.get("sarah_jenkins@codeforge.io");
                        if (repUser_15_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc15)
                                                .author(repUser_15_1)
                                                .content("The connection from Stock I greedy tracking to Stock with Cooldown state machine DP is such a great learning journey.")
                                                .createdAt(LocalDateTime.now().minusHours(34))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_15_2 = userCache.get("priya_natarajan@codeforge.io");
                        if (repUser_15_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc15)
                                                .author(repUser_15_2)
                                                .content("In Stock II, many candidates overcomplicate with DP when greedy slope addition is literally 3 lines of code!")
                                                .createdAt(LocalDateTime.now().minusHours(33))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_15_3 = userCache.get("daniel_alves@codeforge.io");
                        if (repUser_15_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc15)
                                                .author(repUser_15_3)
                                                .content("Another perspective: It is essentially finding the maximum subarray sum on the array of price differences!")
                                                .createdAt(LocalDateTime.now().minusHours(32))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_15_4 = userCache.get("isabella_rossi@codeforge.io");
                        if (repUser_15_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc15)
                                                .author(repUser_15_4)
                                                .content("Super clear summary Lucas. This is the ultimate beginner friendly problem to build dynamic tracking intuition.")
                                                .createdAt(LocalDateTime.now().minusHours(31))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_15_5 = userCache.get("grace_lee@codeforge.io");
                        if (repUser_15_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc15)
                                                .author(repUser_15_5)
                                                .content("Kadane's algorithm and Stock I are practically twin problems when viewed through running difference lenses.")
                                                .createdAt(LocalDateTime.now().minusHours(30))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_15_6 = userCache.get("aiden_murphy@codeforge.io");
                        if (repUser_15_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc15)
                                                .author(repUser_15_6)
                                                .content("Clean, concise, and optimal. Great stepping stone for DP series.")
                                                .createdAt(LocalDateTime.now().minusHours(29))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author16 = userCache.get("emma_novak@codeforge.io");
                if (author16 != null) {
                        Discussion disc16 = Discussion.builder()
                                        .author(author16)
                                        .title("Space optimization from O(N) array to two rolling variables in 1D DP")
                                        .category("Questions")
                                        .problemId(198L)
                                        .problemTitle("House Robber")
                                        .content("In House Robber: `dp[i] = max(dp[i-1], dp[i-2] + nums[i])`.\nBecause `dp[i]` only looks back 2 steps, we can discard the entire array and keep just `prev1` and `prev2`, dropping auxiliary space from O(N) to O(1).\n\nDoes this rolling variable optimization apply universally whenever recurrences have a constant lookback window?")
                                        .upvotes(21)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(9))
                                        .build();
                        disc16 = discussionRepository.save(disc16);

                        User repUser_16_1 = userCache.get("david_kim@codeforge.io");
                        if (repUser_16_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc16)
                                                .author(repUser_16_1)
                                                .content("Yes! Exactly like Fibonacci numbers and Climbing Stairs. Any linear recurrence `f(n) = g(f(n-1), ..., f(n-k))` only needs `k` variables.")
                                                .createdAt(LocalDateTime.now().minusHours(28))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_16_2 = userCache.get("liam_oconnor@codeforge.io");
                        if (repUser_16_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc16)
                                                .author(repUser_16_2)
                                                .content("Even in 2D DP (like Unique Paths or Edit Distance), if row `i` only depends on row `i-1`, you can reduce space from O(M*N) to O(N) using two rows!")
                                                .createdAt(LocalDateTime.now().minusHours(27))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_16_3 = userCache.get("hana_suzuki@codeforge.io");
                        if (repUser_16_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc16)
                                                .author(repUser_16_3)
                                                .content("And if you update in the correct order (e.g. reverse iteration in 0/1 Knapsack), you can even reduce to a single 1D array!")
                                                .createdAt(LocalDateTime.now().minusHours(26))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_16_4 = userCache.get("ethan_walker@codeforge.io");
                        if (repUser_16_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc16)
                                                .author(repUser_16_4)
                                                .content("Interviewers always ask for space optimization after you write the 1D table. Showing the progression from O(N) to O(1) gets top marks.")
                                                .createdAt(LocalDateTime.now().minusHours(25))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_16_5 = userCache.get("noah_becker@codeforge.io");
                        if (repUser_16_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc16)
                                                .author(repUser_16_5)
                                                .content("Just be careful: if the problem asks to reconstruct the actual path of houses robbed, you must retain the full array or back-pointers.")
                                                .createdAt(LocalDateTime.now().minusHours(24))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_16_6 = userCache.get("ryan_bennett@codeforge.io");
                        if (repUser_16_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc16)
                                                .author(repUser_16_6)
                                                .content("House Robber II (circular houses) is a fantastic follow-up: run the linear solution twice, once for `[0..n-2]` and once for `[1..n-1]`!")
                                                .createdAt(LocalDateTime.now().minusHours(23))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author17 = userCache.get("ryan_bennett@codeforge.io");
                if (author17 != null) {
                        Discussion disc17 = Discussion.builder()
                                        .author(author17)
                                        .title("BFS vs DFS vs Disjoint Set Union (Union-Find) for grid connected components")
                                        .category("Solutions")
                                        .problemId(200L)
                                        .problemTitle("Number of Islands")
                                        .content("Three approaches compared for Number of Islands:\n1. **DFS**: Fewest lines of code, modifies grid in-place (`grid[r][c] = '0'`), risk of recursion stack overflow on 1000x1000 grids.\n2. **BFS**: Iterative with queue, memory bounded by perimeter of island (min(M, N)), zero recursion risk.\n3. **Union-Find (DSU)**: Best for dynamic scenarios (e.g. Number of Islands II where land cells are added over time via a stream).")
                                        .upvotes(34)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(8))
                                        .build();
                        disc17 = discussionRepository.save(disc17);

                        User repUser_17_1 = userCache.get("marcus_vance@codeforge.io");
                        if (repUser_17_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc17)
                                                .author(repUser_17_1)
                                                .content("Mentioning Union-Find as the optimal choice for the dynamic/streaming follow-up is an instant hire signal in interviews.")
                                                .createdAt(LocalDateTime.now().minusHours(22))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_17_2 = userCache.get("mia_thompson@codeforge.io");
                        if (repUser_17_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc17)
                                                .author(repUser_17_2)
                                                .content("For BFS, remember to mark the cell as visited *immediately upon pushing to queue*, not upon popping! Otherwise duplicate nodes flood the queue.")
                                                .createdAt(LocalDateTime.now().minusHours(21))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_17_3 = userCache.get("omar_haddad@codeforge.io");
                        if (repUser_17_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc17)
                                                .author(repUser_17_3)
                                                .content("That queue deduplication tip is huge! Marking on pop instead of push causes memory to explode to O(M*N) instead of O(min(M,N)).")
                                                .createdAt(LocalDateTime.now().minusHours(20))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_17_4 = userCache.get("chloe_martin@codeforge.io");
                        if (repUser_17_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc17)
                                                .author(repUser_17_4)
                                                .content("Also in-place modification of `grid` might not be allowed in production if input is immutable. Always ask the interviewer first.")
                                                .createdAt(LocalDateTime.now().minusHours(19))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_17_5 = userCache.get("lucas_ferreira@codeforge.io");
                        if (repUser_17_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc17)
                                                .author(repUser_17_5)
                                                .content("Union-Find with path compression and union by rank has nearly O(1) amortized operations via the inverse Ackermann function α(N).")
                                                .createdAt(LocalDateTime.now().minusHours(18))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_17_6 = userCache.get("zoe_carter@codeforge.io");
                        if (repUser_17_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc17)
                                                .author(repUser_17_6)
                                                .content("Outstanding comparison Ryan. Covering DFS, BFS, and DSU tradeoffs shows broad algorithmic maturity.")
                                                .createdAt(LocalDateTime.now().minusHours(17))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author18 = userCache.get("sofia_petrova@codeforge.io");
                if (author18 != null) {
                        Discussion disc18 = Discussion.builder()
                                        .author(author18)
                                        .title("Iterative pointer reversal without losing node references in Reverse Linked List")
                                        .category("Tips")
                                        .problemId(206L)
                                        .problemTitle("Reverse Linked List")
                                        .content("The foolproof 4-step pointer rotation for reversing a singly linked list:\n```java\nListNode nextTemp = curr.next; // 1. Save next\ncurr.next = prev;              // 2. Reverse link\nprev = curr;                   // 3. Step prev forward\ncurr = nextTemp;               // 4. Step curr forward\n```\nAlways return `prev` as the new head (since `curr` ends at null)!\nDrawing the 3 pointers on paper completely prevents null pointer errors.")
                                        .upvotes(28)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(7))
                                        .build();
                        disc18 = discussionRepository.save(disc18);

                        User repUser_18_1 = userCache.get("sarah_jenkins@codeforge.io");
                        if (repUser_18_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc18)
                                                .author(repUser_18_1)
                                                .content("The 4-step rhythm: Save next -> Reverse -> Advance prev -> Advance curr. Once you memorize this rhythm, you never forget.")
                                                .createdAt(LocalDateTime.now().minusHours(16))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_18_2 = userCache.get("priya_natarajan@codeforge.io");
                        if (repUser_18_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc18)
                                                .author(repUser_18_2)
                                                .content("The recursive approach is also great practice: `ListNode newHead = reverseList(head.next); head.next.next = head; head.next = null;`.")
                                                .createdAt(LocalDateTime.now().minusHours(15))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_18_3 = userCache.get("daniel_alves@codeforge.io");
                        if (repUser_18_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc18)
                                                .author(repUser_18_3)
                                                .content("Recursive uses O(N) call stack space though, so iterative with O(1) space is always the industry standard.")
                                                .createdAt(LocalDateTime.now().minusHours(14))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_18_4 = userCache.get("isabella_rossi@codeforge.io");
                        if (repUser_18_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc18)
                                                .author(repUser_18_4)
                                                .content("Reverse Linked List II (reversing between index left and right) builds directly on this using a dummy head node.")
                                                .createdAt(LocalDateTime.now().minusHours(13))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_18_5 = userCache.get("grace_lee@codeforge.io");
                        if (repUser_18_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc18)
                                                .author(repUser_18_5)
                                                .content("Reverse Nodes in k-Group is the ultimate final boss for this pattern. Mastering this base technique is crucial.")
                                                .createdAt(LocalDateTime.now().minusHours(12))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_18_6 = userCache.get("aiden_murphy@codeforge.io");
                        if (repUser_18_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc18)
                                                .author(repUser_18_6)
                                                .content("Always draw the pointers! Visualizing `prev`, `curr`, and `nextTemp` makes linked list bugs impossible to hide.")
                                                .createdAt(LocalDateTime.now().minusHours(11))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author19 = userCache.get("aiden_murphy@codeforge.io");
                if (author19 != null) {
                        Discussion disc19 = Discussion.builder()
                                        .author(author19)
                                        .title("O(N) time without using the division operator via prefix and suffix products")
                                        .category("Solutions")
                                        .problemId(238L)
                                        .problemTitle("Product of Array Except Self")
                                        .content("The challenge: Solve in O(N) time WITHOUT using the division operator `/`.\n\nOptimal two-pass approach:\n1. Initialize `ans` array where `ans[i]` contains the product of all elements to the left of `i`.\n2. Iterate backwards maintaining a running `rightProduct` variable, multiplying `ans[i] *= rightProduct`, and updating `rightProduct *= nums[i]`.\n\nResult: Exactly O(N) time and O(1) extra space (excluding output array)!")
                                        .upvotes(37)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(6))
                                        .build();
                        disc19 = discussionRepository.save(disc19);

                        User repUser_19_1 = userCache.get("david_kim@codeforge.io");
                        if (repUser_19_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc19)
                                                .author(repUser_19_1)
                                                .content("The right-to-left rolling accumulator is so smart because it avoids allocating a separate suffix product array.")
                                                .createdAt(LocalDateTime.now().minusHours(10))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_19_2 = userCache.get("liam_oconnor@codeforge.io");
                        if (repUser_19_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc19)
                                                .author(repUser_19_2)
                                                .content("What if division was allowed? Then you'd have to deal with zero elements: single zero vs multiple zeros. Much messier!")
                                                .createdAt(LocalDateTime.now().minusHours(9))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_19_3 = userCache.get("hana_suzuki@codeforge.io");
                        if (repUser_19_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc19)
                                                .author(repUser_19_3)
                                                .content("Yes! If there are two or more zeros, all elements become zero. The prefix/suffix product naturally handles zeros with zero special casing.")
                                                .createdAt(LocalDateTime.now().minusHours(8))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_19_4 = userCache.get("ethan_walker@codeforge.io");
                        if (repUser_19_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc19)
                                                .author(repUser_19_4)
                                                .content("This question tests whether you can break a composite computation into symmetric sub-problems: `prefix[i-1] * suffix[i+1]`.")
                                                .createdAt(LocalDateTime.now().minusHours(7))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_19_5 = userCache.get("noah_becker@codeforge.io");
                        if (repUser_19_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc19)
                                                .author(repUser_19_5)
                                                .content("One of the most frequently asked problems at Meta and Apple. Clean, deterministic, and no edge-case landmines.")
                                                .createdAt(LocalDateTime.now().minusHours(6))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_19_6 = userCache.get("emma_novak@codeforge.io");
                        if (repUser_19_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc19)
                                                .author(repUser_19_6)
                                                .content("Clear and elegant explanation Aiden. Bookmarking for interview revision!")
                                                .createdAt(LocalDateTime.now().minusHours(5))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }

                User author20 = userCache.get("zoe_carter@codeforge.io");
                if (author20 != null) {
                        Discussion disc20 = Discussion.builder()
                                        .author(author20)
                                        .title("Top-down memoized recursion vs Bottom-up tabulation for Coin Change")
                                        .category("Questions")
                                        .problemId(322L)
                                        .problemTitle("Coin Change")
                                        .content("In Coin Change (unbounded knapsack), what are the practical performance tradeoffs between top-down DP with memoization vs bottom-up tabulation?\n\nWhy does bottom-up array iteration frequently benchmark 2-3x faster than memoized recursion in Java and C++?")
                                        .upvotes(23)
                                        .moderationStatus("APPROVED")
                                        .createdAt(LocalDateTime.now().minusDays(5))
                                        .build();
                        disc20 = discussionRepository.save(disc20);

                        User repUser_20_1 = userCache.get("alex@codeforge.io");
                        if (repUser_20_1 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc20)
                                                .author(repUser_20_1)
                                                .content("Bottom-up avoids function call overhead (call frame setup/teardown) and benefits heavily from CPU cache locality (sequential memory access).")
                                                .createdAt(LocalDateTime.now().minusHours(4))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_20_2 = userCache.get("marcus_vance@codeforge.io");
                        if (repUser_20_2 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc20)
                                                .author(repUser_20_2)
                                                .content("In top-down, hash map lookup or jumping around non-contiguous array indices causes frequent CPU L1/L2 cache misses.")
                                                .createdAt(LocalDateTime.now().minusHours(3))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_20_3 = userCache.get("mia_thompson@codeforge.io");
                        if (repUser_20_3 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc20)
                                                .author(repUser_20_3)
                                                .content("Bottom-up also prevents recursion stack overflow on large target amounts (e.g. amount = 10000 with coin = 1).")
                                                .createdAt(LocalDateTime.now().minusHours(2))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_20_4 = userCache.get("omar_haddad@codeforge.io");
                        if (repUser_20_4 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc20)
                                                .author(repUser_20_4)
                                                .content("Top-down has one advantage though: it only visits reachable subproblems. If many amounts are unreachable, top-down computes fewer states.")
                                                .createdAt(LocalDateTime.now().minusHours(1))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_20_5 = userCache.get("chloe_martin@codeforge.io");
                        if (repUser_20_5 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc20)
                                                .author(repUser_20_5)
                                                .content("For Coin Change, initializing `dp` array with `amount + 1` (a sentinel value larger than any valid answer) makes the min check super clean.")
                                                .createdAt(LocalDateTime.now().minusHours(0))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                        User repUser_20_6 = userCache.get("ryan_bennett@codeforge.io");
                        if (repUser_20_6 != null) {
                                DiscussionReply rep = DiscussionReply.builder()
                                                .discussion(disc20)
                                                .author(repUser_20_6)
                                                .content("Great discussion Zoe! Starting with top-down to discover the recurrence and then converting to bottom-up is the textbook workflow.")
                                                .createdAt(LocalDateTime.now().minusHours(-1))
                                                .build();
                                discussionReplyRepository.save(rep);
                        }
                }
                log.info("Seeded 20 community discussions with 120 replies.");
        }

        private void seedFeedbacks() {
                if (feedbackRepository.count() > 0) {
                        return;
                }

                User david = userRepository.findByEmail("david_kim@codeforge.io").orElse(null);
                User sarah = userRepository.findByEmail("sarah_jenkins@codeforge.io").orElse(null);
                User marcus = userRepository.findByEmail("marcus_vance@codeforge.io").orElse(null);

                Feedback f1 = Feedback.builder()
                                .user(david)
                                .name("David Kim")
                                .email("david_kim@codeforge.io")
                                .subject("Platform experience")
                                .message("The interactive terminal is incredibly fast! Would love to see more dynamic programming problems added to the library.")
                                .status("REVIEWED")
                                .createdAt(LocalDateTime.now().minusHours(28))
                                .build();

                Feedback f2 = Feedback.builder()
                                .user(sarah)
                                .name("Sarah Jenkins")
                                .email("sarah_jenkins@codeforge.io")
                                .subject("UI suggestion")
                                .message("Dark mode looks great on OLED displays. It would be awesome if we could customize font sizes in the code editor.")
                                .status("NEW")
                                .createdAt(LocalDateTime.now().minusHours(14))
                                .build();

                Feedback f3 = Feedback.builder()
                                .user(marcus)
                                .name("Marcus Vance")
                                .email("marcus_vance@codeforge.io")
                                .subject("Feature Request")
                                .message("Could we get weekly algorithmic contests or timed sprint sessions? That would elevate practice excitement!")
                                .status("NEW")
                                .createdAt(LocalDateTime.now().minusHours(2))
                                .build();

                feedbackRepository.saveAll(List.of(f1, f2, f3));
                log.info("Seeded initial user feedbacks.");
        }
}
