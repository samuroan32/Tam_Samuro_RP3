package com.twinscalev3.notification

object ChatPresenceTracker {
    @Volatile var isChatOpen: Boolean = false
    @Volatile var activePartnerId: String? = null
}
