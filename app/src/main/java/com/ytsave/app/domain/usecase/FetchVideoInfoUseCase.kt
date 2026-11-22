package com.ytsave.app.domain.usecase

import com.ytsave.app.data.engine.YtDlpManager
import com.ytsave.app.domain.model.VideoInfo
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class FetchVideoInfoUseCase @Inject constructor(
    private val ytDlpManager: YtDlpManager
) {
    suspend operator fun invoke(url: String): Result<VideoInfo> {
        return try {
            Result.success(ytDlpManager.extractInfo(url))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
