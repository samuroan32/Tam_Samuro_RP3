package com.twinscalev3.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TwinScaleMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Forward token to repository from AppViewModel on next app launch / login.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val partnerId = message.data["senderId"]
        val body = message.data["text"] ?: message.notification?.body ?: "New message"
        val title = message.data["title"] ?: "TwinScaleV3"

        val shouldSuppress = ChatPresenceTracker.isChatOpen && ChatPresenceTracker.activePartnerId == partnerId
        if (!shouldSuppress) {
            NotificationHelper.showMessage(
                context = applicationContext,
                title = title,
                body = body,
                notificationId = System.currentTimeMillis().toInt()
            )
        }
    }
}
