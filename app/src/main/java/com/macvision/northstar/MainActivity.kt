package com.macvision.northstar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.macvision.northstar.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { /* re-checked on next action */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.inputNumber.setText(Prefs.panicNumber(this))
        b.inputName.setText(Prefs.userName(this))
        b.switchVoice.isChecked = Prefs.voiceEnabled(this)

        b.btnSos.setOnClickListener { confirmAndFire() }

        b.btnSave.setOnClickListener {
            Prefs.setPanicNumber(this, b.inputNumber.text.toString())
            Prefs.setUserName(this, b.inputName.text.toString())
            Prefs.setVoiceEnabled(this, b.switchVoice.isChecked)
            SosService.start(this)
            Toast.makeText(this, "Saved. North Star is armed.", Toast.LENGTH_SHORT).show()
        }

        b.switchVoice.setOnCheckedChangeListener { _, checked ->
            Prefs.setVoiceEnabled(this, checked)
            if (checked) ensurePermissions(true)
            SosService.start(this)
        }

        ensurePermissions(false)
        SosService.start(this)
    }

    private fun confirmAndFire() {
        AlertDialog.Builder(this)
            .setTitle("Send SOS now?")
            .setMessage("This texts your location to your North Star panic line.")
            .setPositiveButton("SEND SOS") { _, _ ->
                startService(SosService.sosIntent(this))
                Toast.makeText(this, "SOS sent.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun ensurePermissions(withMic: Boolean) {
        fun miss(p: String) = ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED
        val need = mutableListOf<String>()
        if (miss(Manifest.permission.SEND_SMS)) need += Manifest.permission.SEND_SMS
        if (miss(Manifest.permission.ACCESS_FINE_LOCATION)) need += Manifest.permission.ACCESS_FINE_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && miss(Manifest.permission.POST_NOTIFICATIONS))
            need += Manifest.permission.POST_NOTIFICATIONS
        if (withMic && miss(Manifest.permission.RECORD_AUDIO)) need += Manifest.permission.RECORD_AUDIO
        if (need.isNotEmpty()) permLauncher.launch(need.toTypedArray())
    }
}
