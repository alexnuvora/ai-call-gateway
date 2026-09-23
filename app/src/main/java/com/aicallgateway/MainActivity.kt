package com.aicallgateway

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.content.Intent
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
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
        handleApprovedIntent(intent, number, status)
        createApprovalChannel()

        val permissions = buildList {
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.ANSWER_PHONE_CALLS)
            if (android.os.Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleApprovedIntent(intent, findViewById(R.id.number), findViewById(R.id.status))
    }

    private fun handleApprovedIntent(intent: Intent, numberView: EditText, status: TextView) {
        if (intent.action != ACTION_APPROVE_CALL) return
        val phone = intent.getStringExtra(EXTRA_PHONE).orEmpty()
        numberView.setText(phone)
        status.text = executeApprovedCommand("call", phone).message
        intent.action = null
    }

    private fun createApprovalChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            APPROVAL_CHANNEL, "Approved call requests", NotificationManager.IMPORTANCE_HIGH
        ))
    }

    // The network consumer will call this after receiving a server-side approved request.
    // It never dials from the network callback: the user must tap the notification action.
    private fun showCallApproval(phone: String) {
        if (!validNumber(phone)) return
        val approve = Intent(this, MainActivity::class.java).apply {
            action = ACTION_APPROVE_CALL
            putExtra(EXTRA_PHONE, phone)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this, phone.hashCode(), approve,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, APPROVAL_CHANNEL)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("Vorlen call request")
            .setContentText("Call $phone")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.sym_action_call, "Approve & call", pending)
            .build()
        if (android.os.Build.VERSION.SDK_INT < 33 ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            getSystemService(NotificationManager::class.java).notify(phone.hashCode(), notification)
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


    // Entry point used by an approved command consumer. It deliberately delegates
    // to the same verified telephony implementation as the on-screen button.
    private fun executeApprovedCommand(action: String, phoneNumber: String?): CallResult {
        return when (action.lowercase()) {
            "call" -> {
                if (phoneNumber.isNullOrBlank()) CallResult(false, "Phone number required")
                else placeSimCall(phoneNumber)
            }
            "hangup" -> endSimCall()
            else -> CallResult(false, "Unsupported command")
        }
    }

    companion object {
        private const val ACTION_APPROVE_CALL = "com.aicallgateway.APPROVE_CALL"
        private const val EXTRA_PHONE = "phone_number"
        private const val APPROVAL_CHANNEL = "approved_calls"
    }

    private fun validNumber(n: String): Boolean {
        if (!n.matches(Regex("^\\+?[0-9]{7,15}$"))) return false
        val digits = n.filter(Char::isDigit)
        return digits !in setOf("999","112","911","000")
    }
}
