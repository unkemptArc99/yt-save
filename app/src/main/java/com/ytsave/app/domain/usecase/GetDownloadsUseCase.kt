package com.ytsave.app.domain.usecase

import com.ytsave.app.data.local.DownloadEntity
import com.ytsave.app.data.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDownloadsUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    fun getCompleted(): Flow<List<DownloadEntity>> = repository.getCompletedDownloads()
    
    fun getActive(): Flow<List<DownloadEntity>> = repository.getActiveDownloads()
}
