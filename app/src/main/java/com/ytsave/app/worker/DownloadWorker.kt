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
import com.ytsave.app.data.engine.FFmpegMerger
import com.ytsave.app.data.engine.FileSystemManager
import com.ytsave.app.data.engine.YtDlpManager
import com.ytsave.app.data.local.DownloadDao
import com.ytsave.app.data.local.DownloadStatus
import com.ytsave.app.domain.model.DownloadProgress
import com.ytsave.app.domain.model.DownloadResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val ytDlpManager: YtDlpManager,
    private val downloadDao: DownloadDao,
    private val ffmpegMerger: FFmpegMerger,
    private val fileManager: FileSystemManager
) : CoroutineWorker(appContext, workerParams) {

    private val json = Json { ignoreUnknownKeys = true }
    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1L)
        val formatSpec = inputData.getString(KEY_FORMAT) ?: "best"
        val downloadType = inputData.getString(KEY_TYPE) ?: "video+audio"

        if (downloadId == -1L) return@withContext Result.failure()

        try {
            setForeground(createForegroundInfo("Starting download...", 0))
            updateDownloadStatus(downloadId, DownloadStatus.DOWNLOADING)

            val tempDir = fileManager.getTempDownloadDir()
            
            val downloadResult: DownloadResult
            var finalFileToMove: File? = null

            if (downloadType == "video+audio" && formatSpec.contains("+")) {
                // Split download to avoid yt-dlp requiring ffmpeg
                val parts = formatSpec.substringBefore("/").split("+")
                val videoFormat = parts[0]
                val audioFormat = parts[1]

                val videoDir = File(tempDir, "v").apply { mkdirs() }
                val audioDir = File(tempDir, "a").apply { mkdirs() }

                val videoJson = ytDlpManager.downloadVideo(url, videoDir.absolutePath, videoFormat) { p -> handleProgress(downloadId, p, "Video") }
                val vResult = json.decodeFromString<DownloadResult>(videoJson)
                if (!vResult.success) throw Exception(vResult.error ?: "Video download failed")

                val audioJson = ytDlpManager.downloadVideo(url, audioDir.absolutePath, audioFormat) { p -> handleProgress(downloadId, p, "Audio") }
                val aResult = json.decodeFromString<DownloadResult>(audioJson)
                if (!aResult.success) throw Exception(aResult.error ?: "Audio download failed")

                updateDownloadStatus(downloadId, DownloadStatus.MERGING)
                setForeground(createForegroundInfo("Merging audio and video...", 100))
                
                val ext = if (vResult.filename.endsWith(".webm") || aResult.filename.endsWith(".webm")) "mkv" else "mp4"
                val outputPath = File(tempDir, "merged_${System.currentTimeMillis()}.$ext").absolutePath
                
                finalFileToMove = ffmpegMerger.mergeAudioVideo(vResult.filename, aResult.filename, outputPath)
                downloadResult = vResult // Use video metadata
            } else {
                val resultJson = ytDlpManager.downloadVideo(url, tempDir.absolutePath, formatSpec) { p -> handleProgress(downloadId, p, "Downloading") }

                downloadResult = json.decodeFromString<DownloadResult>(resultJson)
                if (!downloadResult.success) throw Exception(downloadResult.error ?: "Download failed")
                finalFileToMove = File(downloadResult.filename)
            }

            if (finalFileToMove == null || !finalFileToMove.exists()) {
                throw Exception("Downloaded file not found")
            }

            val publicFile = fileManager.moveToPublicStorage(finalFileToMove, downloadResult.title)

            val download = downloadDao.getById(downloadId)
            if (download != null) {
                downloadDao.update(
                    download.copy(
                        title = downloadResult.title.ifEmpty { download.title },
                        channel = downloadResult.channel.ifEmpty { download.channel },
                        thumbnailUrl = downloadResult.thumbnail.ifEmpty { download.thumbnailUrl },
                        duration = if (downloadResult.duration > 0) downloadResult.duration else download.duration,
                        filePath = publicFile.absolutePath,
                        fileSize = publicFile.length(),
                        status = DownloadStatus.COMPLETED,
                        progress = 100
                    )
                )
            }

            showCompletionNotification(downloadResult.title)
            Result.success()
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

    private fun handleProgress(downloadId: Long, progressJson: String, prefix: String) {
        try {
            val progress = json.decodeFromString<DownloadProgress>(progressJson)
            runBlocking {
                val download = downloadDao.getById(downloadId)
                if (download != null) {
                    downloadDao.update(
                        download.copy(
                            progress = progress.progress,
                            downloadSpeed = formatSpeed(progress.speed),
                            eta = formatEta(progress.eta)
                        )
                    )
                }
                setForeground(createForegroundInfo(
                    "$prefix: ${progress.progress}%",
                    progress.progress
                ))
            }
        } catch (e: Exception) {
            // Ignore parse errors in progress updates
        }
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
