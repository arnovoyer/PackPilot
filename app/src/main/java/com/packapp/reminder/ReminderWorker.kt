package com.packapp.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.packapp.MainActivity
import com.packapp.R
import com.packapp.data.PackDatabase

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        createChannel(applicationContext)

        val listId = inputData.getLong(INPUT_LIST_ID, -1L)
        if (listId <= 0L) return Result.success()
        val listName = inputData.getString(INPUT_LIST_NAME).orEmpty().ifBlank { "deine Liste" }

        val dao = PackDatabase.getInstance(applicationContext).packDao()
        val total = dao.getTotalCountNow(listId)
        val packed = dao.getPackedCountNow(listId)
        val percent = if (total == 0) 0 else ((packed * 100f) / total).toInt()
        val text = applicationContext.getString(
            R.string.notification_body_progress,
            packed,
            total,
            percent
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            listId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.notification_title, listName))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .addAction(0, applicationContext.getString(R.string.notification_action_open), pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(listId.toInt(), notification)

        return Result.success()
    }

    companion object {
        const val INPUT_LIST_ID = "input_list_id"
        const val INPUT_LIST_NAME = "input_list_name"
        const val CHANNEL_ID = "packpilot_reminders"
        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing != null) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
