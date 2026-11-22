package com.ytsave.app.domain.model

data class DownloadResult(
    val filename: String,
    val title: String,
    val channel: String,
    val thumbnail: String,
    val duration: Long,
    val filesize: Long
)
