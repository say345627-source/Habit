package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.SettingsDataStore
import com.example.data.local.WateeraDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habits = WateeraDatabase.getInstance(appContext).habitDao().getAllHabitsOnce()
                val settings = SettingsDataStore(appContext).getSettingsOnce()
                ReminderScheduler(appContext).syncAll(habits, settings)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
