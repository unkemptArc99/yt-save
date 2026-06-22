package com.ytsave.app.domain.usecase

import com.ytsave.app.data.engine.FileSystemManager
import com.ytsave.app.data.local.DownloadEntity
import com.ytsave.app.data.repository.DownloadRepository
import javax.inject.Inject

class DeleteVideoUseCase @Inject constructor(
    private val repository: DownloadRepository,
    private val fileManager: FileSystemManager
) {
    suspend operator fun invoke(download: DownloadEntity) {
        download.filePath?.let { path ->
            fileManager.deleteVideoFile(path)
        }
        repository.deleteDownload(download)
    }
}
