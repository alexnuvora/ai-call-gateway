package com.aicallgateway

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    private val requestCode = 100
    private lateinit var stateView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val status = findViewById<TextView>(R.id.status)
        stateView = findViewById(R.id.callState)
        val number = findViewById<EditText>(R.id.number)

        val permissions = arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.ANSWER_PHONE_CALLS)
        if (permissions.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, requestCode)
        }

        @Suppress("DEPRECATION")
        (getSystemService(TELEPHONY_SERVICE) as TelephonyManager).listen(object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                stateView.text = "Call state: " + when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> "ringing"
                    TelephonyManager.CALL_STATE_OFFHOOK -> "active"
                    else -> "idle / ended"
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE)

        findViewById<Button>(R.id.call).setOnClickListener {
            val result = placeSimCall(number.text.toString().trim())
            status.text = result.message
        }

        findViewById<Button>(R.id.hangup).setOnClickListener {
            status.text = endSimCall().message
        }
    }

    data class CallResult(val success: Boolean, val message: String)

    private fun placeSimCall(number: String): CallResult {
        if (!validNumber(number)) return CallResult(false, "Enter a valid non-emergency number")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return CallResult(false, "Call permission required")
        }
        return try {
            val extras = Bundle().apply { putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true) }
            getSystemService(TelecomManager::class.java).placeCall(Uri.parse("tel:$number"), extras)
            CallResult(true, "Call requested")
        } catch (e: Exception) {
            CallResult(false, "Call failed: ${e.message}")
        }
    }

    private fun endSimCall(): CallResult {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            return CallResult(false, "Phone-control permission required")
        }
        return try {
            @Suppress("DEPRECATION")
            val ended = getSystemService(TelecomManager::class.java).endCall()
            CallResult(ended, if (ended) "Call ended" else "No active call could be ended")
        } catch (e: Exception) {
            CallResult(false, "Hang-up failed: ${e.message}")
        }
    }

    private fun validNumber(n: String): Boolean {
        if (!n.matches(Regex("^\\+?[0-9]{7,15}$"))) return false
        val digits = n.filter(Char::isDigit)
        return digits !in setOf("999","112","911","000")
    }
}
