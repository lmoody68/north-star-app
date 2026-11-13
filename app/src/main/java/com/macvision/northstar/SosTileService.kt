package com.macvision.northstar

import android.os.Build
import android.service.quicksettings.TileService

/** Lock-screen Quick Settings tile → one tap fires the SOS (even without unlocking, if allowed). */
class SosTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val i = SosService.sosIntent(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
    }
}
