package com.example.abysstimer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.abysstimer.data.TimerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class TimerViewModelFactory(private val repository: TimerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
            // Fast synchronous preload (takes ~1-3ms) to guarantee first-frame UI readiness
            val initialItems = runCatching {
                runBlocking(Dispatchers.IO) {
                    repository.getAllItems()
                }
            }.getOrDefault(emptyList())

            @Suppress("UNCHECKED_CAST")
            return TimerViewModel(repository, initialItems) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
