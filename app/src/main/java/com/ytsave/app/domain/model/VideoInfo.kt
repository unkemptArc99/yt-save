package com.ytsave.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoInfo(
    val title: String = "",
    val channel: String = "",
    val thumbnail: String = "",
    val duration: Long = 0,
    val formats: List<VideoFormat> = emptyList(),
    val error: String? = null
)

@Serializable
data class VideoFormat(
    @SerialName("format_id") val formatId: String = "",
    val ext: String = "",
    val resolution: String = "unknown",
    val filesize: Long? = null,
    val vcodec: String = "none",
    val acodec: String = "none",
    val height: Int = 0,
    val abr: Double = 0.0
)
