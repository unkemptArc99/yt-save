package com.ytsave.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DownloadProgress(
    val status: String = "unknown",
    @SerialName("downloaded_bytes") val downloadedBytes: Long = 0,
    @SerialName("total_bytes") val totalBytes: Long = 0,
    val speed: Double = 0.0,
    val eta: Int = 0,
    val filename: String = "",
    val progress: Int = 0
)
