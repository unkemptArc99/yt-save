package com.ytsave.app.worker

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.ytsave.app.R
import com.ytsave.app.YTSaveApp
import com.ytsave.app.data.engine.FileSystemManager
import com.ytsave.app.data.engine.YtDlpManager
import com.ytsave.app.data.local.DownloadDao
import com.ytsave.app.data.local.DownloadStatus
import com.ytsave.app.domain.model.DownloadProgress
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val ytDlpManager: YtDlpManager,
    private val downloadDao: DownloadDao,
    private val fileManager: FileSystemManager
) : CoroutineWorker(appContext, workerParams) {

    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1L)
        val formatSpec = inputData.getString(KEY_FORMAT) ?: "best"

        if (downloadId == -1L) return@withContext Result.failure()

        try {
            setForeground(createForegroundInfo("Starting download...", 0))
            updateDownloadStatus(downloadId, DownloadStatus.DOWNLOADING)

            val tempDir = fileManager.getTempDownloadDir(downloadId)
            val downloadResult = coroutineScope {
                val progress = Channel<DownloadProgress>(Channel.CONFLATED)
                val reporter = launch {
                    for (update in progress) handleProgress(downloadId, update)
                }
                try {
                    ytDlpManager.downloadVideo(url, tempDir, formatSpec) { progress.trySend(it) }
                } finally {
                    progress.close()
                    reporter.join()
                }
            }
            ensureActive()
            val publicFile = fileManager.moveToPublicStorage(
                File(downloadResult.filename),
                downloadResult.title,
                inputData.getString(KEY_TYPE) == "audio_only"
            )
            // Only this worker's files are removed. Partial files are kept between retries.
            tempDir.deleteRecursively()

            val download = downloadDao.getById(downloadId)
            if (download != null) {
                downloadDao.update(
                    download.copy(
                        title = downloadResult.title.ifEmpty { download.title },
                        channel = downloadResult.channel.ifEmpty { download.channel },
                        thumbnailUrl = downloadResult.thumbnail.ifEmpty { download.thumbnailUrl },
                        duration = if (downloadResult.duration > 0) downloadResult.duration else download.duration,
                        filePath = publicFile.absolutePath,
                        fileSize = downloadResult.filesize,
                        status = DownloadStatus.COMPLETED,
                        progress = 100
                    )
                )
            }

            showCompletionNotification(downloadResult.title)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = e.message?.lowercase() ?: ""
            if (errorMessage.contains("network") || errorMessage.contains("timeout") || errorMessage.contains("resolve host") || errorMessage.contains("unreachable")) {
                updateDownloadError(downloadId, "Network error. Retrying...")
                return@withContext Result.retry()
            }
            updateDownloadError(downloadId, e.message ?: "Unknown error")
            Result.failure()
        }
    }

    private suspend fun handleProgress(downloadId: Long, progress: DownloadProgress) {
        val download = downloadDao.getById(downloadId) ?: return
        if (download.status != DownloadStatus.DOWNLOADING && download.status != DownloadStatus.MERGING) return

        val merging = progress.isMerging
        downloadDao.update(
            download.copy(
                status = if (merging) DownloadStatus.MERGING else DownloadStatus.DOWNLOADING,
                progress = progress.progress,
                downloadSpeed = formatSpeed(progress.speed),
                eta = formatEta(progress.eta)
            )
        )
        val message = if (merging) "Merging audio and video..." else "Downloading: ${progress.progress}%"
        setForeground(createForegroundInfo(message, progress.progress))
    }

    private suspend fun updateDownloadStatus(id: Long, status: DownloadStatus) {
        val download = downloadDao.getById(id)
        if (download != null) {
            downloadDao.update(download.copy(status = status))
        }
    }

    private suspend fun updateDownloadError(id: Long, message: String) {
        val download = downloadDao.getById(id)
        if (download != null) {
            downloadDao.update(download.copy(
                status = DownloadStatus.FAILED,
                errorMessage = message
            ))
        }
    }

    private fun createForegroundInfo(text: String, progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, YTSaveApp.DOWNLOAD_CHANNEL_ID)
            .setContentTitle("YTSave")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showCompletionNotification(title: String) {
        val notification = NotificationCompat.Builder(applicationContext, YTSaveApp.DOWNLOAD_CHANNEL_ID)
            .setContentTitle("Download Complete")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(title.hashCode(), notification)
    }

    private fun formatSpeed(bytesPerSecond: Double): String {
        return when {
            bytesPerSecond >= 1_000_000 -> "%.1f MB/s".format(bytesPerSecond / 1_000_000)
            bytesPerSecond >= 1_000 -> "%.0f KB/s".format(bytesPerSecond / 1_000)
            else -> "%.0f B/s".format(bytesPerSecond)
        }
    }

    private fun formatEta(seconds: Int): String {
        if (seconds <= 0) return "--:--"
        val mins = seconds / 60
        val secs = seconds % 60
        return if (mins >= 60) {
            "%d:%02d:%02d".format(mins / 60, mins % 60, secs)
        } else {
            "%d:%02d".format(mins, secs)
        }
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_FORMAT = "format"
        const val KEY_TYPE = "type"
        const val NOTIFICATION_ID = 1001
    }
}
