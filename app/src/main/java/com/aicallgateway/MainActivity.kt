package com.aicallgateway

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    private val requestCode = 100
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val status = findViewById<TextView>(R.id.status)
        val number = findViewById<EditText>(R.id.number)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE), requestCode)
        }
        findViewById<Button>(R.id.call).setOnClickListener {
            val n = number.text.toString().trim()
            if (!n.matches(Regex("^\\+?[0-9]{7,15}$"))) { status.text = "Enter a valid phone number"; return@setOnClickListener }
            if (n == "999" || n == "112" || n.endsWith("999") && n.length <= 4) { status.text = "Emergency numbers are blocked"; return@setOnClickListener }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) { status.text = "Call permission required"; return@setOnClickListener }
            try {
                val telecom = getSystemService(TelecomManager::class.java)
                val extras = Bundle().apply { putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true) }
                telecom.placeCall(Uri.parse("tel:$n"), extras)
                status.text = "Call requested"
            } catch (e: Exception) { status.text = "Call failed: ${e.message}" }
        }
    }
}
