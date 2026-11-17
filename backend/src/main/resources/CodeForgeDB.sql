-- ==============================================================================
-- CodeForge Database Schema Definition Script (DDL)
-- ==============================================================================
-- Target Engine: Microsoft SQL Server 2019+
-- Database: CodeForgeDb
--
-- Architectural Role:
-- Defines the foundational relational schema representing core platform entities:
-- 1. dbo.users              - Identity credentials, roles, tokens, and gamified practice metrics.
-- 2. dbo.exercises          - Algorithmic problems, constraints, and starter codes.
-- 3. dbo.test_cases         - Standard verification suites and sample test cases.
-- 4. dbo.submissions        - Code execution submissions, performance telemetry, verdicts.
-- 5. dbo.saved_exercises    - Bookmarked exercises per user.
-- 6. dbo.notes              - Learner study notes, pattern templates, and takeaway memos.
-- 7. dbo.discussions        - Community discussion threads, questions, and solution guides.
-- 8. dbo.discussion_replies - Discussion thread responses and comments.
-- 9. dbo.discussion_upvotes - Discussion thread upvote tracking.
-- ==============================================================================

USE CodeForgeDb;
GO

-- ==============================================================================
-- 1. Table: dbo.users (Corresponds to User.java entity)
-- Stores account identities, BCrypt password hashes, reset tokens, and user statistics.
-- ==============================================================================
CREATE TABLE dbo.users (
    id                   BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY, -- Auto-incrementing primary key
    username             VARCHAR(100) NOT NULL UNIQUE,              -- Unique user handle (e.g., alex_morgan)
    email                VARCHAR(255) NOT NULL UNIQUE,              -- Unique user email
    password_hash        VARCHAR(255) NOT NULL,                     -- BCrypt-hashed password string
    full_name            VARCHAR(150) NOT NULL,                     -- Display full name
    role                 VARCHAR(50)  NULL,                         -- Role: 'Guest' | 'Learner' | 'Admin'
    streak_days          INT NULL,                                  -- Consecutive active practice days
    solved_count         INT NULL,                                  -- Number of accepted problems solved
    acceptance_rate      NUMERIC(19,2) NULL,                        -- Overall acceptance rate (percentage)
    practice_time_hours  NUMERIC(19,2) NULL,                        -- Total practice duration in hours
    reset_token          VARCHAR(255) NULL,                         -- UUID token for password reset
    reset_token_expiry   DATETIME2 NULL,                            -- Expiration timestamp for password reset
    created_at           DATETIME2 NULL                             -- Registration timestamp
);
GO

-- ==============================================================================
-- 2. Table: dbo.exercises (Corresponds to Exercise.java entity)
-- Stores algorithmic problems, markdown descriptions, limits, and starter templates.
-- ==============================================================================
CREATE TABLE dbo.exercises (
    id                      BIGINT NOT NULL PRIMARY KEY,           -- Explicit problem ID (e.g. 104, 105)
    title                   VARCHAR(255) NOT NULL,                 -- Challenge title (e.g. 'Pair Sum')
    difficulty              VARCHAR(50)  NOT NULL,                 -- Difficulty tier: 'Easy' | 'Medium' | 'Hard'
    topics                  VARCHAR(255) NOT NULL,                 -- Comma-separated category topics
    acceptance_rate         NUMERIC(19,2) NULL,                    -- Platform-wide acceptance rate
    description             TEXT NOT NULL,                         -- Problem statement in Markdown
    constraints             TEXT NULL,                             -- Problem input limits in Markdown
    starter_code_python     TEXT NULL,                             -- Starter template for Python 3
    starter_code_javascript TEXT NULL,                             -- Starter template for JavaScript
    starter_code_cpp        TEXT NULL,                             -- Starter template for C++
    starter_code_java       TEXT NULL,                             -- Starter template for Java
    time_limit_ms           INT NULL,                              -- Maximum allowed execution time in ms
    memory_limit_mb         INT NULL,                              -- Maximum allowed memory in MB
    is_custom               BIT NULL,                              -- 1 if user-created, 0 if system problem
    author_id               BIGINT NULL,                           -- Foreign key referencing author in dbo.users
    moderation_status       VARCHAR(20) DEFAULT 'APPROVED' NOT NULL, -- Moderation status: 'PENDING' | 'APPROVED' | 'REJECTED'
    rejection_reason        VARCHAR(500) NULL,                     -- Feedback reason from administrator upon rejection
    created_at              DATETIME2 NULL,                        -- Problem creation timestamp
    CONSTRAINT fk_exercises_author FOREIGN KEY (author_id) REFERENCES dbo.users(id)
);
GO

-- ==============================================================================
-- 3. Table: dbo.test_cases (Corresponds to TestCase.java entity)
-- Stores verification test inputs and expected outputs for problem judging.
-- ==============================================================================
CREATE TABLE dbo.test_cases (
    id               BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,    -- Auto-incrementing primary key
    exercise_id      BIGINT NOT NULL,                              -- Foreign key referencing dbo.exercises
    case_name        VARCHAR(100) NOT NULL,                        -- Case label (e.g. 'Case 1')
    input_data       TEXT NOT NULL,                                -- Standard input stream content
    expected_output  TEXT NOT NULL,                                -- Expected standard output for matching
    is_sample        BIT NULL,                                     -- 1: Public IDE sample, 0: Hidden test
    CONSTRAINT fk_test_cases_exercise FOREIGN KEY (exercise_id) REFERENCES dbo.exercises(id)
);
GO

-- ==============================================================================
-- 4. Table: dbo.submissions (Corresponds to Submission.java entity)
-- Stores user solution submissions, judging results, runtimes, and percentiles.
-- ==============================================================================
CREATE TABLE dbo.submissions (
    id                  BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY, -- Auto-incrementing primary key
    user_id             BIGINT NOT NULL,                           -- Submitting user foreign key
    exercise_id         BIGINT NOT NULL,                           -- Target exercise foreign key
    language            VARCHAR(50) NOT NULL,                      -- Language: 'python' | 'javascript' | 'cpp' | 'java'
    source_code         TEXT NOT NULL,                             -- Full source code submitted
    status              VARCHAR(50) NOT NULL,                      -- Verdict: 'Accepted' | 'Wrong Answer' | etc.
    runtime_ms          INT NULL,                                  -- Execution time in milliseconds
    memory_mb           NUMERIC(19,2) NULL,                        -- Peak memory consumed in MB
    runtime_beats_pct   NUMERIC(19,2) NULL,                        -- Runtime performance percentile
    memory_beats_pct    NUMERIC(19,2) NULL,                        -- Memory efficiency percentile
    tests_passed        INT NULL,                                  -- Number of passed test cases
    total_tests         INT NULL,                                  -- Total test cases evaluated
    submitted_at        DATETIME2 NULL,                            -- Submission timestamp
    CONSTRAINT fk_submissions_user     FOREIGN KEY (user_id)     REFERENCES dbo.users(id),
    CONSTRAINT fk_submissions_exercise FOREIGN KEY (exercise_id) REFERENCES dbo.exercises(id)
);
GO

-- ==============================================================================
-- 5. Table: dbo.saved_exercises (Corresponds to SavedExercise.java entity)
-- Stores bookmarked problems for users.
-- ==============================================================================
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

-- ==============================================================================
-- 6. Table: dbo.notes (Corresponds to Note.java entity)
-- Stores learner personal study notes, templates, and takeaways.
-- ==============================================================================
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

-- ==============================================================================
-- 7. Table: dbo.discussions (Corresponds to Discussion.java entity)
-- Stores community discussion threads, questions, and solution breakdowns.
-- ==============================================================================
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

-- ==============================================================================
-- 8. Table: dbo.discussion_replies (Corresponds to DiscussionReply.java entity)
-- Stores threaded replies to community discussions.
-- ==============================================================================
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

-- ==============================================================================
-- 9. Table: dbo.discussion_upvotes (Corresponds to DiscussionUpvote.java entity)
-- Tracks upvote interactions for discussions to enforce single-upvote idempotency.
-- ==============================================================================
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
