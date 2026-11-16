-- ==============================================================================
-- CodeForge Database Schema Definition Script (DDL)
-- ==============================================================================
-- Target Engine: Microsoft SQL Server 2019+
-- Database: CodeForgeDb
--
-- Architectural Role:
-- Defines the foundational relational schema representing core platform entities:
-- 1. dbo.users       - Identity credentials, roles, and gamified practice metrics.
-- 2. dbo.exercises   - Algorithmic problems, constraints, and starter codes.
-- 3. dbo.test_cases  - Standard verification suites and sample test cases.
-- 4. dbo.submissions - Code execution submissions, performance telemetry, verdicts.
-- ==============================================================================

USE CodeForgeDb;
GO

-- ==============================================================================
-- 1. Table: dbo.users (Corresponds to User.java entity)
-- Stores account identities, BCrypt password hashes, and user statistics.
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
