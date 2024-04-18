package com.abhishek.workmanager

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.abhishek.workmanager.helper.AppConstants
import com.abhishek.workmanager.helper.NotificationHelper
import com.abhishek.workmanager.helper.WorkerType
import com.abhishek.workmanager.ui.theme.WorkManagerTheme
import com.abhishek.workmanager.worker.ImageResizerCoroutineWorker
import com.abhishek.workmanager.worker.ImageResizerFailingWorker
import com.abhishek.workmanager.worker.ImageResizerForegroundWorker
import com.abhishek.workmanager.worker.ImageResizerListenableFutureWorker
import com.abhishek.workmanager.worker.ImageResizerObservableWorker
import com.abhishek.workmanager.worker.ImageResizerRxWorker
import com.abhishek.workmanager.worker.ImageResizerWorker
import com.abhishek.workmanager.worker.ImageSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val workManager by lazy {
        WorkManager.getInstance(this)
    }

    private var workId: UUID? = null
    private var workerTag: String = "ImageResizerWorkerTag"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WorkManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Resizer()
                }
            }
        }

        NotificationHelper.checkAndAskForNotificationPermission(this)
    }

    @Composable
    private fun Resizer() {
        var taskProgress by remember {
            mutableStateOf(0f)
        }

        var outputPath by remember {
            mutableStateOf("")
        }

        var isTaskCompleted by remember {
            mutableStateOf(false)
        }

        val coroutineScope = rememberCoroutineScope()

        if (isTaskCompleted) {
            ImageOutputPreview(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                outputPath = outputPath
            ) {
                taskProgress = 0f
                outputPath = ""
                isTaskCompleted = false
            }
        } else {
            ImageResizer(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                progress = taskProgress
            ) { workerType ->
                when (workerType) {
                    is WorkerType.Worker -> {
                        startImageResizer()
                    }

                    is WorkerType.CoroutineWorker -> {
                        startImageResizerCoroutine()
                    }

                    is WorkerType.ListenableFutureWorker -> {
                        startImageResizerListenableFuture()
                    }

                    is WorkerType.RxWorker -> {
                        startImageResizerRx()
                    }

                    is WorkerType.ChainedWork -> {
                        startImageResizerChainedWork()
                    }

                    is WorkerType.RetryingWork -> {
                        startImageResizerWithRetry()
                    }

                    is WorkerType.ConstrainedWork -> {
                        startImageResizerWithConstraints()
                    }

                    is WorkerType.PeriodicWork -> {
                        startImageResizerPeriodically()
                    }

                    is WorkerType.CancelWork -> {
                        cancelImageResizerWorker()
                    }

                    is WorkerType.ExpeditedWork -> {
                        startImageResizerExpedited()
                    }

                    is WorkerType.ForegroundWork -> {
                        startImageResizerForeground()
                    }

                    is WorkerType.ObservableWork -> {
                        if (isTaskCompleted) {
                            return@ImageResizer
                        }

                        startImageResizerObservable(coroutineScope) { progress ->
                            taskProgress = progress
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ImageResizer(
        modifier: Modifier = Modifier,
        progress: Float,
        onResizeClick: (workerType: WorkerType) -> Unit
    ) {
        val workerTypesList = listOf(
            WorkerType.Worker(),
            WorkerType.CoroutineWorker(),
            WorkerType.ListenableFutureWorker(),
            WorkerType.RxWorker(),
            WorkerType.ChainedWork(),
            WorkerType.RetryingWork(),
            WorkerType.ConstrainedWork(),
            WorkerType.PeriodicWork(),
            WorkerType.CancelWork(),
            WorkerType.ExpeditedWork(),
            WorkerType.ForegroundWork(),
            WorkerType.ObservableWork(),
        )

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 16.dp, bottom = 16.dp),
                text = "Resize Image",
                textAlign = TextAlign.Center,
                fontSize = MaterialTheme.typography.titleLarge.fontSize
            )
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, true)
                    .padding(top = 16.dp, bottom = 16.dp),
                painter = painterResource(id = R.drawable.image),
                contentDescription = "Image To Resize"
            )
            Column {
                workerTypesList.forEach { workerType ->
                    ElevatedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(top = 2.dp, bottom = 2.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            onResizeClick(workerType)
                        }
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            text = workerType.ctaText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(
                        top = 16.dp, bottom = 16.dp
                    ),
                progress = progress / 100f
            )
        }
    }

    @Composable
    fun ImageOutputPreview(
        modifier: Modifier = Modifier,
        outputPath: String,
        onBackClicked: () -> Unit
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 16.dp, bottom = 16.dp),
                text = "Output Image",
                textAlign = TextAlign.Center,
                fontSize = MaterialTheme.typography.titleLarge.fontSize
            )
            AsyncImage(
                model = outputPath,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, true)
                    .padding(top = 16.dp, bottom = 16.dp),
                contentDescription = "Resized Image"
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                text = outputPath,
                textAlign = TextAlign.Center
            )
            ElevatedButton(
                modifier = Modifier
                    .align(Alignment.End)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 16.dp, bottom = 16.dp),
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary),
                onClick = onBackClicked
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    text = "Go Back",
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    private fun startImageResizer() {
        workId = UUID.randomUUID()
        val imageResizerWorkerRequest = OneTimeWorkRequestBuilder<ImageResizerWorker>()
            .setInputData(
                workDataOf(
                    AppConstants.IMAGE_ID to R.drawable.image
                )
            ).setId(workId!!).build()

        workManager.enqueue(imageResizerWorkerRequest)
    }

    private fun startImageResizerCoroutine() {
        val imageResizerWorkerRequest = OneTimeWorkRequestBuilder<ImageResizerCoroutineWorker>()
            .setInputData(
                workDataOf(
                    AppConstants.IMAGE_ID to R.drawable.image
                )
            ).build()

        workManager.enqueueUniqueWork(
            "ImageResizerWorker",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            imageResizerWorkerRequest
        )
    }

    private fun startImageResizerListenableFuture() {
        val imageResizerWorkerRequest =
            OneTimeWorkRequestBuilder<ImageResizerListenableFutureWorker>()
                .setInputData(
                    workDataOf(
                        AppConstants.IMAGE_ID to R.drawable.image
                    )
                ).build()

        workManager.enqueue(imageResizerWorkerRequest)
    }

    private fun startImageResizerRx() {
        val imageResizerWorkerRequest =
            OneTimeWorkRequestBuilder<ImageResizerRxWorker>()
                .setInputData(
                    workDataOf(
                        AppConstants.IMAGE_ID to R.drawable.image
                    )
                ).build()

        workManager.enqueue(imageResizerWorkerRequest)
    }

    private fun startImageResizerChainedWork() {
        val imageResizerWorkerRequest =
            OneTimeWorkRequestBuilder<ImageResizerWorker>()
                .setInputData(
                    workDataOf(
                        AppConstants.IMAGE_ID to R.drawable.image
                    )
                ).addTag(workerTag).build()

        val imageSyncWorkRequest = OneTimeWorkRequestBuilder<ImageSyncWorker>()
            .addTag(workerTag).build()

       val task3= OneTimeWorkRequestBuilder<ImageResizerRxWorker>()
            .setInputData(
                workDataOf(
                    AppConstants.IMAGE_ID to R.drawable.image
                )
            ).addTag(workerTag).build()

        workManager.beginWith(imageResizerWorkerRequest)
            .then(imageSyncWorkRequest)
            .then(task3)
            .enqueue()
    }

    private fun startImageResizerWithRetry() {
        val imageResizerWorkerRequest =
            OneTimeWorkRequestBuilder<ImageResizerFailingWorker>()
                .setInputData(
                    workDataOf(
                        AppConstants.IMAGE_ID to R.drawable.image
                    )
                ).setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    2,
                    TimeUnit.SECONDS
                )
                .build()

        workManager.enqueue(imageResizerWorkerRequest)
    }

    private fun startImageResizerWithConstraints() {
        val imageResizerWorkerRequest =
            OneTimeWorkRequestBuilder<ImageResizerWorker>()
                .setInputData(
                    workDataOf(
                        AppConstants.IMAGE_ID to R.drawable.image
                    )
                ).setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresCharging(true)
                        .apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                setRequiresDeviceIdle(false)
                            }
                        }
                        .setRequiresBatteryNotLow(true)
                        .setRequiresStorageNotLow(true)
                        .build()
                ).build()

        workManager.enqueue(imageResizerWorkerRequest)
    }

    private fun startImageResizerPeriodically() {
        workId = UUID.randomUUID()
        val imageResizerWorkerRequest =
            PeriodicWorkRequestBuilder<ImageResizerWorker>(
                15, TimeUnit.MINUTES
            ).setInputData(
                workDataOf(
                    AppConstants.IMAGE_ID to R.drawable.image
                )
            ).setId(workId!!).build()

        workManager.enqueue(imageResizerWorkerRequest)
    }

    private fun cancelImageResizerWorker() {
        workManager.cancelWorkById(workId!!)
        // workManager.cancelUniqueWork("ImageResizerWorker")
        // workManager.cancelAllWorkByTag(workerTag)
        // workManager.cancelAllWork()
    }

    private fun startImageResizerExpedited() {
        val imageResizerWorkerRequest =
            OneTimeWorkRequestBuilder<ImageResizerWorker>().setInputData(
                workDataOf(
                    AppConstants.IMAGE_ID to R.drawable.image
                )
            )
                .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                .build()

        workManager.enqueue(imageResizerWorkerRequest)
    }

    private fun startImageResizerForeground() {
        val imageResizerWorkerRequest =
            OneTimeWorkRequestBuilder<ImageResizerForegroundWorker>().setInputData(
                workDataOf(
                    AppConstants.IMAGE_ID to R.drawable.image
                )
            )
                .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                .build()

        workManager.enqueue(imageResizerWorkerRequest)
    }

    private fun startImageResizerObservable(
        coroutineScope: CoroutineScope,
        onProgressUpdated: (Float) -> Unit
    ) {
        workId = UUID.randomUUID()
        val imageResizeWorkRequest =
            OneTimeWorkRequestBuilder<ImageResizerObservableWorker>()
                .setInputData(
                    workDataOf(
                        AppConstants.IMAGE_ID to R.drawable.image
                    )
                )
                .setId(workId!!)
                .build()

        workManager.enqueue(
            imageResizeWorkRequest
        )

        coroutineScope.launch {
            workManager.getWorkInfoByIdFlow(workId!!).onEach { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.ENQUEUED -> {}
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo?.progress?.getFloat(
                            AppConstants.IMAGE_RESIZER_PROGRESS, 0f
                        )
                        onProgressUpdated(progress ?: 0f)
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        onProgressUpdated(100f)
                        val outputData = workInfo?.outputData?.getString(AppConstants.IMAGE_PATH)
                        Log.d("WORK_INFO", "Output Data: ${outputData}")
                    }
                    WorkInfo.State.FAILED -> {
                        onProgressUpdated(0f)
                    }
                    WorkInfo.State.BLOCKED -> {}
                    WorkInfo.State.CANCELLED -> {}
                    null -> {}
                }
            }.collect()
        }
    }
}