package com.packapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.packapp.ui.PackPilotRoot
import com.packapp.reminder.ReminderWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderWorker.createChannel(this)
        setContent {
            PackPilotRoot()
        }
    }
}
