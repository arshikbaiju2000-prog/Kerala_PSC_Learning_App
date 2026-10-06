package com.keralapsc.prep.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Type-safe, lambda-based ViewModelProvider.Factory for manual DI.
 */
inline fun <reified VM : ViewModel> viewModelFactory(
    crossinline creator: () -> VM
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return creator() as T
    }
}
