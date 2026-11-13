package com.macvision.northstar

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

/** The always-on guardian: keeps North Star alive, hosts the optional voice trigger, and runs the SOS pipeline. */
class SosService : Service() {

    companion object {
        const val ACTION_START = "com.macvision.northstar.START"
        const val ACTION_SOS = "com.macvision.northstar.SOS"
        private const val CH_ID = "northstar_guardian"
        private const val NOTIF_ID = 1001

        fun start(ctx: Context) {
            val i = Intent(ctx, SosService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }

        fun sosIntent(ctx: Context): Intent =
            Intent(ctx, SosService::class.java).setAction(ACTION_SOS)
    }

    private var voice: VoiceTrigger? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SOS -> fireSos()
            else -> if (Prefs.voiceEnabled(this)) startVoice() else stopVoice()
        }
        return START_STICKY
    }

    private fun fireSos() {
        SosSender.fire(this) { ok, _ -> vibrate(ok) }
    }

    private fun startVoice() {
        if (voice == null) {
            voice = VoiceTrigger(this) { fireSos() }.also { it.start() }
        }
    }

    private fun stopVoice() {
        voice?.stop()
        voice = null
    }

    override fun onDestroy() {
        stopVoice()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun vibrate(ok: Boolean) {
        val vib: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        val pattern = if (ok) longArrayOf(0, 120, 80, 120) else longArrayOf(0, 500)
        vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private fun createChannel() {
        val ch = NotificationChannel(CH_ID, "North Star Guardian", NotificationManager.IMPORTANCE_LOW)
        ch.description = "Keeps North Star ready to send an SOS."
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val sosPi = PendingIntent.getService(
            this, 1, sosIntent(this),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CH_ID)
            .setContentTitle("North Star is ready")
            .setContentText("Tap SOS to send your location to your contacts.")
            .setSmallIcon(R.drawable.ic_sos)
            .setOngoing(true)
            .setContentIntent(openApp)
            .addAction(R.drawable.ic_sos, "SOS", sosPi)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
