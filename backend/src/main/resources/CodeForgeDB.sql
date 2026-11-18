-- ==============================================================================
-- CodeForge Database — Full Rebuild Script (DDL + DML)
-- ==============================================================================
-- Target Engine : Microsoft SQL Server 2019+
-- Database      : CodeForgeDb
--
-- What this script does, in order:
--   1. Drops the 9 CodeForge tables if they already exist (safe to re-run).
--   2. Re-creates the schema (unchanged from CodeForgeDB.sql).
--   3. Seeds 1 admin account + 20 virtual learner accounts.
--   4. Seeds the full 100-problem practice catalog (id 1-100), replacing the
--      7 legacy problems (104-110) and the 100 problems that used to live only
--      in frontend/js/data.js. Every problem's author is a randomly chosen
--      learner from the 20 seeded above.
--   5. Seeds one or more sample test cases per problem.
--
-- IMPORTANT: run this against the REAL SQL Server "CodeForgeDb" database
-- (the default Spring profile), not the H2 in-memory "dev" profile used by
-- run-dev.bat / the automated tests. See the accompanying notes for why.
-- ==============================================================================

USE CodeForgeDb;
GO

-- ------------------------------------------------------------------------------
-- 0. Drop existing tables (children first, to satisfy foreign keys)
-- ------------------------------------------------------------------------------
IF OBJECT_ID('dbo.community_reports', 'U') IS NOT NULL DROP TABLE dbo.community_reports;
IF OBJECT_ID('dbo.discussion_upvotes', 'U') IS NOT NULL DROP TABLE dbo.discussion_upvotes;
IF OBJECT_ID('dbo.discussion_replies', 'U') IS NOT NULL DROP TABLE dbo.discussion_replies;
IF OBJECT_ID('dbo.discussions', 'U') IS NOT NULL DROP TABLE dbo.discussions;
IF OBJECT_ID('dbo.notes', 'U') IS NOT NULL DROP TABLE dbo.notes;
IF OBJECT_ID('dbo.saved_exercises', 'U') IS NOT NULL DROP TABLE dbo.saved_exercises;
IF OBJECT_ID('dbo.submissions', 'U') IS NOT NULL DROP TABLE dbo.submissions;
IF OBJECT_ID('dbo.test_cases', 'U') IS NOT NULL DROP TABLE dbo.test_cases;
IF OBJECT_ID('dbo.exercises', 'U') IS NOT NULL DROP TABLE dbo.exercises;
IF OBJECT_ID('dbo.users', 'U') IS NOT NULL DROP TABLE dbo.users;
GO

-- ------------------------------------------------------------------------------
-- 1. Table: dbo.users
-- ------------------------------------------------------------------------------
CREATE TABLE dbo.users (
    id                   BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    username             VARCHAR(100) NOT NULL UNIQUE,
    email                VARCHAR(255) NOT NULL UNIQUE,
    password_hash        VARCHAR(255) NOT NULL,
    full_name            VARCHAR(150) NOT NULL,
    role                 VARCHAR(50)  NULL,
    streak_days          INT NULL,
    solved_count         INT NULL,
    acceptance_rate      NUMERIC(19,2) NULL,
    practice_time_hours  NUMERIC(19,2) NULL,
    reset_token          VARCHAR(255) NULL,
    reset_token_expiry   DATETIME2 NULL,
    created_at           DATETIME2 NULL
);
GO

-- ------------------------------------------------------------------------------
-- 2. Table: dbo.exercises
-- ------------------------------------------------------------------------------
CREATE TABLE dbo.exercises (
    id                      BIGINT NOT NULL PRIMARY KEY,
    title                   VARCHAR(255) NOT NULL,
    difficulty              VARCHAR(50)  NOT NULL,
    topics                  VARCHAR(255) NOT NULL,
    acceptance_rate         NUMERIC(19,2) NULL,
    description             TEXT NOT NULL,
    constraints             TEXT NULL,
    starter_code_python     TEXT NULL,
    starter_code_javascript TEXT NULL,
    starter_code_cpp        TEXT NULL,
    starter_code_java       TEXT NULL,
    time_limit_ms           INT NULL,
    memory_limit_mb         INT NULL,
    is_custom               BIT NULL,
    author_id               BIGINT NULL,
    moderation_status       VARCHAR(20) DEFAULT 'APPROVED' NOT NULL,
    rejection_reason        VARCHAR(500) NULL,
    created_at              DATETIME2 NULL,
    CONSTRAINT fk_exercises_author FOREIGN KEY (author_id) REFERENCES dbo.users(id)
);
GO

-- ------------------------------------------------------------------------------
-- 3. Table: dbo.test_cases
-- ------------------------------------------------------------------------------
CREATE TABLE dbo.test_cases (
    id               BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    exercise_id      BIGINT NOT NULL,
    case_name        VARCHAR(100) NOT NULL,
    input_data       TEXT NOT NULL,
    expected_output  TEXT NOT NULL,
    is_sample        BIT NULL,
    CONSTRAINT fk_test_cases_exercise FOREIGN KEY (exercise_id) REFERENCES dbo.exercises(id)
);
GO

-- ------------------------------------------------------------------------------
-- 4. Table: dbo.submissions
-- ------------------------------------------------------------------------------
CREATE TABLE dbo.submissions (
    id                  BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    exercise_id         BIGINT NOT NULL,
    language            VARCHAR(50) NOT NULL,
    source_code         TEXT NOT NULL,
    status              VARCHAR(50) NOT NULL,
    runtime_ms          INT NULL,
    memory_mb           NUMERIC(19,2) NULL,
    runtime_beats_pct   NUMERIC(19,2) NULL,
    memory_beats_pct    NUMERIC(19,2) NULL,
    tests_passed        INT NULL,
    total_tests         INT NULL,
    submitted_at        DATETIME2 NULL,
    CONSTRAINT fk_submissions_user     FOREIGN KEY (user_id)     REFERENCES dbo.users(id),
    CONSTRAINT fk_submissions_exercise FOREIGN KEY (exercise_id) REFERENCES dbo.exercises(id)
);
GO

-- ------------------------------------------------------------------------------
-- 5. Table: dbo.saved_exercises
-- ------------------------------------------------------------------------------
CREATE TABLE dbo.saved_exercises (
    id               BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    exercise_id      BIGINT NOT NULL,
    created_at       DATETIME2 NULL,
    CONSTRAINT uk_saved_exercises_user_exercise UNIQUE (user_id, exercise_id),
    CONSTRAINT fk_saved_exercises_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT fk_saved_exercises_exercise FOREIGN KEY (exercise_id) REFERENCES dbo.exercises(id)
);
GO

-- ------------------------------------------------------------------------------
-- 6. Table: dbo.notes
-- ------------------------------------------------------------------------------
CREATE TABLE dbo.notes (
    id               BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    title            VARCHAR(255) NOT NULL,
    problem_id       BIGINT NULL,
    problem_title    VARCHAR(255) NULL,
    tags             VARCHAR(255) NULL,
    content          TEXT NOT NULL,
    updated_at       DATETIME2 NULL,
    CONSTRAINT fk_notes_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);
GO

-- ------------------------------------------------------------------------------
-- 7. Table: dbo.discussions
-- ------------------------------------------------------------------------------
CREATE TABLE dbo.discussions (
    id               BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    author_id        BIGINT NOT NULL,
    title            VARCHAR(255) NOT NULL,
    category         VARCHAR(50) NOT NULL,
    problem_id       BIGINT NULL,
    problem_title    VARCHAR(255) NULL,
    content          TEXT NOT NULL,
    upvotes          INT DEFAULT 0 NOT NULL,
    moderation_status VARCHAR(20) DEFAULT 'APPROVED' NOT NULL,
    rejection_reason  VARCHAR(500) NULL,
    created_at       DATETIME2 NULL,
    CONSTRAINT fk_discussions_author FOREIGN KEY (author_id) REFERENCES dbo.users(id)
);
GO

-- ------------------------------------------------------------------------------
-- 8. Table: dbo.discussion_replies
-- ------------------------------------------------------------------------------
CREATE TABLE dbo.discussion_replies (
    id               BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    discussion_id    BIGINT NOT NULL,
    author_id        BIGINT NOT NULL,
    content          TEXT NOT NULL,
    created_at       DATETIME2 NULL,
    CONSTRAINT fk_discussion_replies_discussion FOREIGN KEY (discussion_id) REFERENCES dbo.discussions(id) ON DELETE CASCADE,
    CONSTRAINT fk_discussion_replies_author FOREIGN KEY (author_id) REFERENCES dbo.users(id)
);
GO

-- ------------------------------------------------------------------------------
-- 9. Table: dbo.discussion_upvotes
-- ------------------------------------------------------------------------------
CREATE TABLE dbo.discussion_upvotes (
    id               BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    discussion_id    BIGINT NOT NULL,
    user_id          BIGINT NOT NULL,
    created_at       DATETIME2 NULL,
    CONSTRAINT uk_discussion_upvotes UNIQUE (discussion_id, user_id),
    CONSTRAINT fk_discussion_upvotes_discussion FOREIGN KEY (discussion_id) REFERENCES dbo.discussions(id) ON DELETE CASCADE,
    CONSTRAINT fk_discussion_upvotes_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);
GO

-- ------------------------------------------------------------------------------
-- 10. Table: dbo.community_reports
-- ------------------------------------------------------------------------------
CREATE TABLE dbo.community_reports (
    id               BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    report_type      VARCHAR(20) NOT NULL, -- 'DISCUSSION' or 'REPLY'
    target_id        BIGINT NOT NULL,
    reporter_id      BIGINT NOT NULL,
    reason           VARCHAR(100) NOT NULL,
    details          TEXT NULL,
    status           VARCHAR(20) DEFAULT 'PENDING' NOT NULL, -- 'PENDING', 'DISMISSED', 'RESOLVED'
    created_at       DATETIME2 NULL,
    CONSTRAINT fk_community_reports_user FOREIGN KEY (reporter_id) REFERENCES dbo.users(id)
);
GO

-- ==============================================================================
-- SEED DATA
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- A. Users: 1 admin account + 20 virtual learner accounts
-- Login for the admin account : alex@codeforge.io / password123
-- Login for every learner account : <email below> / Learner@123
-- (Both hashes below are real BCrypt hashes, verified to match these plaintext
--  passwords -- the placeholder hash that used to ship in data.sql did NOT
--  actually match "password123", so logging in with it would have failed.)
-- ------------------------------------------------------------------------------
SET IDENTITY_INSERT dbo.users ON;

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (1, 'alex_morgan', 'alex@codeforge.io', '$2b$10$a3DRjdEfncUYteAIU7dnzeJNr4xOWznGQTjPWIwi4Y54PWj8PzXQK', 'Alex Morgan', 'Admin', 7, 48, 76.20, 12.50, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (2, 'david_kim', 'david_kim@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'David Kim', 'Learner', 14, 69, 88.57, 15.04, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (3, 'sarah_jenkins', 'sarah_jenkins@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Sarah Jenkins', 'Learner', 6, 36, 77.55, 19.20, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (4, 'marcus_vance', 'marcus_vance@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Marcus Vance', 'Learner', 15, 58, 71.22, 36.38, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (5, 'priya_natarajan', 'priya_natarajan@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Priya Natarajan', 'Learner', 9, 43, 66.51, 32.66, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (6, 'liam_oconnor', 'liam_oconnor@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Liam O''Connor', 'Learner', 14, 68, 57.17, 36.96, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (7, 'mia_thompson', 'mia_thompson@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Mia Thompson', 'Learner', 5, 57, 84.59, 19.69, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (8, 'daniel_alves', 'daniel_alves@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Daniel Alves', 'Learner', 2, 37, 87.45, 15.44, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (9, 'hana_suzuki', 'hana_suzuki@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Hana Suzuki', 'Learner', 6, 55, 51.88, 36.74, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (10, 'omar_haddad', 'omar_haddad@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Omar Haddad', 'Learner', 2, 3, 66.21, 22.32, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (11, 'isabella_rossi', 'isabella_rossi@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Isabella Rossi', 'Learner', 2, 28, 56.65, 25.22, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (12, 'ethan_walker', 'ethan_walker@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Ethan Walker', 'Learner', 17, 37, 76.42, 25.78, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (13, 'chloe_martin', 'chloe_martin@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Chloe Martin', 'Learner', 4, 3, 59.73, 23.74, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (14, 'noah_becker', 'noah_becker@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Noah Becker', 'Learner', 15, 20, 70.65, 19.45, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (15, 'grace_lee', 'grace_lee@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Grace Lee', 'Learner', 4, 67, 58.66, 17.29, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (16, 'lucas_ferreira', 'lucas_ferreira@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Lucas Ferreira', 'Learner', 5, 20, 81.82, 36.69, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (17, 'emma_novak', 'emma_novak@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Emma Novak', 'Learner', 9, 67, 64.37, 32.51, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (18, 'ryan_bennett', 'ryan_bennett@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Ryan Bennett', 'Learner', 16, 28, 62.67, 38.39, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (19, 'sofia_petrova', 'sofia_petrova@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Sofia Petrova', 'Learner', 12, 12, 47.41, 14.53, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (20, 'aiden_murphy', 'aiden_murphy@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Aiden Murphy', 'Learner', 11, 6, 46.20, 19.56, CURRENT_TIMESTAMP);

INSERT INTO dbo.users (id, username, email, password_hash, full_name, role, streak_days, solved_count, acceptance_rate, practice_time_hours, created_at)
VALUES (21, 'zoe_carter', 'zoe_carter@codeforge.io', '$2b$10$mz5NqrTsIc9EH9Or.GWpjedbmIbzjQAf9n924nQIwWJ4Hn9B9Umuu', 'Zoe Carter', 'Learner', 21, 41, 66.16, 36.64, CURRENT_TIMESTAMP);

SET IDENTITY_INSERT dbo.users OFF;
GO


-- ------------------------------------------------------------------------------
-- B. Exercises: the full 100-problem catalog (id 1-100)
-- author_id is randomly assigned to one of the 20 learner accounts seeded above.
-- moderation_status = 'APPROVED' for all of them (they are the public catalog).
-- ------------------------------------------------------------------------------

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, starter_code_python, starter_code_javascript, starter_code_cpp, starter_code_java, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (1, 'Pair Sum', 'Easy', 'Arrays, Hash map', 78.40, 'Given an integer array `nums` and an integer `target`, return the indices of the two numbers that add up to `target`.

You may assume that each input has exactly one solution. You may not use the same element twice.

Return the answer in any order.', '2 <= nums.length <= 10^4
-10^9 <= nums[i], target <= 10^9
Exactly one valid answer exists.', 'def pair_sum(nums: list[int], target: int) -> list[int]:
    # Remember each value and its index.
    seen = {}

    for i, num in enumerate(nums):
        complement = target - num

        if complement in seen:
            return [seen[complement], i]

        seen[num] = i

    return []', 'function pairSum(nums, target) {
    const seen = new Map();
    for (let i = 0; i < nums.length; i++) {
        const complement = target - nums[i];
        if (seen.has(complement)) {
            return [seen.get(complement), i];
        }
        seen.set(nums[i], i);
    }
    return [];
}', 'class Solution {
public:
    vector<int> pairSum(vector<int>& nums, int target) {
        unordered_map<int, int> seen;
        for (int i = 0; i < nums.size(); ++i) {
            int complement = target - nums[i];
            if (seen.find(complement) != seen.end()) {
                return {seen[complement], i};
            }
            seen[nums[i]] = i;
        }
        return {};
    }
};', 'class Solution {
    public int[] pairSum(int[] nums, int target) {
        Map<Integer, Integer> seen = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (seen.containsKey(complement)) {
                return new int[] { seen.get(complement), i };
            }
            seen.put(nums[i], i);
        }
        return new int[0];
    }
}', 1000, 256, 1, 5, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (2, 'Contains Duplicate', 'Easy', 'Arrays, Hash set', 68.9, 'Given an integer array nums, return true if any value appears at least twice in the array, and false if every element is distinct.', '1 <= nums.length <= 10^5
-10^9 <= nums[i] <= 10^9', 1000, 256, 1, 12, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (3, 'Group Anagrams', 'Medium', 'Strings, Sorting', 66.4, 'Given an array of strings, group the strings that are anagrams of one another and return the groups in any order.', '1 <= strs.length <= 10^4', 1000, 256, 1, 18, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (4, 'Valid Anagram', 'Easy', 'Strings, Hash map', 73.0, 'Given two strings s and t, return true if t is an anagram of s (built from exactly the same letters, with the same frequency), and false otherwise.', '1 <= s.length, t.length <= 5*10^4
Lowercase English letters only.', 1000, 256, 1, 18, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (5, 'Top K Frequent Elements', 'Medium', 'Arrays, Heap', 56.1, 'Given an integer array and an integer k, return the k most frequent elements, in any order.', '1 <= nums.length <= 10^5', 1000, 256, 1, 5, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (6, 'Product of Array Except Self', 'Medium', 'Arrays, Prefix sum', 49.4, 'Given an integer array nums, return an array answer such that answer[i] equals the product of every element of nums except nums[i], without using division and in O(n) time.', '2 <= nums.length <= 10^5', 1000, 256, 1, 9, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (7, 'Trapping Rain Water', 'Hard', 'Arrays, Two pointers', 31.0, 'Given n non-negative integers describing an elevation map where every bar has width 1, compute how much water the terrain can trap after it rains.', '1 <= height.length <= 2*10^4', 1000, 256, 1, 21, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (8, 'Valid Parentheses', 'Easy', 'Strings, Stacks', 66.9, 'Given a string containing only the characters ''('', '')'', ''{'', ''}'', ''['' and '']'', determine if every opening bracket is closed by a bracket of the same type in the correct order.', '1 <= s.length <= 10^4', 1000, 256, 1, 21, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (9, 'Longest Consecutive Sequence', 'Medium', 'Arrays, Hash set', 51.9, 'Given an unsorted array of integers, return the length of the longest run of consecutive integers that appear in the array, in O(n) time.', '0 <= nums.length <= 10^5', 1000, 256, 1, 19, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (10, 'Binary Search', 'Easy', 'Arrays, Binary search', 70.0, 'Given a sorted array of distinct integers and a target value, return the index of the target if it exists in the array, otherwise return -1. Your solution must run in O(log n) time.', '1 <= nums.length <= 10^4
nums is sorted in ascending order with distinct values.', 1000, 256, 1, 15, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (11, '3Sum', 'Medium', 'Arrays, Two pointers', 45.9, 'Given an integer array nums, return all unique triplets [nums[i], nums[j], nums[k]] with distinct indices whose values sum to 0. The result must not contain duplicate triplets.', '3 <= nums.length <= 3000', 1000, 256, 1, 20, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (12, 'Container With Most Water', 'Medium', 'Arrays, Two pointers', 47.8, 'Given n non-negative integers describing vertical line heights, choose two lines that together with the x-axis form a container, and return the maximum amount of water it can hold.', '2 <= height.length <= 10^5', 1000, 256, 1, 19, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (13, 'Minimum Window Substring', 'Hard', 'Strings, Sliding window', 43.8, 'Given two strings s and t, return the shortest substring of s that contains every character of t, including duplicates, or an empty string if no such window exists.', '1 <= s.length, t.length <= 10^5', 1000, 256, 1, 17, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (14, 'Best Time to Buy and Sell Stock', 'Easy', 'Arrays, Greedy', 84.9, 'Given an array prices where prices[i] is the price of a share on day i, choose one day to buy and a later day to sell to maximize profit. Return the maximum achievable profit, or 0 if no profit is possible.', '1 <= prices.length <= 10^5
0 <= prices[i] <= 10^4', 1000, 256, 1, 20, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (15, 'Two Sum II - Sorted Input', 'Medium', 'Arrays, Two pointers', 42.6, 'Given a 1-indexed array already sorted in non-decreasing order, find two numbers that add up to a target value and return their indices (1-indexed) as an array of length two.', '2 <= numbers.length <= 3*10^4', 1000, 256, 1, 16, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (16, 'Valid Palindrome', 'Easy', 'Strings, Two pointers', 74.5, 'Given a string, determine whether it reads the same forwards and backwards after converting all letters to lowercase and removing every character that is not a letter or digit.', '1 <= s.length <= 2*10^5', 1000, 256, 1, 9, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (17, 'Longest Substring Without Repeating Characters', 'Medium', 'Strings, Sliding window', 52.4, 'Given a string, find the length of the longest substring that does not contain any repeated character.', '0 <= s.length <= 5*10^4', 1000, 256, 1, 2, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (18, 'Longest Repeating Character Replacement', 'Medium', 'Strings, Sliding window', 52.6, 'Given a string of uppercase letters and an integer k, you may change up to k characters in the string to any other letter. Return the length of the longest substring you can make consist of a single repeated letter.', '1 <= s.length <= 10^5', 1000, 256, 1, 21, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (19, 'Sliding Window Maximum', 'Hard', 'Arrays, Sliding window', 31.7, 'Given an array and a window of size k sliding from the very left to the very right, return an array holding the maximum value inside the window at every position.', '1 <= nums.length <= 10^5', 1000, 256, 1, 4, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (20, 'Reverse String', 'Easy', 'Strings, Two pointers', 85.5, 'Write a function that reverses an array of characters in place, using O(1) extra memory.', '1 <= s.length <= 10^5', 1000, 256, 1, 5, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (21, 'Permutation in String', 'Medium', 'Strings, Sliding window', 44.5, 'Given two strings s1 and s2, return true if s2 contains a contiguous substring that is a permutation of s1.', '1 <= s1.length, s2.length <= 10^4', 1000, 256, 1, 11, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (22, 'Merge Sorted Array', 'Easy', 'Arrays, Two pointers', 73.0, 'You are given two integer arrays nums1 and nums2, both sorted in non-decreasing order, where nums1 has extra trailing space to hold the result. Merge nums2 into nums1 in place as one sorted array.', 'nums1.length == m + n
nums2.length == n', 1000, 256, 1, 5, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (23, 'Search in Rotated Sorted Array', 'Medium', 'Arrays, Binary search', 57.6, 'Given an ascending array of distinct integers that has been rotated at an unknown pivot, and a target value, return the index of the target in O(log n) time, or -1 if it is not present.', '1 <= nums.length <= 5000', 1000, 256, 1, 16, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (24, 'Find Minimum in Rotated Sorted Array', 'Medium', 'Arrays, Binary search', 43.9, 'Given a rotated sorted array of unique elements, find the minimum element in O(log n) time.', '1 <= nums.length <= 5000', 1000, 256, 1, 2, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (25, 'Median of Two Sorted Arrays', 'Hard', 'Arrays, Binary search', 49.8, 'Given two sorted arrays of sizes m and n, return the median of the two arrays combined, with an overall run time of O(log(m + n)).', '0 <= m, n <= 1000', 1000, 256, 1, 17, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (26, 'Move Zeroes', 'Easy', 'Arrays, Two pointers', 78.7, 'Given an integer array, move every 0 to the end of the array while keeping the relative order of the non-zero elements, and do it in place without copying the array.', '1 <= nums.length <= 10^4', 1000, 256, 1, 12, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (27, 'Koko Eating Bananas', 'Medium', 'Arrays, Binary search', 67.6, 'Koko has piles of bananas and h hours to eat them all; each hour she picks one pile and eats up to k bananas from it. Return the minimum integer eating speed k that lets her finish every pile within h hours.', '1 <= piles.length <= 10^4', 1000, 256, 1, 8, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (28, 'Majority Element', 'Easy', 'Arrays, Hash map', 76.0, 'Given an array of size n, return the majority element — the element that appears more than floor(n / 2) times. You may assume a majority element always exists.', '1 <= nums.length <= 5*10^4', 1000, 256, 1, 14, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (29, 'Time Based Key-Value Store', 'Medium', 'Hash map, Binary search', 45.2, 'Design a time-based key-value store that can store several values for the same key at different timestamps, and retrieve the value for a key at (or just before) a requested timestamp.', 'At most 2*10^5 calls total.', 1000, 256, 1, 10, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (30, 'Daily Temperatures', 'Medium', 'Arrays, Stacks', 45.7, 'Given a list of daily temperatures, return an array where answer[i] is the number of days you must wait after day i for a warmer temperature, or 0 if no such day exists.', '1 <= temperatures.length <= 10^5', 1000, 256, 1, 13, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (31, 'Largest Rectangle in Histogram', 'Hard', 'Arrays, Stacks', 44.4, 'Given an array of bar heights where each bar has width 1, find the area of the largest rectangle that fits entirely inside the histogram.', '1 <= heights.length <= 10^5', 1000, 256, 1, 13, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (32, 'Single Number', 'Easy', 'Arrays, Bit manipulation', 76.3, 'Given a non-empty array where every element appears exactly twice except for one, find that single element using O(n) time and O(1) extra space.', '1 <= nums.length <= 3*10^4', 1000, 256, 1, 14, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (33, 'Car Fleet', 'Medium', 'Arrays, Stacks', 58.8, 'Several cars drive toward the same destination on a one-lane road. A car that catches up to a slower car ahead joins it as one fleet travelling at the slower speed. Return the number of fleets that will arrive.', '1 <= position.length == speed.length <= 10^5', 1000, 256, 1, 18, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (34, 'Missing Number', 'Easy', 'Arrays, Math', 73.6, 'Given an array containing n distinct numbers taken from the range [0, n], find the one number in that range that is missing from the array.', '1 <= n <= 10^4', 1000, 256, 1, 4, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (35, 'Generate Parentheses', 'Medium', 'Strings, Backtracking', 60.8, 'Given n pairs of parentheses, generate every combination of well-formed (balanced) parentheses that can be made with them.', '1 <= n <= 8', 1000, 256, 1, 12, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (36, 'Evaluate Reverse Polish Notation', 'Medium', 'Stacks, Math', 58.0, 'Evaluate an arithmetic expression written in Reverse Polish (postfix) notation, where the valid operators are +, -, *, and /.', '1 <= tokens.length <= 10^4', 1000, 256, 1, 4, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (37, 'Merge k Sorted Lists', 'Hard', 'Linked list, Heap', 42.7, 'Given an array of k sorted linked lists, merge all of them into a single sorted linked list and return its head.', '0 <= k <= 10^4', 1000, 256, 1, 19, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (38, 'Intersection of Two Arrays', 'Easy', 'Arrays, Hash set', 84.6, 'Given two integer arrays, return an array of their intersection, where each element in the result must appear only once and the order does not matter.', '1 <= nums1.length, nums2.length <= 1000', 1000, 256, 1, 19, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (39, 'Maximum Subarray', 'Medium', 'Arrays, Dynamic programming', 67.6, 'Given an integer array, find the contiguous subarray with at least one number that has the largest sum, and return that sum.', '1 <= nums.length <= 10^5', 1000, 256, 1, 11, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (40, 'First Unique Character in a String', 'Easy', 'Strings, Hash map', 82.3, 'Given a lowercase string, find the index of the first character that does not repeat anywhere else in the string. Return -1 if no such character exists.', '1 <= s.length <= 10^5', 1000, 256, 1, 11, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (41, 'Jump Game', 'Medium', 'Arrays, Greedy', 56.2, 'Given an array where nums[i] is the furthest you may jump from index i, starting at index 0, determine whether you can reach the final index.', '1 <= nums.length <= 10^4', 1000, 256, 1, 16, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (42, 'Jump Game II', 'Medium', 'Arrays, Greedy', 45.6, 'Using the same jump-length array as in Jump Game, return the minimum number of jumps required to reach the last index (it is guaranteed to be reachable).', '1 <= nums.length <= 10^4', 1000, 256, 1, 6, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (43, 'Reverse Nodes in k-Group', 'Hard', 'Linked list, Recursion', 26.6, 'Given the head of a linked list, reverse the nodes k at a time and return the resulting list; if fewer than k nodes remain at the end, leave them unchanged.', '1 <= number of nodes <= 5000', 1000, 256, 1, 20, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (44, 'Ransom Note', 'Easy', 'Strings, Hash map', 82.1, 'Given two strings ransomNote and magazine, return true if ransomNote can be built entirely out of the letters available in magazine, where each letter in magazine can be used at most once.', '1 <= ransomNote.length, magazine.length <= 10^5', 1000, 256, 1, 11, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (45, 'Merge Intervals', 'Medium', 'Intervals, Sorting', 45.4, 'Given a collection of intervals, merge every pair of overlapping intervals and return the resulting set of non-overlapping intervals that covers all the input ranges.', '1 <= intervals.length <= 10^4', 1000, 256, 1, 2, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (46, 'Isomorphic Strings', 'Easy', 'Strings, Hash map', 65.0, 'Given two strings s and t of the same length, determine whether the characters in s can be consistently replaced to obtain t, where no two different characters map to the same character and order is preserved.', '1 <= s.length <= 5*10^4
s.length == t.length', 1000, 256, 1, 13, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (47, 'Insert Interval', 'Medium', 'Intervals, Arrays', 65.9, 'Given a set of non-overlapping intervals sorted by start time and a new interval, insert the new interval, merging where necessary so the intervals stay sorted and non-overlapping.', '0 <= intervals.length <= 10^4', 1000, 256, 1, 13, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (48, 'Non-overlapping Intervals', 'Medium', 'Intervals, Greedy', 49.8, 'Given an array of intervals, find the minimum number of intervals that must be removed so that the remaining intervals do not overlap.', '1 <= intervals.length <= 10^5', 1000, 256, 1, 16, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (49, 'Word Ladder', 'Hard', 'Graphs, Queues', 36.2, 'Given a beginWord, an endWord, and a word list, return the number of words in the shortest transformation sequence from beginWord to endWord, changing one letter at a time, where every intermediate word must exist in the word list. Return 0 if no such sequence exists.', '1 <= beginWord.length <= 10', 1000, 256, 1, 15, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (50, 'Roman to Integer', 'Easy', 'Strings, Math', 67.2, 'Given a string representing a roman numeral, convert it to its integer value.', '1 <= s.length <= 15
Value is between 1 and 3999.', 1000, 256, 1, 4, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (51, 'Subsets', 'Medium', 'Backtracking, Arrays', 43.7, 'Given an array of unique integers, return every possible subset of the array (the power set), without any duplicates.', '1 <= nums.length <= 10', 1000, 256, 1, 14, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (52, 'Longest Common Prefix', 'Easy', 'Strings, Arrays', 70.4, 'Given an array of strings, find the longest string that is a prefix of every string in the array. Return an empty string if there is no common prefix.', '1 <= strs.length <= 200', 1000, 256, 1, 20, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (53, 'Permutations', 'Medium', 'Backtracking, Arrays', 66.7, 'Given an array of distinct integers, return every possible ordering (permutation) of the array, in any order.', '1 <= nums.length <= 6', 1000, 256, 1, 19, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (54, 'Combination Sum', 'Medium', 'Backtracking, Arrays', 54.6, 'Given an array of distinct positive integers and a target value, return every unique combination of numbers (each number may be reused any number of times) that sums exactly to the target.', '1 <= candidates.length <= 30', 1000, 256, 1, 17, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (55, 'Longest Valid Parentheses', 'Hard', 'Strings, Dynamic programming', 43.7, 'Given a string containing only ''('' and '')'', find the length of the longest contiguous substring that forms a valid, well-formed sequence of parentheses.', '0 <= s.length <= 3*10^4', 1000, 256, 1, 5, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (56, 'Plus One', 'Easy', 'Arrays, Math', 79.8, 'Given a large non-negative integer represented as an array of digits (most significant digit first), add one to the number and return the resulting array of digits.', '1 <= digits.length <= 100', 1000, 256, 1, 15, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (57, 'Word Search', 'Medium', 'Matrix, Backtracking', 53.8, 'Given an m x n grid of letters and a word, determine whether the word can be traced through horizontally or vertically adjacent cells, without reusing the same cell twice.', 'Grid up to 6 x 6.', 1000, 256, 1, 18, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (58, 'Remove Duplicates from Sorted Array', 'Easy', 'Arrays, Two pointers', 80.5, 'Given an array sorted in non-decreasing order, remove the duplicates in place so each unique value appears only once (kept at the front of the array), and return the count of unique values.', '1 <= nums.length <= 3*10^4', 1000, 256, 1, 21, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (59, 'Number of Islands', 'Medium', 'Graphs, Matrix', 51.8, 'Given an m x n grid of ''1'' (land) and ''0'' (water), return the number of islands — groups of land cells connected horizontally or vertically.', '1 <= m, n <= 300', 1000, 256, 1, 17, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (60, 'Clone Graph', 'Medium', 'Graphs, Hash map', 67.6, 'Given a reference node of a connected undirected graph, produce a complete deep copy of the graph and return the corresponding reference node in the copy.', '0 <= number of nodes <= 100', 1000, 256, 1, 14, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (61, 'Edit Distance', 'Hard', 'Dynamic programming, Strings', 32.6, 'Given two strings word1 and word2, return the minimum number of single-character insert, delete, or replace operations needed to turn word1 into word2.', '0 <= word1.length, word2.length <= 500', 1000, 256, 1, 18, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (62, 'Climbing Stairs', 'Easy', 'Dynamic programming, Math', 68.4, 'You are climbing a staircase of n steps. Each move you may climb 1 or 2 steps. Return the number of distinct ways to reach the top.', '1 <= n <= 45', 1000, 256, 1, 10, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (63, 'Course Schedule', 'Medium', 'Graphs, Topological sort', 61.8, 'Given numCourses and a list of prerequisite pairs [a, b] meaning course b must be taken before course a, determine whether it is possible to finish all of the courses.', '1 <= numCourses <= 2000', 1000, 256, 1, 15, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (64, 'Fizz Buzz', 'Easy', 'Math, Strings', 67.9, 'Given an integer n, return a string array answer of length n where, for every i from 1 to n, answer[i-1] is "FizzBuzz" when i is divisible by both 3 and 5, "Fizz" when divisible by 3 only, "Buzz" when divisible by 5 only, and the decimal string of i otherwise.', '1 <= n <= 10^4', 1000, 256, 1, 20, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (65, 'Pacific Atlantic Water Flow', 'Medium', 'Graphs, Matrix', 63.8, 'Given an m x n grid of heights for a continent bordered by the Pacific (top/left edges) and the Atlantic (bottom/right edges), return every cell from which water can reach both oceans, flowing only to neighbors of equal or lower height.', '1 <= m, n <= 200', 1000, 256, 1, 17, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (66, 'Rotting Oranges', 'Medium', 'Graphs, Queues', 58.8, 'Given a grid where each cell holds 0 (empty), 1 (fresh orange), or 2 (rotten orange), every minute each fresh orange adjacent to a rotten one becomes rotten too. Return the minimum number of minutes until no fresh orange remains, or -1 if that is impossible.', '1 <= m, n <= 10', 1000, 256, 1, 18, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (67, 'Regular Expression Matching', 'Hard', 'Dynamic programming, Strings', 49.7, 'Implement regular expression matching that supports ''.'' (any single character) and ''*'' (zero or more of the preceding element), and decide whether a pattern matches an entire input string.', '1 <= s.length <= 20
1 <= p.length <= 30', 1000, 256, 1, 18, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (68, 'Reverse Linked List', 'Easy', 'Linked list, Recursion', 85.1, 'Given the head of a singly linked list, reverse the list and return the new head.', '0 <= number of nodes <= 5000', 1000, 256, 1, 2, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (69, 'Add Two Numbers', 'Medium', 'Linked list, Math', 66.7, 'Two non-empty linked lists represent two non-negative integers, each digit stored in reverse order. Add the two numbers together and return the sum as a linked list, also in reverse-digit order.', '1 <= number of nodes in each list <= 100', 1000, 256, 1, 20, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (70, 'Merge Two Sorted Lists', 'Easy', 'Linked list, Recursion', 76.3, 'You are given the heads of two sorted linked lists. Merge them into one sorted list by splicing the existing nodes together, and return the head of the merged list.', '0 <= number of nodes in each list <= 50', 1000, 256, 1, 9, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (71, 'Remove Nth Node From End of List', 'Medium', 'Linked list, Two pointers', 42.2, 'Given the head of a linked list, remove the nth node counted from the end of the list in a single pass, and return the head.', '1 <= number of nodes <= 30', 1000, 256, 1, 6, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (72, 'Reorder List', 'Medium', 'Linked list, Two pointers', 51.2, 'Given the head of a singly linked list L0 → L1 → ... → Ln, reorder it in place into L0 → Ln → L1 → Ln-1 → L2 → Ln-2 → ... without changing any node''s value.', '1 <= number of nodes <= 5*10^4', 1000, 256, 1, 3, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (73, 'N-Queens', 'Hard', 'Backtracking, Matrix', 49.3, 'Given an integer n, place n queens on an n x n chessboard so that no two queens attack each other, and return every distinct board arrangement that achieves this.', '1 <= n <= 9', 1000, 256, 1, 18, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (74, 'Linked List Cycle', 'Easy', 'Linked list, Two pointers', 77.1, 'Given the head of a linked list, determine whether the list contains a cycle, using O(1) extra memory.', '0 <= number of nodes <= 10^4', 1000, 256, 1, 5, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (75, 'Copy List with Random Pointer', 'Medium', 'Linked list, Hash map', 48.6, 'Given a linked list where each node also has a random pointer that may point to any node in the list or to null, construct and return a complete deep copy of the list.', '0 <= number of nodes <= 1000', 1000, 256, 1, 21, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (76, 'Maximum Depth of Binary Tree', 'Easy', 'Trees, Recursion', 63.5, 'Given the root of a binary tree, return its maximum depth — the number of nodes along the longest path from the root down to the farthest leaf.', '0 <= number of nodes <= 10^4', 1000, 256, 1, 15, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (77, 'Lowest Common Ancestor of a BST', 'Medium', 'Trees, Recursion', 63.9, 'Given a binary search tree and two of its nodes p and q, find their lowest common ancestor — the deepest node that has both p and q among its descendants.', '2 <= number of nodes <= 10^5', 1000, 256, 1, 17, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (78, 'Binary Tree Level Order Traversal', 'Medium', 'Trees, Queues', 48.4, 'Given the root of a binary tree, return the values of its nodes grouped level by level, from left to right.', '0 <= number of nodes <= 2000', 1000, 256, 1, 6, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (79, 'Binary Tree Maximum Path Sum', 'Hard', 'Trees, Recursion', 50.9, 'Given the root of a binary tree, return the maximum sum along any path between two nodes in the tree (the path does not need to pass through the root).', '1 <= number of nodes <= 3*10^4', 1000, 256, 1, 17, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (80, 'Invert Binary Tree', 'Easy', 'Trees, Recursion', 71.0, 'Given the root of a binary tree, invert the tree by swapping the left and right child of every node, and return the new root.', '0 <= number of nodes <= 100', 1000, 256, 1, 9, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (81, 'Validate Binary Search Tree', 'Medium', 'Trees, Recursion', 67.8, 'Given the root of a binary tree, determine whether it is a valid binary search tree, where every node''s left subtree contains only smaller values and its right subtree only larger values.', '1 <= number of nodes <= 10^4', 1000, 256, 1, 5, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (82, 'Symmetric Tree', 'Easy', 'Trees, Recursion', 62.7, 'Given the root of a binary tree, check whether it is a mirror of itself around its center.', '1 <= number of nodes <= 1000', 1000, 256, 1, 16, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (83, 'Kth Smallest Element in a BST', 'Medium', 'Trees, Recursion', 43.5, 'Given the root of a binary search tree and an integer k, return the kth smallest value stored in the tree (1-indexed).', '1 <= number of nodes <= 10^4', 1000, 256, 1, 17, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (84, 'Coin Change', 'Medium', 'Dynamic programming, Arrays', 62.0, 'Given an array of coin denominations and a target amount, return the fewest number of coins needed to make up that amount, or -1 if it cannot be made.', '1 <= coins.length <= 12', 1000, 256, 1, 19, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (85, 'Serialize and Deserialize Binary Tree', 'Hard', 'Trees, Design', 46.4, 'Design an approach to serialize a binary tree into a single string and later deserialize that string back into the exact same tree structure.', '0 <= number of nodes <= 10^4', 1000, 256, 1, 10, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (86, 'Min Stack', 'Easy', 'Stacks, Design', 68.3, 'Design a stack that supports push, pop, top, and retrieving the minimum element, all in O(1) time.', 'At most 3*10^4 calls total.', 1000, 256, 1, 19, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (87, 'House Robber', 'Medium', 'Dynamic programming, Arrays', 60.1, 'Given the amount of money stashed in each house along a street, where robbing two adjacent houses triggers an alarm, return the maximum amount you can rob without robbing two adjacent houses.', '1 <= nums.length <= 100', 1000, 256, 1, 14, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (88, 'Implement Queue using Stacks', 'Easy', 'Stacks, Design', 63.9, 'Implement a first-in-first-out queue using only two stacks, supporting push, pop, peek, and empty.', 'At most 100 calls total.', 1000, 256, 1, 11, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (89, 'Longest Increasing Subsequence', 'Medium', 'Dynamic programming, Binary search', 43.1, 'Given an integer array, return the length of its longest strictly increasing subsequence (the chosen elements do not need to be contiguous).', '1 <= nums.length <= 2500', 1000, 256, 1, 19, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (90, 'Unique Paths', 'Medium', 'Dynamic programming, Math', 48.8, 'A robot starts at the top-left corner of an m x n grid and can only move down or right. Return the number of distinct paths to the bottom-right corner.', '1 <= m, n <= 100', 1000, 256, 1, 18, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (91, 'Find Median from Data Stream', 'Hard', 'Heap, Design', 47.9, 'Design a data structure that can add numbers from a stream one at a time and return the median of every number added so far, at any point in the stream.', 'At most 5*10^4 calls total.', 1000, 256, 1, 17, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (92, 'Happy Number', 'Easy', 'Math, Hash set', 76.0, 'A number is "happy" if repeatedly replacing it with the sum of the squares of its digits eventually reaches 1. Determine whether a given positive integer is happy.', '1 <= n <= 2^31 - 1', 1000, 256, 1, 14, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (93, 'Decode Ways', 'Medium', 'Dynamic programming, Strings', 59.6, 'A message made only of digits can be decoded into letters using the mapping ''A'' -> "1", ..., ''Z'' -> "26". Given such a digit string, return the total number of ways it can be decoded.', '1 <= s.length <= 100', 1000, 256, 1, 15, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (94, 'Power of Two', 'Easy', 'Math, Bit manipulation', 77.8, 'Given an integer n, return true if there exists an integer x such that n == 2^x.', '-2^31 <= n <= 2^31 - 1', 1000, 256, 1, 8, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (95, 'Word Break', 'Medium', 'Dynamic programming, Strings', 53.5, 'Given a string s and a dictionary of words wordDict, return true if s can be split into a sequence of one or more dictionary words, where words may be reused.', '1 <= s.length <= 300', 1000, 256, 1, 11, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (96, 'Partition Equal Subset Sum', 'Medium', 'Dynamic programming, Arrays', 57.0, 'Given an array of positive integers, determine whether it can be split into two subsets whose sums are equal.', '1 <= nums.length <= 200', 1000, 256, 1, 10, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (97, 'Alien Dictionary', 'Hard', 'Graphs, Topological sort', 33.6, 'Given a list of words that are sorted according to the rules of an unknown alien alphabet, work out one valid ordering of that alphabet''s letters. Return an empty string if no valid ordering exists.', '1 <= words.length <= 100', 1000, 256, 1, 2, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (98, 'Pascal''s Triangle', 'Easy', 'Arrays, Dynamic programming', 77.4, 'Given an integer numRows, return the first numRows rows of Pascal''s triangle, where every number is the sum of the two numbers directly above it.', '1 <= numRows <= 30', 1000, 256, 1, 3, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (99, 'K Closest Points to Origin', 'Medium', 'Heap, Sorting', 51.8, 'Given an array of points on the X-Y plane and an integer k, return the k points closest to the origin, in any order, using Euclidean distance.', '1 <= k <= points.length <= 10^4', 1000, 256, 1, 6, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO dbo.exercises (id, title, difficulty, topics, acceptance_rate, description, constraints, time_limit_ms, memory_limit_mb, is_custom, author_id, moderation_status, created_at)
VALUES (100, 'Kth Largest Element in an Array', 'Medium', 'Heap, Sorting', 67.9, 'Given an unsorted integer array, return the kth largest element in sorted order (not the kth distinct element).', '1 <= k <= nums.length <= 10^5', 1000, 256, 1, 21, 'APPROVED', CURRENT_TIMESTAMP);

GO


-- ------------------------------------------------------------------------------
-- C. Sample test cases (shown in the IDE's test console)
-- ------------------------------------------------------------------------------

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (1, 'Case 1', 'nums = [2,7,11,15], target = 9', '[0, 1]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (1, 'Case 2', 'nums = [3,2,4], target = 6', '[1, 2]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (2, 'Case 1', 'nums = [1,2,3,1]', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (3, 'Case 1', 'strs = ["eat","tea","tan","ate","nat","bat"]', '[["bat"],["nat","tan"],["ate","eat","tea"]]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (4, 'Case 1', 's = "anagram", t = "nagaram"', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (5, 'Case 1', 'nums = [1,1,1,2,2,3], k = 2', '[1,2]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (6, 'Case 1', 'nums = [1,2,3,4]', '[24,12,8,6]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (7, 'Case 1', 'height = [0,1,0,2,1,0,1,3,2,1,2,1]', '6', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (8, 'Case 1', 's = "()[]{}"', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (9, 'Case 1', 'nums = [100,4,200,1,3,2]', '4', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (10, 'Case 1', 'nums = [-1,0,3,5,9,12], target = 9', '4', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (11, 'Case 1', 'nums = [-1,0,1,2,-1,-4]', '[[-1,-1,2],[-1,0,1]]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (12, 'Case 1', 'height = [1,8,6,2,5,4,8,3,7]', '49', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (13, 'Case 1', 's = "ADOBECODEBANC", t = "ABC"', '"BANC"', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (14, 'Case 1', 'prices = [7,1,5,3,6,4]', '5', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (15, 'Case 1', 'numbers = [2,7,11,15], target = 9', '[1,2]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (16, 'Case 1', 's = "A man, a plan, a canal: Panama"', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (17, 'Case 1', 's = "abcabcbb"', '3', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (18, 'Case 1', 's = "AABABBA", k = 1', '4', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (19, 'Case 1', 'nums = [1,3,-1,-3,5,3,6,7], k = 3', '[3,3,5,5,6,7]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (20, 'Case 1', 's = ["h","e","l","l","o"]', '["o","l","l","e","h"]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (21, 'Case 1', 's1 = "ab", s2 = "eidbaooo"', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (22, 'Case 1', 'nums1 = [1,2,3,0,0,0], m = 3, nums2 = [2,5,6], n = 3', '[1,2,2,3,5,6]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (23, 'Case 1', 'nums = [4,5,6,7,0,1,2], target = 0', '4', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (24, 'Case 1', 'nums = [3,4,5,1,2]', '1', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (25, 'Case 1', 'nums1 = [1,3], nums2 = [2]', '2.0', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (26, 'Case 1', 'nums = [0,1,0,3,12]', '[1,3,12,0,0]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (27, 'Case 1', 'piles = [3,6,7,11], h = 8', '4', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (28, 'Case 1', 'nums = [2,2,1,1,1,2,2]', '2', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (29, 'Case 1', 'set("foo","bar",1); get("foo",1); get("foo",3)', 'null, "bar", "bar"', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (30, 'Case 1', 'temperatures = [73,74,75,71,69,72,76,73]', '[1,1,4,2,1,1,0,0]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (31, 'Case 1', 'heights = [2,1,5,6,2,3]', '10', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (32, 'Case 1', 'nums = [4,1,2,1,2]', '4', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (33, 'Case 1', 'target = 12, position = [10,8,0,5,3], speed = [2,4,1,1,3]', '3', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (34, 'Case 1', 'nums = [3,0,1]', '2', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (35, 'Case 1', 'n = 3', '["((()))","(()())","(())()","()(())","()()()"]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (36, 'Case 1', 'tokens = ["2","1","+","3","*"]', '9', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (37, 'Case 1', 'lists = [[1,4,5],[1,3,4],[2,6]]', '[1,1,2,3,4,4,5,6]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (38, 'Case 1', 'nums1 = [1,2,2,1], nums2 = [2,2]', '[2]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (39, 'Case 1', 'nums = [-2,1,-3,4,-1,2,1,-5,4]', '6', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (40, 'Case 1', 's = "leetcode"', '0', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (41, 'Case 1', 'nums = [2,3,1,1,4]', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (42, 'Case 1', 'nums = [2,3,1,1,4]', '2', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (43, 'Case 1', 'head = [1,2,3,4,5], k = 2', '[2,1,4,3,5]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (44, 'Case 1', 'ransomNote = "aa", magazine = "aab"', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (45, 'Case 1', 'intervals = [[1,3],[2,6],[8,10],[15,18]]', '[[1,6],[8,10],[15,18]]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (46, 'Case 1', 's = "egg", t = "add"', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (47, 'Case 1', 'intervals = [[1,3],[6,9]], newInterval = [2,5]', '[[1,5],[6,9]]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (48, 'Case 1', 'intervals = [[1,2],[2,3],[3,4],[1,3]]', '1', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (49, 'Case 1', 'beginWord = "hit", endWord = "cog", wordList = ["hot","dot","dog","lot","log","cog"]', '5', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (50, 'Case 1', 's = "LVIII"', '58', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (51, 'Case 1', 'nums = [1,2,3]', '[[],[1],[2],[1,2],[3],[1,3],[2,3],[1,2,3]]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (52, 'Case 1', 'strs = ["flower","flow","flight"]', '"fl"', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (53, 'Case 1', 'nums = [1,2,3]', '[[1,2,3],[1,3,2],[2,1,3],[2,3,1],[3,1,2],[3,2,1]]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (54, 'Case 1', 'candidates = [2,3,6,7], target = 7', '[[2,2,3],[7]]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (55, 'Case 1', 's = ")()())"', '4', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (56, 'Case 1', 'digits = [1,2,3]', '[1,2,4]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (57, 'Case 1', 'board = [["A","B","C","E"],["S","F","C","S"],["A","D","E","E"]], word = "ABCCED"', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (58, 'Case 1', 'nums = [0,0,1,1,1,2,2,3,3,4]', '5, with nums becoming [0,1,2,3,4,...]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (59, 'Case 1', 'grid = [["1","1","0"],["1","1","0"],["0","0","1"]]', '2', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (60, 'Case 1', 'adjList = [[2,4],[1,3],[2,4],[1,3]]', '[[2,4],[1,3],[2,4],[1,3]]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (61, 'Case 1', 'word1 = "horse", word2 = "ros"', '3', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (62, 'Case 1', 'n = 3', '3', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (63, 'Case 1', 'numCourses = 2, prerequisites = [[1,0]]', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (64, 'Case 1', 'n = 5', '["1","2","Fizz","4","Buzz"]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (65, 'Case 1', 'heights = [[1,2,2,3,5],[3,2,3,4,4],[2,4,5,3,1],[6,7,1,4,5],[5,1,1,2,4]]', '[[0,4],[1,3],[1,4],[2,2],[3,0],[3,1],[4,0]]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (66, 'Case 1', 'grid = [[2,1,1],[1,1,0],[0,1,1]]', '4', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (67, 'Case 1', 's = "aa", p = "a*"', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (68, 'Case 1', 'head = [1,2,3,4,5]', '[5,4,3,2,1]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (69, 'Case 1', 'l1 = [2,4,3], l2 = [5,6,4]', '[7,0,8]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (70, 'Case 1', 'list1 = [1,2,4], list2 = [1,3,4]', '[1,1,2,3,4,4]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (71, 'Case 1', 'head = [1,2,3,4,5], n = 2', '[1,2,3,5]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (72, 'Case 1', 'head = [1,2,3,4]', '[1,4,2,3]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (73, 'Case 1', 'n = 4', '[[".Q..","...Q","Q...","..Q."],["..Q.","Q...","...Q",".Q.."]]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (74, 'Case 1', 'head = [3,2,0,-4] with the tail connected back to index 1', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (75, 'Case 1', 'head = [[7,null],[13,0],[11,4],[10,2],[1,0]]', '[[7,null],[13,0],[11,4],[10,2],[1,0]]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (76, 'Case 1', 'root = [3,9,20,null,null,15,7]', '3', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (77, 'Case 1', 'root = [6,2,8,0,4,7,9,null,null,3,5], p = 2, q = 8', '6', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (78, 'Case 1', 'root = [3,9,20,null,null,15,7]', '[[3],[9,20],[15,7]]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (79, 'Case 1', 'root = [-10,9,20,null,null,15,7]', '42', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (80, 'Case 1', 'root = [4,2,7,1,3,6,9]', '[4,7,2,9,6,3,1]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (81, 'Case 1', 'root = [2,1,3]', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (82, 'Case 1', 'root = [1,2,2,3,4,4,3]', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (83, 'Case 1', 'root = [3,1,4,null,2], k = 1', '1', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (84, 'Case 1', 'coins = [1,2,5], amount = 11', '3', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (85, 'Case 1', 'root = [1,2,3,null,null,4,5]', '[1,2,3,null,null,4,5]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (86, 'Case 1', 'push(-2); push(0); push(-3); getMin(); pop(); top(); getMin()', '-3, 0, -2', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (87, 'Case 1', 'nums = [1,2,3,1]', '4', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (88, 'Case 1', 'push(1); push(2); peek(); pop(); empty()', '1, 1, false', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (89, 'Case 1', 'nums = [10,9,2,5,3,7,101,18]', '4', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (90, 'Case 1', 'm = 3, n = 7', '28', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (91, 'Case 1', 'addNum(1); addNum(2); findMedian(); addNum(3); findMedian()', '1.5, 2.0', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (92, 'Case 1', 'n = 19', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (93, 'Case 1', 's = "226"', '3', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (94, 'Case 1', 'n = 16', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (95, 'Case 1', 's = "leetcode", wordDict = ["leet","code"]', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (96, 'Case 1', 'nums = [1,5,11,5]', 'true', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (97, 'Case 1', 'words = ["wrt","wrf","er","ett","rftt"]', '"wertf"', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (98, 'Case 1', 'numRows = 5', '[[1],[1,1],[1,2,1],[1,3,3,1],[1,4,6,4,1]]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (99, 'Case 1', 'points = [[1,3],[-2,2]], k = 1', '[[-2,2]]', 1);

INSERT INTO dbo.test_cases (exercise_id, case_name, input_data, expected_output, is_sample)
VALUES (100, 'Case 1', 'nums = [3,2,1,5,6,4], k = 2', '5', 1);

GO

-- ------------------------------------------------------------------------------
-- E. Seed 20 Community Discussions & 120 Replies (6 per discussion)
-- ------------------------------------------------------------------------------
SET IDENTITY_INSERT dbo.discussions ON;

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (1, 2, 'Optimal O(N) Hash Map approach vs Sorting Two-Pointer for Pair Sum', 'Solutions', 1, 'Pair Sum', 'While sorting takes O(N log N) time and alters element indices, a single-pass hash map achieves optimal O(N) time and O(N) space. Store the complement `target - num` as you iterate.

Watch out for edge cases with negative integers and when `complement == num`!', 24, 'APPROVED', DATEADD(day, -24, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (2, 3, 'How to properly shrink the sliding window without redundant scans?', 'Questions', 3, 'Longest Substring Without Repeating Characters', 'When encountering a duplicate character in the sliding window, is it cleaner to advance the `left` pointer one-by-one in a while loop, or store character indices in a map and jump `left = max(left, lastSeenIndex + 1)` directly?', 18, 'APPROVED', DATEADD(day, -23, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (3, 4, 'Avoid TLE in Python: collections.deque vs list.pop(0) in BFS traversals', 'Tips', NULL, NULL, 'PSA for Python developers: `list.pop(0)` is strictly O(N) because it shifts all remaining elements in memory! In tree and graph BFS traversals, this easily causes Time Limit Exceeded.

Always use `from collections import deque` and `queue.popleft()` which runs in guaranteed O(1) time.', 35, 'APPROVED', DATEADD(day, -22, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (4, 5, 'FAANG Onsite Interview Debrief: Top 5 Patterns asked in 2026', 'Interview Prep', NULL, NULL, 'Just completed onsite loops at Google and Amazon. Over 5 technical rounds, 80% of algorithmic questions boiled down to these 5 archetypes:
1. Sliding Window (dynamic & fixed size)
2. Monotonic Stack (Next Greater Element)
3. Top-K using Min/Max Heap
4. Tree DFS with bottom-up path aggregations
5. Topological Sort (Kahn''s BFS algorithm)

Mastering these 5 patterns gave me immense confidence!', 42, 'APPROVED', DATEADD(day, -21, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (5, 6, 'Why Greedy Two-Pointer correctly guarantees Maximum Area in Container With Most Water', 'Solutions', 11, 'Container With Most Water', 'A common doubt: Why are we sure that moving the pointer at the shorter height will never miss a better solution with an inner line?

Mathematical proof: Area is bounded by `min(height[l], height[r]) * (r - l)`. If we moved the taller pointer inwards, width decreases AND height can never exceed the shorter pointer. Thus area is strictly smaller! Moving the shorter line is the ONLY move that could possibly increase area.', 29, 'APPROVED', DATEADD(day, -20, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (6, 7, 'Best strategy to avoid duplicate triplets in 3Sum without using an expensive Set?', 'Questions', 15, '3Sum', 'I keep generating duplicate triplets in 3Sum when the input contains multiple identical numbers. What is the cleanest condition to skip duplicates for both the outer anchor index and the two inner two-pointer indices after finding a valid sum?', 16, 'APPROVED', DATEADD(day, -19, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (7, 8, 'Clean Stack solution in Java using modern Switch Expressions and Deque', 'Solutions', 20, 'Valid Parentheses', 'A neat trick for Valid Parentheses: whenever you see an opening bracket `(`, `[`, `{`, push the corresponding closing bracket `)`, `]`, `}` onto the stack!

Then when you encounter a closing character, you only need to check `if (stack.isEmpty() || stack.pop() != c) return false;`. No complicated lookup map needed!', 31, 'APPROVED', DATEADD(day, -18, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (8, 9, 'System Design for Backend Engineers: Transitioning from DSA to Architecture', 'Interview Prep', NULL, NULL, 'Once you feel comfortable with LeetCode Medium algorithms, how do you structure your System Design prep? Which resources helped you understand caching layers (Redis), DB partitioning/sharding, message brokers (Kafka/RabbitMQ), and CAP theorem tradeoffs most intuitively?', 38, 'APPROVED', DATEADD(day, -17, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (9, 10, 'Bit Manipulation Tricks that frequently show up in coding interviews', 'Tips', NULL, NULL, 'Quick bit manipulation reference:
- `n & (n - 1)` : Clears the lowest set bit (Brian Kernighan''s algorithm for counting bits in O(k) steps)
- `n & -n` : Isolates the lowest set bit (due to two''s complement arithmetic)
- `x ^ y` : Finds bits that differ
- `x ^ x == 0` and `x ^ 0 == x` : Finds the single non-duplicate number in an array

These turn O(32) iterations into instant O(1) bitwise operations!', 27, 'APPROVED', DATEADD(day, -16, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (10, 11, 'Intuitive logic to decide which half is sorted in Rotated Binary Search', 'Solutions', 33, 'Search in Rotated Sorted Array', 'The key insight for Search in Rotated Sorted Array:
At least one half is ALWAYS sorted!
- If `nums[left] <= nums[mid]`: The left half is sorted.
  - Target is in left half if `nums[left] <= target < nums[mid]`.
  - Otherwise target must be in right half.
- Else: The right half is sorted.
  - Target is in right half if `nums[mid] < target <= nums[right]`.
  - Otherwise target must be in left half.

Strictly O(log N) runtime with O(1) space.', 25, 'APPROVED', DATEADD(day, -15, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (11, 12, 'Two Pointers vs Monotonic Stack for Trapping Rain Water: Which is preferred?', 'Questions', 42, 'Trapping Rain Water', 'I solved Trapping Rain Water using a Monotonic Decreasing Stack in O(N) time and O(N) space. But interviewers often ask to optimize to O(1) space using two pointers (`leftMax` and `rightMax`).

How do you explain the transition between both techniques smoothly in a live interview?', 22, 'APPROVED', DATEADD(day, -14, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (12, 13, 'Kadane''s Algorithm: Understanding dynamic state transition vs Divide & Conquer', 'Solutions', 53, 'Maximum Subarray', 'Kadane''s algorithm is the pinnacle of elegant 1D Dynamic Programming:
`currentMax = max(nums[i], currentMax + nums[i])`
At each index, you make a binary choice: either extend the running contiguous subarray, or abandon it and start fresh from `nums[i]`.

Follow-up question: How do you return the actual starting and ending indices of the maximal subarray?', 33, 'APPROVED', DATEADD(day, -13, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (13, 14, 'Handling Recursion Stack Overflow in Deep Trees and Graphs', 'Tips', NULL, NULL, 'When traversing deep graphs or skewed trees with depth N >= 10^5, standard recursive DFS will trigger `StackOverflowError` in Java/C++ or exceed `sys.getrecursionlimit()` in Python.

Key mitigation strategies:
1. Convert recursion to an iterative loop with an explicit `ArrayDeque` allocated on the heap.
2. In Python, `sys.setrecursionlimit(200000)` works for competitions but is discouraged in production.
3. Use BFS or Morris Traversal for O(1) space tree walks.', 20, 'APPROVED', DATEADD(day, -12, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (14, 15, 'Mock Interview Checklist: What interviewers silently evaluate during the 45 mins', 'Interview Prep', NULL, NULL, 'A rubric based on shadowing senior FAANG interviewers:
1. **Clarification (3-5 mins)**: Do you check empty inputs, duplicates, range of values, and mutable inputs?
2. **Communication (ongoing)**: Never stay silent for >30 seconds. Think out loud.
3. **Brute Force first (5 mins)**: State the naive approach and its complexity to set a baseline.
4. **Optimization & Dry Run**: Manually trace a sample input through your logic before writing code.
5. **Clean Code**: Meaningful variable names, modular helpers, and zero syntax flubs.
6. **Complexity**: Stating time AND space complexity accurately without waiting to be prompted.', 45, 'APPROVED', DATEADD(day, -11, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (15, 16, 'Single pass O(N) time and O(1) space tracking min price so far in Stock Buy/Sell', 'Solutions', 121, 'Best Time to Buy and Sell Stock', 'For Best Time to Buy and Sell Stock I:
You only need to maintain `minPrice = min(minPrice, price)` and calculate potential profit `maxProfit = max(maxProfit, price - minPrice)` as you iterate.

This simple problem forms the bedrock for:
- Stock II: Infinite transactions (accumulate every positive slope `prices[i] - prices[i-1]`)
- Stock with Cooldown / Fee: State machine DP with `buy` and `sell` states.', 26, 'APPROVED', DATEADD(day, -10, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (16, 17, 'Space optimization from O(N) array to two rolling variables in 1D DP', 'Questions', 198, 'House Robber', 'In House Robber: `dp[i] = max(dp[i-1], dp[i-2] + nums[i])`.
Because `dp[i]` only looks back 2 steps, we can discard the entire array and keep just `prev1` and `prev2`, dropping auxiliary space from O(N) to O(1).

Does this rolling variable optimization apply universally whenever recurrences have a constant lookback window?', 21, 'APPROVED', DATEADD(day, -9, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (17, 18, 'BFS vs DFS vs Disjoint Set Union (Union-Find) for grid connected components', 'Solutions', 200, 'Number of Islands', 'Three approaches compared for Number of Islands:
1. **DFS**: Fewest lines of code, modifies grid in-place (`grid[r][c] = ''0''`), risk of recursion stack overflow on 1000x1000 grids.
2. **BFS**: Iterative with queue, memory bounded by perimeter of island (min(M, N)), zero recursion risk.
3. **Union-Find (DSU)**: Best for dynamic scenarios (e.g. Number of Islands II where land cells are added over time via a stream).', 34, 'APPROVED', DATEADD(day, -8, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (18, 19, 'Iterative pointer reversal without losing node references in Reverse Linked List', 'Tips', 206, 'Reverse Linked List', 'The foolproof 4-step pointer rotation for reversing a singly linked list:
```java
ListNode nextTemp = curr.next; // 1. Save next
curr.next = prev;              // 2. Reverse link
prev = curr;                   // 3. Step prev forward
curr = nextTemp;               // 4. Step curr forward
```
Always return `prev` as the new head (since `curr` ends at null)!
Drawing the 3 pointers on paper completely prevents null pointer errors.', 28, 'APPROVED', DATEADD(day, -7, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (19, 20, 'O(N) time without using the division operator via prefix and suffix products', 'Solutions', 238, 'Product of Array Except Self', 'The challenge: Solve in O(N) time WITHOUT using the division operator `/`.

Optimal two-pass approach:
1. Initialize `ans` array where `ans[i]` contains the product of all elements to the left of `i`.
2. Iterate backwards maintaining a running `rightProduct` variable, multiplying `ans[i] *= rightProduct`, and updating `rightProduct *= nums[i]`.

Result: Exactly O(N) time and O(1) extra space (excluding output array)!', 37, 'APPROVED', DATEADD(day, -6, CURRENT_TIMESTAMP));

INSERT INTO dbo.discussions (id, author_id, title, category, problem_id, problem_title, content, upvotes, moderation_status, created_at)
VALUES (20, 21, 'Top-down memoized recursion vs Bottom-up tabulation for Coin Change', 'Questions', 322, 'Coin Change', 'In Coin Change (unbounded knapsack), what are the practical performance tradeoffs between top-down DP with memoization vs bottom-up tabulation?

Why does bottom-up array iteration frequently benchmark 2-3x faster than memoized recursion in Java and C++?', 23, 'APPROVED', DATEADD(day, -5, CURRENT_TIMESTAMP));

SET IDENTITY_INSERT dbo.discussions OFF;
GO

SET IDENTITY_INSERT dbo.discussion_replies ON;

INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (1, 1, 3, 'Great write-up David! Did you measure the memory overhead of the Map in Java? Sometimes Primitive IntIntMap helps on huge inputs.', DATEADD(hour, -124, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (2, 1, 5, 'Sorting + Two pointers is still super valuable when the question asks to return the values instead of indices and space is constrained to O(1)!', DATEADD(hour, -123, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (3, 1, 8, '100% agreed. In a live interview, proposing brute force O(N^2) first, then optimizing to Hash Map O(N) shows clear progression.', DATEADD(hour, -122, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (4, 1, 12, 'Don''t forget to handle integer overflow if target and numbers can be close to Integer.MAX_VALUE in other languages.', DATEADD(hour, -121, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (5, 1, 17, 'Clean and straightforward explanation! The single pass check `seen.containsKey(complement)` before inserting the current element is key.', DATEADD(hour, -120, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (6, 1, 21, 'Bookmarked! This is the canonical problem for understanding Hash Map complement lookup patterns.', DATEADD(hour, -119, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (7, 2, 2, 'Storing the last seen index is strictly fewer iterations! Just remember `left = max(left, lastSeen[c] + 1)` so `left` never moves backward.', DATEADD(hour, -118, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (8, 2, 4, 'Both run in O(N) amortized time, but the index map approach avoids unnecessary while iterations. Much cleaner in production code.', DATEADD(hour, -117, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (9, 2, 6, 'In C++ and Java, using an `int[128]` array for ASCII characters instead of `HashMap` gives an enormous speed boost on LeetCode tests.', DATEADD(hour, -116, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (10, 2, 9, 'I used to forget the `Math.max(left, ...)` condition and spent 30 minutes debugging why the pointer moved left. Great question!', DATEADD(hour, -115, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (11, 2, 14, 'The while loop approach with a `HashSet` is often easier to explain verbally to an interviewer under pressure.', DATEADD(hour, -114, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (12, 2, 18, 'Both are valid, but interviewers love when you mention that the while loop visits each character at most twice (2N total operations).', DATEADD(hour, -113, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (13, 3, 1, 'Excellent reminder Marcus. This is one of the top subtle mistakes candidates make in Python live coding rounds.', DATEADD(hour, -112, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (14, 3, 5, 'I learned this the hard way on Word Ladder! Switched to `deque` and runtime dropped from 1800ms to 95ms.', DATEADD(hour, -111, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (15, 3, 7, 'Same concept in JavaScript: `Array.prototype.shift()` is O(N). For large queues, use a pointer index or a custom ring buffer.', DATEADD(hour, -110, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (16, 3, 10, 'In Java, also avoid `java.util.Vector` and `Stack`. Prefer `ArrayDeque` as it is non-synchronized and faster than `LinkedList`.', DATEADD(hour, -109, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (17, 3, 13, 'Thanks for sharing! Adding this tip to my Python interview cheat sheet.', DATEADD(hour, -108, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (18, 3, 16, 'A great follow-up is explaining why `deque` uses a doubly linked list of fixed-size memory blocks under the hood in CPython.', DATEADD(hour, -107, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (19, 4, 2, 'Huge congratulations Priya! Did any round ask follow-up questions about distributed scaling or memory limits?', DATEADD(hour, -106, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (20, 4, 3, 'Thanks for the summary! Monotonic stack has been popping up so frequently recently in stock and histogram problems.', DATEADD(hour, -105, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (21, 4, 8, 'Did you practice mostly LeetCode Mediums or Hard? How much did behavioral (LP / Googleyness) weigh into the loops?', DATEADD(hour, -104, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (22, 4, 11, 'Super encouraging post! Did you write tests on the whiteboard / Google Doc or did the interviewers provide test runners?', DATEADD(hour, -103, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (23, 4, 15, 'Topological sort is definitely an Amazon favorite for task scheduler and package dependency problems.', DATEADD(hour, -102, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (24, 4, 19, 'Bookmarked! Focusing on patterns rather than memorizing individual problems is 100% the right strategy.', DATEADD(hour, -101, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (25, 5, 4, 'This mathematical explanation is the clearest I''ve read. Explaining this proof in an interview shows true mastery.', DATEADD(hour, -100, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (26, 5, 7, 'Brilliant deduction! The proof by contradiction makes it crystal clear why no potential maximal area is skipped.', DATEADD(hour, -99, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (27, 5, 12, 'I love problems where the greedy choice has an intuitive mathematical invariant. O(N) time with O(1) space.', DATEADD(hour, -98, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (28, 5, 14, 'Do you have recommendations for similar problems that rely on eliminating sub-optimal search spaces with two pointers?', DATEADD(hour, -97, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (29, 5, 17, 'Trapping Rain Water uses a very similar leftMax / rightMax invariant! Highly recommend trying that next.', DATEADD(hour, -96, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (30, 5, 20, 'Crisp and concise. This helped me finally understand why we discard the shorter line.', DATEADD(hour, -95, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (31, 6, 3, 'After sorting, for outer loop: `if (i > 0 && nums[i] == nums[i-1]) continue;`. That prevents using the same anchor twice!', DATEADD(hour, -94, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (32, 6, 5, 'For inner pointers, once `nums[l] + nums[r] == -nums[i]`: increment `l` while `nums[l] == nums[l-1]` and decrement `r` while `nums[r] == nums[r+1]`.', DATEADD(hour, -93, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (33, 6, 9, 'Crucial point: always compare with `i-1` and NOT `i+1`, otherwise you would skip valid triplets like `[-1, -1, 2]`!', DATEADD(hour, -92, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (34, 6, 13, 'Sorting takes O(N log N) which is dominated by the O(N^2) two-pointer search, so sorting is basically free here.', DATEADD(hour, -91, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (35, 6, 18, 'Using a `Set<List<Integer>>` causes huge memory allocations and can trigger TLE. The pointer skip technique is strictly better.', DATEADD(hour, -90, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (36, 6, 21, 'Thanks everyone! This exact question tripped me up in a phone screen last week.', DATEADD(hour, -89, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (37, 7, 2, 'That trick is so elegant! Pushing the expected closing bracket reduces the matching condition to a single equality check.', DATEADD(hour, -88, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (38, 7, 6, 'And at the end, don''t forget `return stack.isEmpty();` to catch leftover opening brackets like `(((`.', DATEADD(hour, -87, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (39, 7, 10, 'In Java, always use `Deque<Character> stack = new ArrayDeque<>();`. Never use `java.util.Stack` because of synchronized lock overhead.', DATEADD(hour, -86, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (40, 7, 15, 'In C++, `std::string` can act as a stack with `push_back` and `pop_back` to avoid heap allocations completely!', DATEADD(hour, -85, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (41, 7, 16, 'What is the time complexity if string length is odd? Quick early return: `if (s.length() % 2 != 0) return false;`!', DATEADD(hour, -84, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (42, 7, 19, 'Loved the early exit check for odd lengths. Saves unnecessary stack operations on invalid test cases.', DATEADD(hour, -83, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (43, 8, 1, 'Start with Alex Xu''s System Design Interview Volume 1 & 2. Practice back-of-the-envelope calculations for QPS and storage.', DATEADD(hour, -82, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (44, 8, 4, 'Martin Kleppmann''s ''Designing Data-Intensive Applications'' (DDIA) is the gold standard for deep dive into storage engines and replication.', DATEADD(hour, -81, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (45, 8, 7, 'Focus on real-world scenarios: URL shortener (hashing/collisions), Rate limiter (Token bucket/Redis), and Chat service (WebSockets).', DATEADD(hour, -80, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (46, 8, 11, 'Don''t just memorize designs. Learn the trade-offs: Why choose NoSQL vs SQL? What happens during network partitions?', DATEADD(hour, -79, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (47, 8, 14, 'Mock interviews with peers are critical. In 45 minutes you have to drive the conversation from requirements to high-level to deep-dive.', DATEADD(hour, -78, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (48, 8, 18, 'Always ask clarifying questions in the first 5 minutes: Read-heavy vs Write-heavy ratio, Latency SLA, and Consistency requirements.', DATEADD(hour, -77, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (49, 9, 5, 'Brian Kernighan''s trick `n &= (n - 1)` is pure magic. Used it to solve Counting Bits in optimal O(N) time.', DATEADD(hour, -76, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (50, 9, 8, 'Also `(n & (n - 1)) == 0 && n > 0` checks if a number is a power of two in exactly one line of code!', DATEADD(hour, -75, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (51, 9, 12, 'Two''s complement `-n = ~n + 1` is why `n & -n` works so cleanly to extract the rightmost 1-bit. Great refresher.', DATEADD(hour, -74, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (52, 9, 16, 'Single Number II (where every element appears 3 times except one) is an awesome follow-up using bit counters.', DATEADD(hour, -73, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (53, 9, 17, 'Watch out for operator precedence in C++ and Java! Bitwise operators like `&` have lower precedence than `==`.', DATEADD(hour, -72, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (54, 9, 20, 'Always wrap bitwise expressions in parentheses: `(n & 1) == 0`. Saved me so many compiler bugs!', DATEADD(hour, -71, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (55, 10, 2, 'Breaking it down by ''which half is guaranteed sorted'' makes this problem so much easier than trying to locate the pivot first.', DATEADD(hour, -70, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (56, 10, 6, 'Be careful with duplicate elements (Search in Rotated Sorted Array II) where `nums[left] == nums[mid] == nums[right]` degrades to O(N).', DATEADD(hour, -69, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (57, 10, 9, 'The `<=` condition vs `<` is where most people get off-by-one errors. Checking the closed boundary on `left` or `right` is essential.', DATEADD(hour, -68, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (58, 10, 13, 'I used to find the min index first with binary search and then do a second binary search. This one-pass method is twice as clean!', DATEADD(hour, -67, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (59, 10, 15, 'Can confirm, this exact question was my first round technical screening at Microsoft. Nailed it thanks to this pattern!', DATEADD(hour, -66, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (60, 10, 21, 'Saved! Clean logic that eliminates messy edge cases.', DATEADD(hour, -65, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (61, 11, 3, 'Start by explaining the stack horizontal-layering approach: stack computes water by horizontal bars bounded by left and right walls.', DATEADD(hour, -64, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (62, 11, 4, 'Then explain: ''We can also calculate water vertically column-by-column: water[i] = min(maxL, maxR) - height[i]''. That naturally motivates two pointers!', DATEADD(hour, -63, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (63, 11, 7, 'Because we only need `min(maxL, maxR)`, whichever side has the smaller max determines the water level regardless of the other side!', DATEADD(hour, -62, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (64, 11, 10, 'That insight: ''If leftMax < rightMax, the water at left is determined solely by leftMax'' is the eureka moment interviewers look for.', DATEADD(hour, -61, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (65, 11, 14, 'The two-pointer solution is only 15 lines of code and O(1) extra space. Unbeatable.', DATEADD(hour, -60, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (66, 11, 19, 'Practicing both is great because Monotonic Stack generalizes better to Largest Rectangle in Histogram.', DATEADD(hour, -59, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (67, 12, 5, 'To track indices: keep `start = 0`, `end = 0`, `tempStart = 0`. Whenever `currentMax < 0`, set `tempStart = i + 1`!', DATEADD(hour, -58, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (68, 12, 8, 'When `currentMax + nums[i] > globalMax`, update `globalMax`, `start = tempStart`, and `end = i`.', DATEADD(hour, -57, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (69, 12, 11, 'The Divide & Conquer O(N log N) approach using Segment Tree concepts is also a frequent follow-up for streaming/range query updates.', DATEADD(hour, -56, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (70, 12, 16, 'What if all numbers in the array are negative? Kadane''s handles this seamlessly by picking the single least-negative number.', DATEADD(hour, -55, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (71, 12, 18, 'This is often asked as the base problem before Maximum Product Subarray (which requires tracking both min and max due to negative signs).', DATEADD(hour, -54, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (72, 12, 20, 'Short, intuitive, and optimal O(N) time with O(1) auxiliary space.', DATEADD(hour, -53, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (73, 13, 1, 'Spot on Noah. In production systems, heap space is gigabytes while stack space is typically just 1-2 megabytes.', DATEADD(hour, -52, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (74, 13, 6, 'Morris Traversal is mind-blowing when you first see it. Re-wiring temporary right child pointers to predecessors achieves O(1) space!', DATEADD(hour, -51, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (75, 13, 9, 'For graph DFS with cycles, iterative DFS also makes it easier to track visited state and path back-tracing.', DATEADD(hour, -50, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (76, 13, 12, 'Iterative tree traversal with simulated call-stack is a favorite Google interview follow-up.', DATEADD(hour, -49, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (77, 13, 17, 'Good reminder to never rely on `sys.setrecursionlimit` during interviews unless explicitly permitted by the interviewer.', DATEADD(hour, -48, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (78, 13, 21, 'Always mention stack depth when discussing the space complexity of recursive DFS: O(H) where H can be O(N) in the worst case.', DATEADD(hour, -47, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (79, 14, 2, 'Point #4 is massive. Tracing your algorithm with a small input on whiteboard catches 90% of off-by-one errors before running.', DATEADD(hour, -46, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (80, 14, 4, 'Agree on point #3! Jumping straight to complex code without agreeing on the approach with the interviewer is the #1 rejection reason.', DATEADD(hour, -45, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (81, 14, 7, 'Also remember: interviewers want to see how you respond to hints. Treat hints as collaborative nudges, not failures.', DATEADD(hour, -44, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (82, 14, 10, 'Great checklist Grace! Printing this out and reviewing before every mock interview session.', DATEADD(hour, -43, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (83, 14, 13, 'Time management is everything. If you are not writing code by minute 20, you will likely run out of time to test.', DATEADD(hour, -42, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (84, 14, 19, 'Don''t forget to test edge cases at the end: null/empty array, single element, all duplicates, and negative numbers.', DATEADD(hour, -41, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (85, 15, 3, 'The connection from Stock I greedy tracking to Stock with Cooldown state machine DP is such a great learning journey.', DATEADD(hour, -40, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (86, 15, 5, 'In Stock II, many candidates overcomplicate with DP when greedy slope addition is literally 3 lines of code!', DATEADD(hour, -39, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (87, 15, 8, 'Another perspective: It is essentially finding the maximum subarray sum on the array of price differences!', DATEADD(hour, -38, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (88, 15, 11, 'Super clear summary Lucas. This is the ultimate beginner friendly problem to build dynamic tracking intuition.', DATEADD(hour, -37, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (89, 15, 15, 'Kadane''s algorithm and Stock I are practically twin problems when viewed through running difference lenses.', DATEADD(hour, -36, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (90, 15, 20, 'Clean, concise, and optimal. Great stepping stone for DP series.', DATEADD(hour, -35, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (91, 16, 2, 'Yes! Exactly like Fibonacci numbers and Climbing Stairs. Any linear recurrence `f(n) = g(f(n-1), ..., f(n-k))` only needs `k` variables.', DATEADD(hour, -34, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (92, 16, 6, 'Even in 2D DP (like Unique Paths or Edit Distance), if row `i` only depends on row `i-1`, you can reduce space from O(M*N) to O(N) using two rows!', DATEADD(hour, -33, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (93, 16, 9, 'And if you update in the correct order (e.g. reverse iteration in 0/1 Knapsack), you can even reduce to a single 1D array!', DATEADD(hour, -32, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (94, 16, 12, 'Interviewers always ask for space optimization after you write the 1D table. Showing the progression from O(N) to O(1) gets top marks.', DATEADD(hour, -31, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (95, 16, 14, 'Just be careful: if the problem asks to reconstruct the actual path of houses robbed, you must retain the full array or back-pointers.', DATEADD(hour, -30, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (96, 16, 18, 'House Robber II (circular houses) is a fantastic follow-up: run the linear solution twice, once for `[0..n-2]` and once for `[1..n-1]`!', DATEADD(hour, -29, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (97, 17, 4, 'Mentioning Union-Find as the optimal choice for the dynamic/streaming follow-up is an instant hire signal in interviews.', DATEADD(hour, -28, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (98, 17, 7, 'For BFS, remember to mark the cell as visited *immediately upon pushing to queue*, not upon popping! Otherwise duplicate nodes flood the queue.', DATEADD(hour, -27, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (99, 17, 10, 'That queue deduplication tip is huge! Marking on pop instead of push causes memory to explode to O(M*N) instead of O(min(M,N)).', DATEADD(hour, -26, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (100, 17, 13, 'Also in-place modification of `grid` might not be allowed in production if input is immutable. Always ask the interviewer first.', DATEADD(hour, -25, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (101, 17, 16, 'Union-Find with path compression and union by rank has nearly O(1) amortized operations via the inverse Ackermann function α(N).', DATEADD(hour, -24, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (102, 17, 21, 'Outstanding comparison Ryan. Covering DFS, BFS, and DSU tradeoffs shows broad algorithmic maturity.', DATEADD(hour, -23, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (103, 18, 3, 'The 4-step rhythm: Save next -> Reverse -> Advance prev -> Advance curr. Once you memorize this rhythm, you never forget.', DATEADD(hour, -22, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (104, 18, 5, 'The recursive approach is also great practice: `ListNode newHead = reverseList(head.next); head.next.next = head; head.next = null;`.', DATEADD(hour, -21, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (105, 18, 8, 'Recursive uses O(N) call stack space though, so iterative with O(1) space is always the industry standard.', DATEADD(hour, -20, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (106, 18, 11, 'Reverse Linked List II (reversing between index left and right) builds directly on this using a dummy head node.', DATEADD(hour, -19, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (107, 18, 15, 'Reverse Nodes in k-Group is the ultimate final boss for this pattern. Mastering this base technique is crucial.', DATEADD(hour, -18, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (108, 18, 20, 'Always draw the pointers! Visualizing `prev`, `curr`, and `nextTemp` makes linked list bugs impossible to hide.', DATEADD(hour, -17, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (109, 19, 2, 'The right-to-left rolling accumulator is so smart because it avoids allocating a separate suffix product array.', DATEADD(hour, -16, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (110, 19, 6, 'What if division was allowed? Then you''d have to deal with zero elements: single zero vs multiple zeros. Much messier!', DATEADD(hour, -15, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (111, 19, 9, 'Yes! If there are two or more zeros, all elements become zero. The prefix/suffix product naturally handles zeros with zero special casing.', DATEADD(hour, -14, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (112, 19, 12, 'This question tests whether you can break a composite computation into symmetric sub-problems: `prefix[i-1] * suffix[i+1]`.', DATEADD(hour, -13, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (113, 19, 14, 'One of the most frequently asked problems at Meta and Apple. Clean, deterministic, and no edge-case landmines.', DATEADD(hour, -12, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (114, 19, 17, 'Clear and elegant explanation Aiden. Bookmarking for interview revision!', DATEADD(hour, -11, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (115, 20, 1, 'Bottom-up avoids function call overhead (call frame setup/teardown) and benefits heavily from CPU cache locality (sequential memory access).', DATEADD(hour, -10, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (116, 20, 4, 'In top-down, hash map lookup or jumping around non-contiguous array indices causes frequent CPU L1/L2 cache misses.', DATEADD(hour, -9, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (117, 20, 7, 'Bottom-up also prevents recursion stack overflow on large target amounts (e.g. amount = 10000 with coin = 1).', DATEADD(hour, -8, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (118, 20, 10, 'Top-down has one advantage though: it only visits reachable subproblems. If many amounts are unreachable, top-down computes fewer states.', DATEADD(hour, -7, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (119, 20, 13, 'For Coin Change, initializing `dp` array with `amount + 1` (a sentinel value larger than any valid answer) makes the min check super clean.', DATEADD(hour, -6, CURRENT_TIMESTAMP));
INSERT INTO dbo.discussion_replies (id, discussion_id, author_id, content, created_at)
VALUES (120, 20, 18, 'Great discussion Zoe! Starting with top-down to discover the recurrence and then converting to bottom-up is the textbook workflow.', DATEADD(hour, -5, CURRENT_TIMESTAMP));

SET IDENTITY_INSERT dbo.discussion_replies OFF;
GO
