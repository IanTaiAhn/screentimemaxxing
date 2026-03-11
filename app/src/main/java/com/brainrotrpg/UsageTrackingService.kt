package com.brainrotrpg

import android.app.Service
import android.content.Intent
import android.os.IBinder

class UsageTrackingService : Service() {

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO (Task 4.1): Implement usage tracking logic via UsageTrackingWorker
        return START_NOT_STICKY
    }
}
