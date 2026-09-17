package com.aistudio.abyss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aistudio.abyss.data.TimerRepository
import com.aistudio.abyss.ui.AbyssTheme
import com.aistudio.abyss.ui.TimerScreen
import com.aistudio.abyss.ui.TimerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = TimerRepository(applicationContext)
        val viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TimerViewModel(repository) as T
                }
            }
        )[TimerViewModel::class.java]

        setContent {
            AbyssTheme {
                TimerScreen(viewModel = viewModel)
            }
        }
    }
}
