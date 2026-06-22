package com.ytsave.app.domain.usecase

import com.ytsave.app.data.engine.YtDlpManager
import com.ytsave.app.domain.model.VideoInfo
import kotlinx.serialization.json.Json
import javax.inject.Inject

class FetchVideoInfoUseCase @Inject constructor(
    private val ytDlpManager: YtDlpManager
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(url: String): Result<VideoInfo> {
        return try {
            val jsonString = ytDlpManager.extractInfo(url)
            val info = json.decodeFromString<VideoInfo>(jsonString)
            if (info.error != null && info.error.isNotBlank()) {
                Result.failure(Exception(info.error))
            } else {
                Result.success(info)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
