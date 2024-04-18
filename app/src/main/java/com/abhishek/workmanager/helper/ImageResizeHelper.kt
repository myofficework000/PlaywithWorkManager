package com.abhishek.workmanager.helper

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.io.FileOutputStream


object ImageResizeHelper {
    var failureCount = 0

    fun resizeBitmap(context: Context, inputImageId: Int, maxWidth: Int, maxHeight: Int): String? {
        val imageToResize = ContextCompat.getDrawable(context, inputImageId)?.toBitmap() ?: return null

        var image: Bitmap? = null
        if (maxHeight > 0 && maxWidth > 0) {
            val width = imageToResize.width
            val height = imageToResize.height
            val ratioBitmap = width.toFloat() / height.toFloat()
            val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
            var finalWidth = maxWidth
            var finalHeight = maxHeight
            if (ratioMax > ratioBitmap) {
                finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
            } else {
                finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
            }
            image = Bitmap.createScaledBitmap(imageToResize, finalWidth, finalHeight, true)
        }

        if (image == null) return null

        val outputImageDirectory = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        ).path + File.separator + "resized"

        val outputImageName = "resized_${System.currentTimeMillis()}.jpg"

        val resizedImagePath = saveBitmapAsFile(
            image, outputImageDirectory, outputImageName
        )

        return resizedImagePath
    }

    fun saveBitmapAsFile(bitmap: Bitmap, directoryPath: String, fileName: String): String {
        val directory = File(directoryPath)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, fileName)
        val fileOutputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
        fileOutputStream.flush()
        fileOutputStream.close()
        return file.absolutePath
    }

    fun scanFile(context: Context, path: String, callback: (path: String?, uri: Uri?)-> Unit) {
        MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf("image/*")) { path, uri ->
            callback.invoke(path, uri)
        }
    }
}