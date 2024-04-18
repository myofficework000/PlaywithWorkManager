package com.abhishek.workmanager.worker

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.abhishek.workmanager.helper.AppConstants
import com.abhishek.workmanager.helper.ImageResizeHelper
import com.abhishek.workmanager.helper.NotificationHelper
import com.google.common.util.concurrent.ListenableFuture

class ImageResizerListenableFutureWorker(appContext: Context, workerParams: WorkerParameters) :
    ListenableWorker(appContext, workerParams) {
    override fun startWork(): ListenableFuture<Result> {
        return CallbackToFutureAdapter.getFuture {
            val imageId = inputData.getInt(AppConstants.IMAGE_ID, -1)
            val resizedImagePath = ImageResizeHelper.resizeBitmap(
                applicationContext, imageId, 500, 500
            )
            NotificationHelper.createNotification(
                applicationContext,
                text = "Image resized at : $resizedImagePath"
            )

            it.set(Result.success())
        }
    }
}