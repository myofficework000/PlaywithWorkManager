package com.abhishek.workmanager.helper

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import com.abhishek.workmanager.R
import java.util.UUID
import kotlin.random.Random

object NotificationHelper {
    fun checkAndAskForNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isNotificationPermissionAvailable =
                ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)

            if (isNotificationPermissionAvailable != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
    }

    fun createNotification(
        applicationContext: Context,
        text: String
    ) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                    NotificationManager

        val id = "droidcon_channel_id"
        val title = "Worker"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    id,
                    title,
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }

    fun createImageResizerForegroundInfo(applicationContext: Context, id: UUID): ForegroundInfo {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                    NotificationManager

        val cancelIntent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)

        val notification = NotificationCompat.Builder(applicationContext, "droidcon_channel_id")
            .setContentTitle("Resizing image")
            .setSmallIcon(R.mipmap.ic_launcher)
            .addAction(
                R.drawable.ic_cancel,
                "Cancel",
                cancelIntent
            )
            .build()

        notificationManager.notify(id.hashCode(), notification)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(id.hashCode(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
        } else {
            ForegroundInfo(id.hashCode(), notification)
        }
    }
}