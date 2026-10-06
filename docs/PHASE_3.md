# Phase 3: Question Engine & Core Quiz Presentation

## 1. Overview & Objectives

Phase 3 implements the **Question Selection Engine**, **Answer Evaluation & Official PSC Answer-Key Resolution System**, **Quiz Runner & Summary UI**, and **Atomic Quiz Attempt & Telemetry Persistence** for the Kerala PSC Exam Preparation Platform.

### Key Deliverables Completed:
* **Quiz Modes:** Daily Practice Quiz and Topic-specific Practice Quiz (`DAILY`, `TOPIC`).
* **Database-Level Question Selection Engine:** Direct SQL-level querying (`QuestionDao`) filtering by topic, verification status (`PUBLISHED`), and limit (`LIMIT :limit`). Shuffles questions, guarantees distinct question IDs, and handles small/empty datasets gracefully without padding or duplicating questions.
* **Answer Evaluation Engine:** Authoritative answer-key matching with strict distinction between genuine correct matches (`isGenuineCorrect = true`) and cancelled questions awarded credit under official PSC policy (`status = CANCELLED_AWARDED`, `isCredited = true`, `isGenuineCorrect = false`).
* **Deterministic Integer Metrics:** Accuracy calculation strictly using integer arithmetic (`(creditedAnswers * 100) / totalQuestions`) with no floating-point rounding. Total credited answers calculated as `creditedAnswers = correctCount + cancelledCount`.
* **Atomic Room Persistence:** Writes completed sessions into `quiz_attempts` (`QuizAttemptEntity`), individual question responses into `quiz_answers` (`QuizAnswerEntity`), and updates baseline progress metrics in `user_question_progress` (`UserQuestionProgressEntity`).
* **Dynamic Options Rendering:** Options rendered dynamically based on `question.options` (never hardcoding exactly 4 options).
* **Official PSC Source Attribution:** Bottom sheet displaying authority, examination title, question paper code, alpha booklet code, medium of examination, and revision status without fabricating missing metadata.
* **Zero Timer / Practice Philosophy:** Response time tracked internally (`responseTimeMs`, `totalTimeSeconds`) without visible countdown timers or stressful time limits.
* **Rotation & Session Abandonment Preservation:** Preserves active quiz question and selection state across device configuration changes (rotation) via `SavedStateHandle`. Session abandonment via back-press dialog leaves cleanly without recording false completed attempts.

---

## 2. Architecture & File Structure

```
app/src/main/java/com/keralapsc/prep/
├── di/
│   └── AppContainer.kt                      # Registered quiz use cases
├── domain/
│   ├── model/quiz/
│   │   ├── QuizModels.kt                    # Updated QuizAttempt.accuracyPercentage to Int
│   │   └── QuizSessionModels.kt             # EvaluationStatus, QuizQuestion, AnswerEvaluation, QuizResult
│   ├── repository/
│   │   └── QuizRepository.kt                # Quiz repository interface
│   └── usecase/quiz/
│       ├── GetQuizQuestionsUseCase.kt       # Database-level question selection & deduplication
│       ├── EvaluateAnswerUseCase.kt         # Authoritative key matching & cancelled question credit
│       ├── CalculateQuizResultUseCase.kt    # Integer arithmetic accuracy & aggregation
│       └── SaveQuizAttemptUseCase.kt        # Atomic Room persistence of attempts, answers & telemetry
├── data/
│   └── repository/
│       └── QuizRepositoryImpl.kt            # Updated accuracy calculation to integer arithmetic
└── presentation/
    ├── navigation/
    │   ├── NavGraph.kt                      # QuizRunner and QuizSummary routes
    │   └── PSCNavHost.kt                    # Real screen composables wired
    ├── viewmodel/
    │   └── ViewModelFactory.kt              # AppContainer ViewModelProvider factory
    ├── theme/
    │   └── Color.kt                         # Phase 3 semantic color aliases
    └── quiz/
        ├── runner/
        │   ├── QuizUiState.kt               # State, events, and one-time effects
        │   ├── QuizViewModel.kt             # Quiz runner state machine with SavedStateHandle
        │   ├── QuizRunnerScreen.kt          # Compose runner screen with progress, cards & dialogs
        │   └── components/
        │       ├── OptionCard.kt            # Dynamic option component with states (unselected, selected, correct, incorrect, cancelled)
        │       ├── AnswerFeedbackCard.kt    # Immediate feedback card with explanation & revision remarks
        │       └── QuestionAttributionBottomSheet.kt # Official PSC sourcing & metadata sheet
        └── summary/
            ├── QuizSummaryUiState.kt        # Summary screen state
            ├── QuizSummaryViewModel.kt      # Loads completed attempt by ID
            └── QuizSummaryScreen.kt         # Summary screen with score, integer accuracy, and metrics
```

---

## 3. Official Kerala PSC Scoring & Cancellation Rules

1. **Genuine Correct Answer:**
   * User selection matches the authoritative answer key (`correctOptionId`).
   * `status = EvaluationStatus.CORRECT`
   * `isCredited = true`, `isGenuineCorrect = true`

2. **Incorrect Answer:**
   * User selection does not match the active answer key.
   * `status = EvaluationStatus.INCORRECT`
   * `isCredited = false`, `isGenuineCorrect = false`

3. **Cancelled Question (PSC Policy Compliance):**
   * Question marked as cancelled by Commission in revised answer key (`correctOptionId == null`).
   * `status = EvaluationStatus.CANCELLED_AWARDED`
   * `isCredited = true`, `isGenuineCorrect = false`
   * Tracked in distinct bucket `cancelledCount`.
   * Displayed with Gold/Amber styling and clear official credit disclosure.

4. **Scoring Formulation:**
   * `creditedAnswers = correctCount + cancelledCount`
   * `score = creditedAnswers`
   * `accuracyPercentage: Int = if (totalQuestions > 0) (creditedAnswers * 100) / totalQuestions else 0`

---

---

## 4. Learning Telemetry Semantics & Cancelled Question Distinction

### Architectural Distinction: Scoring vs Candidate Proficiency Telemetry

The Kerala PSC engine maintains a strict separation between **Quiz Scoring** (which honors official PSC cancellation policy awarding full credit to candidates) and **Learning Telemetry** (`user_question_progress.timesCorrect`, which measures true candidate proficiency):

1. **Scoring Formulation (Preserved for Candidates):**
   * `correctCount`: Genuine correct answers.
   * `cancelledCount`: Officially cancelled Kerala PSC questions.
   * `creditedAnswers = correctCount + cancelledCount`.
   * `QuizAttempt.score = creditedAnswers`.
   * `QuizAnswer.isCorrect`: Stored as `true` for both genuine correct and cancelled-awarded questions to preserve scoring history.

2. **Telemetry Formulation (`user_question_progress`):**
   * `CORRECT` (Genuine correct answer):
     * `timesAttempted += 1`
     * `timesCorrect += 1`
     * `consecutiveCorrect += 1`
   * `INCORRECT`:
     * `timesAttempted += 1`
     * `timesCorrect += 0`
     * `consecutiveCorrect = 0`
   * `CANCELLED_AWARDED` (Officially cancelled Kerala PSC question):
     * `timesAttempted += 1`
     * `timesCorrect += 0` (MUST NOT increment; candidate did not genuinely solve the question)
     * `consecutiveCorrect`: Not incremented
   * `SKIPPED`:
     * `timesAttempted += 1`
     * `timesCorrect += 0`
     * `consecutiveCorrect = 0`

Implemented in `SaveQuizAttemptUseCase.kt`:
```kotlin
val wasGenuinelyCorrect = eval.isGenuineCorrect // EvaluationStatus.CORRECT ONLY
val newCorrectAttempts = (existing?.correctAttempts ?: 0) + (if (wasGenuinelyCorrect) 1 else 0)
val newConsecutive = if (wasGenuinelyCorrect) (existing?.consecutiveCorrect ?: 0) + 1 else 0
```

---

## 5. Test Verification Results

### Unit Tests (`gradlew.bat testDebugUnitTest`):
* `GetQuizQuestionsUseCaseTest` (4 tests): Daily selection, topic filtering, deduplication, smaller-than-target question sets, empty topic handling.
* `EvaluateAnswerUseCaseTest` (4 tests): Correct selection, incorrect selection, cancelled question full credit, genuine correct separation, remarks disclosure.
* `CalculateQuizResultUseCaseTest` (3 tests): Integer arithmetic verification, truncation testing, cancelled count aggregation, zero-division safety.
* `SaveQuizAttemptUseCaseTest` (1 test): Telemetry verification distinguishing genuine correct (`timesCorrect +1`) from cancelled awarded (`timesCorrect +0`), while preserving attempt score credit.
* `QuizViewModelTest` (10 tests):
  1. `test1_initialLoading_loadsAndDisplaysQuestion1`: Loading -> Question 1 displayed upon question load.
  2. `test2_optionSelection_storesSelectionAndPreventsDoubleSelection`: Selection storage, feedback reveal, double-tap prevention.
  3. `test3_correctAnswer_evaluatesToCorrect`: `CORRECT` evaluation state and flags.
  4. `test4_incorrectAnswer_evaluatesToIncorrect`: `INCORRECT` evaluation state and flags.
  5. `test5_cancelledQuestion_evaluatesToCancelledAwardedWithFullCredit`: `CANCELLED_AWARDED` status, full credit (`isCredited = true`), and non-genuine proficiency (`isGenuineCorrect = false`).
  6. `test6_advanceToNextQuestion_movesToQuestion2`: Advances question index and clears answer reveal.
  7. `test7_finalQuestionCompletion_savesAttemptAndEmitsNavigateToSummary`: Sequential quiz completion, `SaveQuizAttemptUseCase` invocation, attempt persistence, and `NavigateToSummary` effect emission.
  8. `test8_exitConfirmation_dialogTogglesWithoutAffectingQuiz`: Back press shows exit dialog; dismissal keeps quiz session active.
  9. `test9_confirmExit_abandonsSessionWithoutSaving`: Confirming exit abandons active session, emits `NavigateBack`, and does NOT persist false completed attempts.
  10. `test10_savedStateHandleRestoration_preservesAttemptIdAndCurrentIndex`: Restoration of current index and active session state via `SavedStateHandle`.
* `ArchitectureAndDispatchersTest` (3 tests): Core architecture and coroutine dispatcher verification.
* `RepositoryMappingTest` (3 tests): Seed dataset integrity and Question 24 cancelled verification.
* **Unit Test Result:** **28 / 28 passed (0 failed, 0 skipped)** (`BUILD SUCCESSFUL`).

### Instrumented & Compose Tests (`gradlew.bat connectedDebugAndroidTest` on Pixel_8a):
* `CancelledQuestionTelemetryAndroidTest`: Full on-device verification of `dev_q_24` cancelled-question telemetry, Room DB DAO updates, and genuine-correct contrast.
* `QuizRunnerScreenTest`:
  * Option card selection & dynamic rendering
  * Feedback card with explanation & continue button
  * Feedback card with cancelled-question disclosure & revision remarks
  * Official source attribution bottom sheet
* `QuizSummaryScreenTest`: Score, integer accuracy, credited count, duration rendering.
* `HomeScreenTest`, `NavigationTest`, `RoomDatabaseTest`, `DaoTests`: Phase 1 & 2 regression suite.
* **Instrumented Test Result:** **30 / 30 tests passed on Pixel_8a(AVD) - 16 (0 failed, 0 skipped)** (`BUILD SUCCESSFUL`).

### Build Verification (`gradlew.bat assembleDebug`):
* **Result:** `BUILD SUCCESSFUL` (0 errors, 0 warnings).

### Pixel_8a Physical Emulator Flow & Database Verification:
* **Question 24 (`dev_q_24`) Cancelled Case:** Verified `dev_q_24` in `topic_renaissance`:
  * `QuizResult`: `cancelledCount = 1`, `correctCount = 0`, `creditedAnswers = 1`, `score = 1`.
  * `QuizAttemptEntity`: `correctAnswers = 1` (credited).
  * `QuizAnswerEntity`: `isCorrect = 1` (credited for scoring review).
  * `UserQuestionProgressEntity`: `timesAttempted = 1`, `timesCorrect = 0` (candidate did not genuinely answer; proficiency not inflated).
* **Regression Verification:** All existing Phase 1, 2, and 3 functionalities (Daily Quiz, Topic Quiz, SQL-level filtering, no duplicates, integer accuracy, source attribution, rotation state preservation, offline execution) verified and intact.

---

## 6. Phase 3 Formal Status

**FORMALLY CLOSED** (All Phase 3 requirements, corrective fixes, and test suites verified and passing).
