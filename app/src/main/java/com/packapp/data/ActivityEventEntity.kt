package com.packapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_events")
data class ActivityEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val listId: Long? = null,
    val itemId: Long? = null,
    val listName: String? = null,
    val itemTitle: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
