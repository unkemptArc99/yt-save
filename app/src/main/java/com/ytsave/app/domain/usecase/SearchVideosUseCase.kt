package com.ytsave.app.domain.usecase

import com.ytsave.app.data.local.DownloadEntity
import com.ytsave.app.data.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchVideosUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    operator fun invoke(query: String): Flow<List<DownloadEntity>> {
        return if (query.isBlank()) {
            repository.getCompletedDownloads()
        } else {
            repository.searchDownloads(query)
        }
    }
}
