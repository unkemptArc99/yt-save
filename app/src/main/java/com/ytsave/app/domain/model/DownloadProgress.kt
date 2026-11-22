package com.ytsave.app.domain.model

data class DownloadProgress(
    val isMerging: Boolean = false,
    val speed: Double = 0.0,
    val eta: Int = 0,
    val progress: Int = 0
)
