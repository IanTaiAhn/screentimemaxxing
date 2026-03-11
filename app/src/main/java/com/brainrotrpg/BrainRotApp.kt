package com.brainrotrpg

import android.app.Application

class BrainRotApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WorkScheduler.schedulePeriodicTracking(this)
    }
}
