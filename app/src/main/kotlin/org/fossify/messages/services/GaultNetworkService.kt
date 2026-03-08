package org.fossify.messages.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.fossify.messages.R

class GaultNetworkService : Service() {

    private val CHANNEL_ID = "GaultNetworkChannel"

    private companion object {
        private const val TAG = "GEFE"
        private const val GAULT_NAMESPACE = "#GaultProtocol"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Gault Network Active")
            .setContentText("Broadcasting on #GaultProtocol")
            .setSmallIcon(R.drawable.ic_messenger_vector) // Using standard Fossify icon
            .build()
        
        startForeground(1, notification)
        Thread({ initialiseGaultNetwork() }, "GaultNetworkInit").start()
    }

    private fun initialiseGaultNetwork() {
        try {
            // 1) Access the BiglyBT Core singleton
            val core = com.biglysoftware.android.core.Core.getAnyInstance()

            // 2) Initialize the Distributed Database (DHT)
            val ddb = core.distributedDatabase

            // 3) Register the Gault Protocol namespace
            ddb.registerNamespace(GAULT_NAMESPACE.toByteArray())

            Log.d(TAG, "Gault Network Resonance: STABLE on $GAULT_NAMESPACE")
        } catch (e: Exception) {
            Log.e(TAG, "Static Overtone Error in DHT Init: ${e.message}", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID, "Gault Network Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}