package com.packapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "packing_lists")
data class PackingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val tripId: Long? = null,
    val weatherLocation: String = "",
    val weatherForecastEpochDay: Long? = null,
    val remindersEnabled: Boolean = false,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val reminderTriggerAtMillis: Long? = null
)
