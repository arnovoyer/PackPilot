package com.packapp.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val UNIQUE_WORK_PREFIX = "packpilot_daily_reminder_"

    fun scheduleForList(context: Context, listId: Long, listName: String, hour: Int, minute: Int) {
        val input = Data.Builder()
            .putLong(ReminderWorker.INPUT_LIST_ID, listId)
            .putString(ReminderWorker.INPUT_LIST_NAME, listName)
            .build()

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelayMillis(hour, minute), TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()

        val uniqueWork = "$UNIQUE_WORK_PREFIX$listId"
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueWork,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelForList(context: Context, listId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("$UNIQUE_WORK_PREFIX$listId")
    }

    private fun calculateInitialDelayMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(1000L)
    }
}
