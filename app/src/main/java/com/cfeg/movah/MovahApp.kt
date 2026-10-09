package com.cfeg.movah

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MovahApp : Application() {

    companion object {
        const val CHANNEL_ID = "movah_service_channel"
        const val CHANNEL_NAME = "File Mover Service"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress during scheduled file transfers"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
