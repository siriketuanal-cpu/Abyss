package com.example.abysstimer.ui

import androidx.compose.runtime.Immutable
import com.example.abysstimer.data.ItemEntity

/**
 * Type-safe settings update parameters, completely replacing fragile Map<String, Any?>.
 * Eliminates boxing, string lookup overhead, and silent typos.
 */
@Immutable
data class TimerSettingsUpdate(
    val intervalMin: Int? = null,
    val max: Int? = null,
    val useChunk: Int? = null,
    val useChunkClear: Boolean = false,
    val durationMin: Int? = null,
    val countMode: String? = null,
    val orbMode: String? = null,
    val layout: String? = null,
    val foldLock: Boolean? = null,
    val orbEditHours: Int? = null,
    val orbEditMinutes: Int? = null,
    val idleEditHours: Int? = null,
    val idleEditMinutes: Int? = null
)

/**
 * Type-safe creation parameters for SetupDialog.
 */
@Immutable
data class TimerCreationParams(
    val intervalMin: Int = 5,
    val max: Int = 100,
    val useChunk: Int? = null,
    val durationMin: Int = 0,
    val countMode: String? = "down",
    val orbMode: String? = "down",
    val color: String? = null
)
