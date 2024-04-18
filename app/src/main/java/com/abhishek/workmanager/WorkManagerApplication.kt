package com.abhishek.workmanager

import android.app.Application
import androidx.work.Configuration

class WorkManagerApplication: Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
}