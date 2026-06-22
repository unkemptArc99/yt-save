package com.ytsave.app.data.repository

import com.ytsave.app.data.local.DownloadDao
import com.ytsave.app.data.local.DownloadEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val downloadDao: DownloadDao
) {
    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    fun getActiveDownloads(): Flow<List<DownloadEntity>> = downloadDao.getActiveDownloads()

    fun getCompletedDownloads(): Flow<List<DownloadEntity>> = downloadDao.getCompletedDownloads()

    fun searchDownloads(query: String): Flow<List<DownloadEntity>> = downloadDao.searchDownloads(query)

    suspend fun getDownloadById(id: Long): DownloadEntity? = downloadDao.getById(id)

    suspend fun insertDownload(download: DownloadEntity): Long = downloadDao.insert(download)

    suspend fun updateDownload(download: DownloadEntity) = downloadDao.update(download)

    suspend fun deleteDownload(download: DownloadEntity) = downloadDao.delete(download)

    fun getTotalStorageUsed(): Flow<Long?> = downloadDao.getTotalStorageUsed()
}
