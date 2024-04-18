package com.abhishek.workmanager.helper

sealed class WorkerType(val ctaText: String) {
    data class Worker(val cta: String = "Start Worker") : WorkerType(cta)
    data class CoroutineWorker(val cta: String = "Start Coroutine Worker") : WorkerType(cta)
    data class ListenableFutureWorker(val cta: String = "Start Listenable Future Worker") : WorkerType(cta)
    data class RxWorker(val cta: String = "Start Rx Worker") : WorkerType(cta)
    data class ChainedWork(val cta: String = "Start Chained Work"): WorkerType(cta)
    data class RetryingWork(val cta: String = "Start Retrying Work") : WorkerType(cta)
    data class ConstrainedWork(val cta: String = "Start Constrained Work") : WorkerType(cta)
    data class PeriodicWork(val cta: String = "Start Periodic Work"): WorkerType(cta)
    data class CancelWork(val cta: String = "Cancel Work") : WorkerType(cta)
    data class ExpeditedWork(val cta: String = "Start Expedited Work"): WorkerType(cta)
    data class ForegroundWork(val cta: String = "Start Foreground Work"): WorkerType(cta)
    data class ObservableWork(val cta: String = "Start Observable Work"): WorkerType(cta)
}