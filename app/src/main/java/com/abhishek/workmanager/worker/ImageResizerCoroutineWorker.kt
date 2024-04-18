package com.abhishek.workmanager.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abhishek.workmanager.helper.AppConstants
import com.abhishek.workmanager.helper.ImageResizeHelper
import com.abhishek.workmanager.helper.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageResizerCoroutineWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val inputImageId = inputData.getInt(AppConstants.IMAGE_ID, -1)

            val resizedImagePath = ImageResizeHelper.resizeBitmap(
                applicationContext, inputImageId, 500, 500
            )

            NotificationHelper.createNotification(
                applicationContext,
                "Image resized at: $resizedImagePath"
            )

            Result.success()
        }
    }
}