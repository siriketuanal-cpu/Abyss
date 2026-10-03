package com.example.abysstimer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.abysstimer.data.ItemEntity
import com.example.abysstimer.data.TimerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class TimerViewModelFactory(
    private val repository: TimerRepository,
    private val initialItems: List<ItemEntity> = emptyList()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
            val items = if (initialItems.isNotEmpty()) {
                initialItems
            } else {
                TimerRepository.inMemoryCache ?: try {
                    runBlocking(Dispatchers.IO) {
                        repository.getAllItems()
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            @Suppress("UNCHECKED_CAST")
            return TimerViewModel(repository, items) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
