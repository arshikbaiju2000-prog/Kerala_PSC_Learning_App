package com.keralapsc.prep.domain.model.question

enum class AnswerKeyStatus {
    PROVISIONAL,
    FINAL,
    REVISED
}

enum class VerificationStatus {
    RAW,
    PARSED,
    ANSWER_MATCHED,
    VALIDATED,
    PUBLISHED
}

enum class QuestionDifficulty {
    EASY,
    MEDIUM,
    HARD
}
