package com.packapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "packing_sessions")
data class PackingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val listName: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val itemsPacked: Int = 0,
    val totalItems: Int = 0
) {
    val durationMillis: Long?
        get() = if (endTime != null) endTime - startTime else null

    val durationSeconds: Long?
        get() = durationMillis?.div(1000)

    val durationFormatted: String
        get() {
            val seconds = durationSeconds ?: return "--:--"
            val minutes = seconds / 60
            val secs = seconds % 60
            return String.format("%02d:%02d", minutes, secs)
        }

    val isCompleted: Boolean
        get() = endTime != null && totalItems > 0 && itemsPacked == totalItems
}
