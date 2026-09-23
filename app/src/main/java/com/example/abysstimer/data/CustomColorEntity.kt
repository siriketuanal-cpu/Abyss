package com.example.abysstimer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_colors")
data class CustomColorEntity(
    @PrimaryKey val slotIndex: Int, // 0 to 5
    val hexColor: String
)
