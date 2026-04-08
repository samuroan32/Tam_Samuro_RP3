package com.twinscalev3

import android.app.Application
import com.twinscalev3.notification.NotificationHelper

class TwinScaleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
