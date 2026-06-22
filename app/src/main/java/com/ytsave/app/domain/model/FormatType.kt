package com.ytsave.app.domain.model

enum class FormatType(val id: String, val label: String) {
    VIDEO_AUDIO("video+audio", "Stitched (Video+Audio)"),
    VIDEO_ONLY("video_only", "Video Only"),
    AUDIO_ONLY("audio_only", "Audio Only");

    companion object {
        fun fromId(id: String): FormatType = values().find { it.id == id } ?: VIDEO_AUDIO
    }
}
