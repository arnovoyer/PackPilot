package com.packapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.packapp.ui.PackPilotRoot
import com.packapp.reminder.ReminderWorker

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderWorker.createChannel(this)
        setContent {
            PackPilotRoot()
        }
    }
}
