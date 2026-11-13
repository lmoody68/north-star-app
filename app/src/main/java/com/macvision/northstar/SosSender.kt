package com.macvision.northstar

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CancellationTokenSource
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * The heart of North Star: get the best location we can quickly, then TEXT it to the panic line.
 * The body embeds raw "lat,lon" which the North Star backend (Phase B) parses directly — no data/link needed.
 */
object SosSender {
    private const val TAG = "NorthStar"

    @SuppressLint("MissingPermission")
    fun fire(ctx: Context, onDone: ((Boolean, String) -> Unit)? = null) {
        val number = Prefs.panicNumber(ctx)
        val name = Prefs.userName(ctx)

        if (!hasLocationPermission(ctx)) {
            val ok = sendSms(ctx, number, composeBody(name, null))
            onDone?.invoke(ok, if (ok) "sent (no location permission)" else "SMS failed")
            return
        }

        val fused = LocationServices.getFusedLocationProviderClient(ctx)
        var finished = false
        fun complete(loc: Location?) {
            if (finished) return
            finished = true
            val ok = sendSms(ctx, number, composeBody(name, loc))
            onDone?.invoke(ok, if (ok) "sent" else "SMS failed")
        }

        try {
            fused.lastLocation.addOnSuccessListener { last ->
                val cts = CancellationTokenSource()
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { fresh -> complete(fresh ?: last) }
                    .addOnFailureListener { complete(last) }
            }.addOnFailureListener { complete(null) }
        } catch (e: SecurityException) {
            complete(null)
        }

        // Safety net: never hang — send with whatever we have after 8s.
        Handler(Looper.getMainLooper()).postDelayed({ complete(null) }, 8000)
    }

    private fun composeBody(name: String, loc: Location?): String {
        val who = if (name.isNotBlank()) "$name " else ""
        return if (loc != null) "SOS ${who}needs help. ${loc.latitude},${loc.longitude}"
        else "SOS ${who}needs help. Location unavailable right now."
    }

    private fun sendSms(ctx: Context, number: String, body: String): Boolean {
        return try {
            val sms = smsManager(ctx)
            val parts = sms.divideMessage(body)
            if (parts.size > 1) sms.sendMultipartTextMessage(number, null, parts, null, null)
            else sms.sendTextMessage(number, null, body, null, null)
            Log.i(TAG, "SOS sent to $number: $body")
            true
        } catch (e: Exception) {
            Log.e(TAG, "SMS send failed", e)
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun smsManager(ctx: Context): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ctx.getSystemService(SmsManager::class.java)
        else SmsManager.getDefault()

    fun hasLocationPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}
