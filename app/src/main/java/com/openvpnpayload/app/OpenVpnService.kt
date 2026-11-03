package com.openvpnpayload.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * هيكل VpnService.
 * حالياً كيشغل غير الـ Payload Proxy.
 * من بعد تقدر تزيد تكامل ics-openvpn هنا.
 */
class OpenVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.openvpnpayload.app.START"
        const val ACTION_STOP = "com.openvpnpayload.app.STOP"
        const val EXTRA_HOST = "extra_host"
        const val EXTRA_PORT = "extra_port"
        const val EXTRA_UA = "extra_ua"

        private const val TAG = "OpenVpnService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "openvpn_payload_channel"
    }

    private var proxy: PayloadProxy? = null
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val host = intent.getStringExtra(EXTRA_HOST) ?: "example.com"
                val port = intent.getIntExtra(EXTRA_PORT, 80)
                val ua = intent.getStringExtra(EXTRA_UA) ?: "Mozilla/5.0 (Linux; Android 13)"

                startProxy(host, port, ua)
                startForeground(NOTIFICATION_ID, createNotification("Payload Proxy شغال"))
            }
            ACTION_STOP -> {
                stopProxy()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startProxy(host: String, port: Int, ua: String) {
        stopProxy()
        proxy = PayloadProxy(
            listenPort = 1080,
            targetHost = host,
            targetPort = port,
            userAgent = ua
        )
        proxy?.start()
        Log.i(TAG, "Proxy started for $host:$port")
    }

    private fun stopProxy() {
        proxy?.stop()
        proxy = null
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
    }

    private fun createNotification(content: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OpenVPN Payload",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, OpenVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OpenVPN + Payload")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف", stopPending)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopProxy()
        super.onDestroy()
    }

    // هنا من بعد تقدر تزيد Builder ديال VpnService باش تدير TUN interface
    // مثال:
    // val builder = Builder()
    //     .addAddress("10.0.0.2", 32)
    //     .addRoute("0.0.0.0", 0)
    //     .setSession("OpenVpnPayload")
    // vpnInterface = builder.establish()
}
