package com.abhishek.workmanager.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.abhishek.workmanager.helper.AppConstants
import com.abhishek.workmanager.helper.ImageResizeHelper
import com.abhishek.workmanager.helper.NotificationHelper

class ImageSyncWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val imagePath = inputData.getString(AppConstants.IMAGE_PATH) ?: ""
        ImageResizeHelper.scanFile(applicationContext, imagePath, { _, _ -> })

        NotificationHelper.createNotification(
            applicationContext,
            "Image sync with Gallery"
        )

        return Result.success()
    }
}