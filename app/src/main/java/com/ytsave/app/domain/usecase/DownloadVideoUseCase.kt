package com.ytsave.app.domain.usecase

import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ytsave.app.data.local.DownloadEntity
import com.ytsave.app.data.local.DownloadStatus
import com.ytsave.app.data.repository.DownloadRepository
import com.ytsave.app.worker.DownloadWorker
import javax.inject.Inject

class DownloadVideoUseCase @Inject constructor(
    private val repository: DownloadRepository,
    private val workManager: WorkManager
) {
    suspend operator fun invoke(url: String, formatSpec: String, downloadType: String, title: String, channel: String): Long {
        val entity = DownloadEntity(
            url = url,
            title = title,
            channel = channel,
            status = DownloadStatus.QUEUED,
            format = downloadType,
            quality = formatSpec
        )
        
        val downloadId = repository.insertDownload(entity)
        
        val inputData = Data.Builder()
            .putString(DownloadWorker.KEY_URL, url)
            .putLong(DownloadWorker.KEY_DOWNLOAD_ID, downloadId)
            .putString(DownloadWorker.KEY_FORMAT, formatSpec)
            .putString(DownloadWorker.KEY_TYPE, downloadType)
            .build()
            
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
            
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(downloadId.toString())
            .build()
            
        workManager.enqueue(request)
        
        return downloadId
    }
}
