package com.abhishek.workmanager.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.abhishek.workmanager.helper.AppConstants
import com.abhishek.workmanager.helper.ImageResizeHelper
import com.abhishek.workmanager.helper.NotificationHelper


class ImageResizerFailingWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val imageId = inputData.getInt(AppConstants.IMAGE_ID, -1)

        val resizedImagePath = ImageResizeHelper.resizeBitmap(applicationContext, imageId, 500, 500)

        if (ImageResizeHelper.failureCount < 2) {
            ImageResizeHelper.failureCount++

            NotificationHelper.createNotification(
                applicationContext,
                "Worker failed and Retrying ${ImageResizeHelper.failureCount}"
            )

            return Result.retry()
        }

        NotificationHelper.createNotification(
            applicationContext,
            "Image resized at :$resizedImagePath"
        )

        return Result.success(
            workDataOf(AppConstants.IMAGE_PATH to resizedImagePath)
        )
    }
}