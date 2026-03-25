package com.packapp.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("PackPilot Erinnerung: $listName")
            .setContentText("$packed von $total Items gepackt ($percent%).")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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
        private const val CHANNEL_NAME = "PackPilot Erinnerungen"
        private const val CHANNEL_DESCRIPTION = "Erinnert dich ans rechtzeitige Packen"
        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing != null) return

            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = CHANNEL_DESCRIPTION
            }
            manager.createNotificationChannel(channel)
        }
    }
}
