package com.keralapsc.prep.core.model

/**
 * Generic Result wrapper for domain and data operations.
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Error(val throwable: Throwable, val message: String? = throwable.message) : AppResult<Nothing>
    data object Loading : AppResult<Nothing>
}
