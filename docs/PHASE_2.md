# Phase 2 — Data Layer & Persistence Technical Documentation

## Overview
Phase 2 establishes the Room-based offline-first persistence foundation for the Kerala PSC Exam Preparation Platform. It implements the data specifications defined in Phase 0, supporting exam hierarchies, questions, options, official answer-key history revisions, multi-post mappings via junction tables, user quiz attempts, spaced repetition progress, streaks, daily goals, and achievements.

---

## 1. Technology Configuration

| Component | Version | Role |
| :--- | :--- | :--- |
| **Room** | 2.6.1 | Local SQLite persistence library (`room-runtime`, `room-ktx`) |
| **Room Compiler** | 2.6.1 | Annotation processing via KSP |
| **KSP** | 2.0.21-1.0.27 | Kotlin Symbol Processing plugin matching Kotlin 2.0.21 |
| **Database Version** | 1 | Initial schema with `exportSchema = true` |
| **DI Container** | `AppContainer` | Single application-scoped manual injection container |

---

## 2. Entity Architecture & Schema Design

### Entities Implemented (19 Total: 17 Primary + 2 Junction Tables)

#### Content & Question Bank Entities
1. **`ExamLevelEntity`** (`exam_levels`): Root examination categories (`10TH_LEVEL`, `PLUS_TWO`, `DEGREE`, `SPECIAL_CATEGORY`).
2. **`ExamEntity`** (`exams`): Specific examination editions (e.g., LDC Preliminary 2023, Degree Prelims 2023) linked to `ExamLevelEntity` with `RESTRICT`.
3. **`PostEntity`** (`posts`): Kerala PSC designations/posts (e.g., Lower Division Clerk, Secretariat Assistant, Village Field Assistant).
4. **`SubjectEntity`** (`subjects`): High-level subject disciplines (Kerala History, Indian Polity, Simple Arithmetic).
5. **`TopicEntity`** (`topics`): Granular syllabus units linked to `SubjectEntity` with `CASCADE`.
6. **`QuestionSourceEntity`** (`question_sources`): Official source metadata (paper code, alpha code, medium, notification URL).
7. **`QuestionEntity`** (`questions`): Primary question record storing text, active correct answer option ID, difficulty, verification status, and revision audit numbers.
8. **`QuestionOptionEntity`** (`question_options`): Options (A, B, C, D) tied to `QuestionEntity` with `CASCADE` and a composite unique index on `(questionId, optionIdentifier)`.
9. **`AnswerKeyEntity`** (`answer_keys`): Official answer-key history log supporting `PROVISIONAL`, `FINAL`, and `REVISED` keys.

#### Many-to-Many Junction Tables
10. **`ExamPostCrossRef`** (`exam_post_cross_ref`): Junction table linking examinations to multiple posts/category codes without duplicating exam entities. Composite PK: `(examId, postId)`.
11. **`QuestionPostCrossRef`** (`question_post_cross_ref`): Junction table linking common questions to multiple posts. Composite PK: `(questionId, postId)`.

#### User Progress & Gamification Entities
12. **`UserQuestionProgressEntity`** (`user_question_progress`): Question mastery metadata (Leitner box, ease factor, repetition count, next review timestamp).
13. **`QuizAttemptEntity`** (`quiz_attempts`): Quiz session record storing type, score, duration, accuracy, and timestamp.
14. **`QuizAnswerEntity`** (`quiz_answers`): Individual user question responses tied to `QuizAttemptEntity` (`CASCADE`) and `QuestionEntity` (`RESTRICT` to prevent user learning history from being deleted).
15. **`DailyGoalEntity`** (`daily_goals`): Daily study target tracking by date string (`YYYY-MM-DD`).
16. **`StreakEntity`** (`streaks`): Continuous study streak tracking with freeze count.
17. **`AchievementEntity`** (`achievements`): Milestone definitions with XP reward and categories.
18. **`UserAchievementEntity`** (`user_achievements`): Unlocked user milestone records with timestamps.
19. **`UserProgressEntity`** (`user_progress`): User XP, current level, total questions solved, and overall accuracy.

---

## 3. Important Relationship Models

### A. Exam ↔ Post and Question ↔ Post (Many-to-Many)
In Kerala PSC, a single examination (e.g. *Degree Level Common Preliminary Exam 114/2023*) is used to create shortlists for multiple distinct posts (e.g. *Secretariat Assistant*, *Sub Inspector*, *Auditor*). Furthermore, past questions from earlier examinations are frequently reused across post preparations.

- **Exam ↔ Post** is modeled using `ExamPostCrossRef`.
- **Question ↔ Post** is modeled using `QuestionPostCrossRef`.
- Both junction tables use composite primary keys and indexed foreign keys with `CASCADE` behavior.
- Conceptual queries support retrieving all questions relevant to a specific post (`getQuestionsByPost`) via SQL `INNER JOIN` without question record duplication.

### B. Question Correct Answer vs. Answer-Key History
- **`QuestionEntity.correctOptionId`**: Stores the **currently active official correct option ID** (or `null` if the question has been officially cancelled by the Commission).
- **`AnswerKeyEntity`**: Preserves the complete historical lineage of published keys (`PROVISIONAL` → `REVISED` / `FINAL`), including change remarks and effective dates.
- When an answer key is revised or cancelled by the PSC, a new record is added to `answer_keys`, and `QuestionEntity` is updated in place. This guarantees that historical auditability is maintained without ambiguity.

### C. Cancelled Questions
- For cancelled questions (e.g. typographical errors or translation discrepancies in Malayalam papers), `QuestionEntity.correctOptionId` is explicitly set to `null`, and `AnswerKeyEntity.correctOptionIdentifier` is recorded as `"CANCELLED"`.
- The database schema never generates fake answers for cancelled questions.

---

## 4. Verification Lifecycle

Questions transition through five discrete stages:
1. `RAW`: Initial ingested format.
2. `PARSED`: Text, options, and metadata extracted.
3. `ANSWER_MATCHED`: Provisional answer key mapped.
4. `VALIDATED`: Expert committee / final key confirmed.
5. `PUBLISHED`: Verified and exposed to end users in the Android app.

Normal app flows query questions with `verificationStatus = 'PUBLISHED'`. Test/development packs can include `VALIDATED` or `RAW` items to test filter boundaries.

---

## 5. Indexing Rationale

Indexes were introduced strictly according to query patterns:
- `questions(examId)`: Fast retrieval of questions during exam-specific mock creation.
- `questions(topicId)`: Fast topic practice queries.
- `questions(verificationStatus)`: High-selectivity filter ensuring draft content is ignored.
- `questions(difficulty)`: Difficulty-filtered practice sessions.
- `questions(topicId, difficulty)`: Composite index for targeted drill sessions.
- `question_options(questionId, optionIdentifier)`: Unique composite index preventing duplicate option letters (e.g. two "A" options).
- `exam_post_cross_ref(examId, postId)` & `question_post_cross_ref(questionId, postId)`: Efficient many-to-many lookups.
- `user_question_progress(nextReviewDue)`: Fast retrieval of items due for spaced repetition review (`<= currentTime`).
- `quiz_attempts(startedAt)`: Fast descending sorting for recent quiz history.

---

## 6. SQLite Foreign Key Integrity & Upsert Finding

### Critical Constraint Finding:
In SQLite, using `@Insert(onConflict = OnConflictStrategy.REPLACE)` on a parent table (such as `exam_levels` or `exams`) triggers an internal `DELETE` before `INSERT`. When child tables have `ForeignKey.RESTRICT` constraints (such as `exams.levelId` referencing `exam_levels.id`), SQLite triggers a `FOREIGN KEY constraint failed (code 1811 SQLITE_CONSTRAINT_TRIGGER)` because active child rows exist during the replace operation.

### Architectural Solution:
All insertion DAOs (`SyncDao`, `ExamDao`, etc.) were configured to use Room 2.6.1 `@Upsert`. In SQLite 3.24+, Room compiles `@Upsert` to `INSERT INTO ... ON CONFLICT DO UPDATE`, updating the parent row in-place without deleting it. This preserves all `RESTRICT` and `CASCADE` relational constraints without data conflicts.

---

## 7. Search Architecture Decision (FTS4 Analysis)

While full-text search (FTS4) was mentioned in Phase 0 planning, Phase 2 implements standard relational indexing on `topicId`, `subjectId`, `examId`, `postId`, and `difficulty`. 

**Rationale for postponing FTS4 to a later phase:**
1. At this phase, questions are searched by structured filters (syllabus, exam, post, topic, difficulty).
2. FTS4 virtual tables in SQLite duplicate string storage and require synchronization triggers (`INSERT`, `UPDATE`, `DELETE` triggers on the base table).
3. Implementing FTS4 prematurely before search requirements are defined would introduce maintenance overhead.
4. Extension point: When free-text search is introduced, an FTS4/FTS5 virtual table `questions_fts` can be added via a Room migration without altering existing entity structures.

---

## 8. Controlled Development Seed Dataset

Rather than importing unverified real Kerala PSC papers prematurely, a controlled 25-question development seed dataset was implemented in `data/local/asset/DevelopmentSeedData.kt`:
- **3 Exam Levels**: 10th Level, Plus Two, Degree Level.
- **3 Exams**: LDC Common Preliminary 2023, VFA Preliminary 2022, Degree Preliminary 2023.
- **4 Posts**: Lower Division Clerk, Village Field Assistant, Clerk Typist, Secretariat Assistant.
- **4 Exam-Post CrossRefs**: Validating multi-post examinations.
- **3 Subjects**: Kerala History & Geography, Indian Constitution & Polity, Simple Arithmetic & Reasoning.
- **6 Topics**: Covering Renaissance, Rivers, Fundamental Rights, Judiciary, Percentages, and Time & Work.
- **3 Official Sources**: Complete with Malayalam and English medium tags.
- **25 Questions**: Clearly labeled as development/test data.
- **100 Options**: 4 options (A, B, C, D) per question.
- **34 Question-Post CrossRefs**: Verifying multi-post question assignments.
- **31 Answer Keys**: Including provisional-to-revised transitions, and Question 24 as an officially `CANCELLED` question.
- **1 Validated Question (Question 25)**: Explicitly excluded from `PUBLISHED` user queries to test filtering.

---

## 9. Repository Layer Architecture

Clean Architecture domain interfaces and mapping implementations:
- **`QuestionRepository` / `QuestionRepositoryImpl`**: Flow-based reactive reads and suspend counts.
- **`ExamRepository` / `ExamRepositoryImpl`**: Exam level, exam, subject, and topic flows.
- **`QuizRepository` / `QuizRepositoryImpl`**: Quiz attempt and answer persistence.
- **`UserProgressRepository` / `UserProgressRepositoryImpl`**: User preparation statistics and spaced repetition review due queues.
- **`GamificationRepository` / `GamificationRepositoryImpl`**: Streaks, daily goals, and achievements.

Entity-to-domain mappers decouple the internal Room database schema from presentation and business logic.

---

## 10. Dependency Injection (`AppContainer`)

The lightweight `AppContainer` pattern was updated to provide a single, application-scoped instance of `PSCDatabase` and all repository implementations.
- No third-party reflection or DI frameworks (no Hilt, no Koin).
- Initialized once in `PSCApplication.onCreate()`.
- Thread-safe lazy instantiation.
- Prepopulates seed dataset asynchronously on `Dispatchers.IO` on initial install.

---

## 11. Verification & Testing

### Test Suite Execution Summary:
- **Unit Tests (`src/test`)**: 6 passed. Validated dataset integrity, enum parsing, and cancelled question handling.
- **Connected Tests (`src/androidTest`) on Pixel_8a Emulator**: **24 out of 24 passed (0 skipped, 0 failed)**:
  1. `test01_databaseCreation`: Verified SQLite database creation and version 1.
  2. `test02_seedDataLoading`: Verified 25 questions, 24 published, levels, exams, and posts.
  3. `test03_foreignKeyConstraints`: Verified `SQLiteConstraintException` when inserting orphan foreign keys.
  4. `test04_questionInsertion`: Verified question creation and retrieval.
  5. `test05_questionOptionInsertion`: Verified option insertion.
  6. `test06_duplicatePrevention`: Verified SQLite unique constraint on `(questionId, optionIdentifier)`.
  7. `test07_examPostManyToManyRelationship`: Verified `ExamPostCrossRef` mapping exam to multiple posts.
  8. `test08_questionPostManyToManyRelationship`: Verified `QuestionPostCrossRef` mapping question to multiple posts.
  9. `test09_answerKeyHistoryAndCancelledSupport`: Verified key revisions (PROVISIONAL → REVISED) and CANCELLED questions with `null` correct option.
  10. `test10_verificationStatusFiltering`: Verified only `PUBLISHED` questions are retrieved for learners.
  11. `test11_queryByExam`: Verified query by examination edition.
  12. `test12_queryByPost`: Verified query by post ID.
  13. `test13_queryByTopic`: Verified query by syllabus topic.
  14. `test14_queryPublishedQuestions`: Verified published limit query.
  15. `test15_queryByDifficulty`: Verified query filtered by EASY, MEDIUM, or HARD.
  16. `test16_questionCounts`: Verified total, published, and topic count aggregations.
  17. `test17_quizAttemptPersistence`: Verified session record persistence and XP earned.
  18. `test18_quizAnswerPersistence`: Verified individual answers stored with `RESTRICT` on question deletion.
  19. `test19_userQuestionProgressPersistence`: Verified spaced repetition progress persistence (box level, intervals, ease factor).
  20. `test20_dailyGoalPersistence`: Verified daily goals and completion status.
  21. `test21_streakPersistence`: Verified streak counts and streak freezes.
  22. `test22_achievementPersistence`: Verified achievement unlock records.
  23. `HomeScreenTest`: Verified Compose UI Home screen components.
  24. `NavigationTest`: Verified Jetpack Compose type-safe bottom navigation.

### Physical/Emulator Verification:
- Installed on active Android Studio `Pixel_8a` emulator (API 35).
- Logcat confirmed: `Persistence layer ready: 25 total questions (24 published)` in 242ms.
- Verified offline launch with Wi-Fi and mobile data disabled: 0 crashes, instantaneous startup.
- All 5 Phase 1 navigation tabs (Home, Practice, Mock Exam, Progress, Profile) verified intact.
