package com.packapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val location: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val completedDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isCompleted: Boolean
        get() = completedDate != null
}
