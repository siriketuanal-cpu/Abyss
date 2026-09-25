package com.example.abysstimer.ui

import com.example.abysstimer.data.ItemEntity

/**
 * Unified UI Actions representing all possible user operations.
 * Single source of truth for user interactions, drastically reducing lambda allocations.
 */
sealed interface TimerAction {
    data class CardShortTap(val item: ItemEntity) : TimerAction
    data class CancelPendingStates(val dummy: Unit = Unit) : TimerAction
    data class UpdateCurrentValue(val id: String, val current: Int) : TimerAction
    data class UpdateMaxValue(val id: String, val max: Int) : TimerAction
    data class MoveItemToTarget(val sourceId: String, val targetId: String) : TimerAction
    data class AddItem(
        val type: String,
        val name: String = "",
        val intervalMin: Int = 5,
        val max: Int = 100,
        val useChunk: Int? = null,
        val durationMin: Int = 0,
        val countMode: String? = "down",
        val orbMode: String? = "down",
        val color: String? = null,
        val parentId: String? = null
    ) : TimerAction
    data class AddGroup(val dummy: Unit = Unit) : TimerAction
    data class AddChildToGroup(
        val groupId: String,
        val type: String,
        val intervalMin: Int = 5,
        val max: Int = 100,
        val useChunk: Int? = null,
        val durationMin: Int = 0,
        val countMode: String? = "down",
        val orbMode: String? = "down"
    ) : TimerAction
    data class UpdateItemName(val id: String, val name: String) : TimerAction
    data class UpdateItemColor(val id: String, val color: String) : TimerAction
    data class UpdateItemSettings(
        val id: String,
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
    ) : TimerAction
    data class DeleteItem(val id: String) : TimerAction
    data class CloneGroup(val id: String) : TimerAction
    data class ToggleHeaderCollapsed(val id: String) : TimerAction
    data class SaveCustomColors(val colors: List<String>) : TimerAction
    data class RefreshNow(val dummy: Unit = Unit) : TimerAction
}
