package com.ytsave.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DownloadResult(
    val success: Boolean = false,
    val filename: String = "",
    val title: String = "",
    val channel: String = "",
    val thumbnail: String = "",
    val duration: Long = 0,
    val filesize: Long = 0,
    val error: String? = null
)
