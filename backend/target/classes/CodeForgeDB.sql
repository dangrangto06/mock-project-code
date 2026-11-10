
USE CodeForgeDb;
GO

-- ============================================================
-- users  <-  entity User.java
-- ============================================================
CREATE TABLE dbo.users (
    id                   BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    username             VARCHAR(100) NOT NULL UNIQUE,
    email                VARCHAR(255) NOT NULL UNIQUE,
    password_hash        VARCHAR(255) NOT NULL,   -- BCrypt hash
    full_name             VARCHAR(150) NOT NULL,
    role                 VARCHAR(50)  NULL,        -- 'Guest' | 'Learner' | 'Admin'
    streak_days          INT NULL,
    solved_count         INT NULL,
    acceptance_rate      NUMERIC(19,2) NULL,
    practice_time_hours  NUMERIC(19,2) NULL,
    created_at           DATETIME2 NULL
);
GO

-- ============================================================
-- exercises  <-  entity Exercise.java
-- ============================================================
CREATE TABLE dbo.exercises (
    id                      BIGINT NOT NULL PRIMARY KEY,
    title                   VARCHAR(255) NOT NULL,
    difficulty              VARCHAR(50)  NOT NULL,   -- 'Easy' | 'Medium' | 'Hard'
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
    created_at              DATETIME2 NULL,
    CONSTRAINT fk_exercises_author FOREIGN KEY (author_id) REFERENCES dbo.users(id)
);
GO

-- ============================================================
-- test_cases  <-  entity TestCase.java
-- ============================================================
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

-- ============================================================
-- submissions  <-  entity Submission.java
-- ============================================================
CREATE TABLE dbo.submissions (
    id                  BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    exercise_id         BIGINT NOT NULL,
    language            VARCHAR(50) NOT NULL,   -- 'python' | 'javascript' | 'cpp' | 'java'
    source_code         TEXT NOT NULL,
    status              VARCHAR(50) NOT NULL,   -- 'Accepted' | 'Wrong Answer' | 'Time Limit Exceeded'...
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