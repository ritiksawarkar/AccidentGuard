
package com.accidentguard.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.accidentguard.MainActivity
import com.accidentguard.R
import com.accidentguard.data.api.RetrofitClient
import com.accidentguard.data.local.TokenManager
import com.accidentguard.data.model.FcmTokenRequest
import com.accidentguard.ui.notification.NotificationHelper
import com.accidentguard.ui.notification.RingtonePlayer
import com.accidentguard.data.service.CallService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AccidentGuardMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_Service"
        const val CHANNEL_ID = "accident_alerts_channel"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d(TAG, "FCM token refreshed")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        val tokenManager = TokenManager(applicationContext)
        val jwtToken = tokenManager.getAuthToken()

        if (!jwtToken.isNullOrEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.apiService.updateFcmToken(
                        authHeader = "Bearer $jwtToken",
                        request = FcmTokenRequest(fcmToken = token)
                    )

                    if (response.isSuccessful) {
                        Log.d(
                            TAG,
                            "FCM token registered successfully on server"
                        )
                    } else {
                        Log.e(
                            TAG,
                            "Failed to register FCM token: ${response.code()}"
                        )
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error registering FCM token", e)
                }
            }
        } else {
            Log.d(
                TAG,
                "User not logged in; FCM token not sent to server yet."
            )
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val type = remoteMessage.data["type"]

        // Handle all emergency call events separately.
        if (type?.startsWith("EMERGENCY_CALL") == true) {
            handleCallNotification(type, remoteMessage.data)
            return
        }

        // Handle visible notification payload.
        remoteMessage.notification?.let {
            val title = it.title ?: "AccidentGuard Alert"
            val body = it.body ?: ""

            val victimName = remoteMessage.data["victim_name"]
            val location = remoteMessage.data["location"]

            val finalBody =
                if (
                    victimName != null &&
                    location != null &&
                    !body.contains(victimName)
                ) {
                    "EMERGENCY: $victimName may have been in an accident near $location."
                } else {
                    body
                }

            val helper = NotificationHelper(applicationContext)
            helper.showNotification(
                title = title,
                message = finalBody,
                isCritical = true,
                navigateTo = if (TokenManager(applicationContext).isHospital()) "hospital_dashboard" else "dashboard"
            )
            return
        }

        // Handle data-only accident notifications.
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"]
                ?: "AccidentGuard Emergency"

            val body = remoteMessage.data["body"]
                ?: "A crash has been detected."

            val helper = NotificationHelper(applicationContext)
            helper.showNotification(
                title = title,
                message = body,
                isCritical = true,
                navigateTo = if (TokenManager(applicationContext).isHospital()) "hospital_dashboard" else "dashboard"
            )
        }
    }

    // ---------------------------------------------------------
    // HANDLE EMERGENCY CALL EVENTS
    // ---------------------------------------------------------

    private fun handleCallNotification(
        type: String,
        data: Map<String, String>
    ) {
        val helper = NotificationHelper(applicationContext)

        val sessionIdStr = data["session_id"]
        val sessionId = sessionIdStr?.toIntOrNull()

        if (sessionId == null) {
            Log.e(
                TAG,
                "Invalid call session ID: $sessionIdStr"
            )
            return
        }

        val victimName = data["victim_name"] ?: "Someone"

        Log.d(
            TAG,
            "Handling call event: $type for session: $sessionId"
        )

        when (type) {

            // -------------------------------------------------
            // NEW EMERGENCY CALL INVITATION
            // -------------------------------------------------

            "EMERGENCY_CALL_INVITATION" -> {
                helper.showCallInvitation(
                    sessionId,
                    victimName
                )
            }

            // -------------------------------------------------
            // EMERGENCY CALL ENDED
            // -------------------------------------------------

            "EMERGENCY_CALL_ENDED" -> {
                Log.d(
                    TAG,
                    "Remote call ended. Stopping local CallService."
                )

                stopLocalCall(sessionId, helper)
            }

            // -------------------------------------------------
            // ALL RESPONDERS REJECTED / CALL FAILED
            // -------------------------------------------------

            "EMERGENCY_CALL_FAILED" -> {
                Log.w(
                    TAG,
                    "All responders rejected. Stopping emergency call."
                )

                stopLocalCall(sessionId, helper)

                helper.showNotification(
                    "Emergency Call Failed",
                    "None of your emergency contacts accepted the call.",
                    isCritical = false
                )
            }

            // -------------------------------------------------
            // OTHER CALL EVENTS
            // -------------------------------------------------

            "EMERGENCY_CALL_CANCELLED",
            "EMERGENCY_CALL_ALREADY_CONNECTED",
            "EMERGENCY_CALL_MISSED",
            "EMERGENCY_CALL_ACCEPTED" -> {

                RingtonePlayer.stopRinging()

                helper.cancelCallNotification(sessionId)

                val title = when (type) {

                    "EMERGENCY_CALL_CANCELLED" ->
                        "Call Cancelled"

                    "EMERGENCY_CALL_ALREADY_CONNECTED" ->
                        "Call Answered"

                    "EMERGENCY_CALL_MISSED" ->
                        "Missed Emergency Call"

                    "EMERGENCY_CALL_ACCEPTED" ->
                        "Call Accepted"

                    else ->
                        "Emergency Call Update"
                }

                val body = when (type) {

                    "EMERGENCY_CALL_CANCELLED" ->
                        "The emergency invitation for $victimName has been cancelled."

                    "EMERGENCY_CALL_ALREADY_CONNECTED" ->
                        "Another contact has already joined the call for $victimName."

                    "EMERGENCY_CALL_MISSED" ->
                        "You missed an emergency call from $victimName."

                    "EMERGENCY_CALL_ACCEPTED" ->
                        "The emergency call for $victimName was accepted."

                    else ->
                        "The emergency call status has changed."
                }

                if (type != "EMERGENCY_CALL_ACCEPTED") {
                    helper.showNotification(
                        title,
                        body,
                        isCritical = false
                    )
                }
            }

            else -> {
                Log.w(
                    TAG,
                    "Unknown emergency call event: $type"
                )
            }
        }
    }

    // ---------------------------------------------------------
    // STOP LOCAL CALL SERVICE
    // ---------------------------------------------------------

    private fun stopLocalCall(
        sessionId: Int,
        helper: NotificationHelper
    ) {
        // Stop incoming call ringtone.
        RingtonePlayer.stopRinging()

        // Remove call invitation notification.
        helper.cancelCallNotification(sessionId)

        // Stop active foreground call service.
        val stopIntent = Intent(
            applicationContext,
            CallService::class.java
        ).apply {
            action = CallService.ACTION_STOP_SERVICE
            putExtra(CallService.EXTRA_SESSION_ID, sessionId)
        }

        try {
            applicationContext.startService(stopIntent)

            Log.d(
                TAG,
                "CallService stop command sent for session: $sessionId"
            )

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to send CallService stop command",
                e
            )
        }
    }

}
