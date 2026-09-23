package com.example.abysstimer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val type: String, // "stam", "orb", "idle", "exped", "group", "header", "rule"
    val name: String = "",
    val current: Int = 0,
    val max: Int = 0,
    val intervalMin: Int = 0,
    val start: Long = System.currentTimeMillis(),
    val useChunk: Int? = null,
    val orbMode: String? = null, // "down" or "up"
    val durationMin: Int = 0,
    val countMode: String? = null, // "down" or "up"
    val state: String? = null, // "running" or "claim"
    val color: String? = null, // Hex color
    val collapsed: Boolean = false,
    val foldLock: Boolean = false,
    val layout: String? = null, // "regular" or "2x2"
    val parentId: String? = null, // if nested inside a group
    val position: Int = 0
)
