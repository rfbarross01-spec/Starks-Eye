package com.lume.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class LumeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val overlayChannel = NotificationChannel(
                CHANNEL_OVERLAY,
                "Lume ativo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantém a bolha do Lume visível sobre outros apps"
                setShowBadge(false)
            }
            nm.createNotificationChannel(overlayChannel)
        }
    }

    companion object {
        const val CHANNEL_OVERLAY = "lume_overlay_channel"
    }
}
