package com.ytsave.app.data.engine

import android.content.Context
import android.util.AtomicFile
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.ytsave.app.BuildConfig
import com.ytsave.app.domain.model.DownloadProgress
import com.ytsave.app.domain.model.DownloadResult
import com.ytsave.app.domain.model.VideoFormat
import com.ytsave.app.domain.model.VideoInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val engine by lazy {
        installBundledYtDlp()
        YoutubeDL.init(context)
        FFmpeg.init(context)
        YoutubeDL
    }

    suspend fun extractInfo(url: String): VideoInfo = runInterruptible(Dispatchers.IO) {
        val request = requestFor(url).addOption("--dump-single-json")
        val info = Json.parseToJsonElement(engine.execute(request, redirectErrorStream = false).out) as JsonObject
        val formats = (info["formats"] as? JsonArray).orEmpty().mapNotNull { entry ->
            val format = entry as? JsonObject ?: return@mapNotNull null
            val id = format.text("format_id") ?: return@mapNotNull null
            val height = format.scalar("height")?.intOrNull ?: 0
            val abr = format.scalar("abr")?.doubleOrNull ?: 0.0
            val videoCodec = format.text("vcodec") ?: "none"
            val audioCodec = format.text("acodec") ?: "none"
            val resolution = when {
                height > 0 -> "${height}p"
                videoCodec == "none" && audioCodec != "none" ->
                    if (abr > 0) "audio ${abr.toInt()}kbps" else "audio"
                else -> format.text("resolution") ?: "unknown"
            }
            VideoFormat(
                formatId = id,
                ext = format.text("ext").orEmpty(),
                resolution = resolution,
                filesize = format.scalar("filesize")?.longOrNull
                    ?: format.scalar("filesize_approx")?.longOrNull,
                vcodec = videoCodec,
                acodec = audioCodec,
                height = height,
                abr = abr
            )
        }
        VideoInfo(
            title = info.text("title") ?: "Unknown",
            channel = info.text("channel") ?: info.text("uploader") ?: "Unknown",
            thumbnail = info.text("thumbnail").orEmpty(),
            duration = info.scalar("duration")?.doubleOrNull?.toLong() ?: 0,
            formats = formats
        )
    }

    suspend fun downloadVideo(
        url: String,
        outputDir: File,
        formatSpec: String,
        onProgress: (DownloadProgress) -> Unit
    ): DownloadResult = runInterruptible(Dispatchers.IO) {
        val request = requestFor(url)
            .addOption("--format", formatSpec)
            .addOption("--paths", outputDir.absolutePath)
            .addOption("--output", "media.%(ext)s")
            .addOption("--no-simulate")
            .addOption("--no-quiet")
            .addOption("--progress")
            .addOption("--newline")
            .addOption("--progress-delta", "0.5")
            .addOption("--progress-template", "download:progress:%(progress._percent_str)s\t%(progress.speed)s\t%(progress.eta)s")
            .addOption("--print", "after_move:result:%(.{filepath,title,channel,uploader,thumbnail,duration})j")
        val response = engine.execute(request, redirectErrorStream = false, callback = { _, _, line ->
            when {
                line.startsWith("progress:") -> {
                    val fields = line.removePrefix("progress:").split('\t')
                    val percent = fields[0].trim().removeSuffix("%").toDoubleOrNull()
                    if (percent != null) {
                        onProgress(DownloadProgress(
                            progress = percent.toInt().coerceIn(0, 100),
                            speed = fields.getOrNull(1)?.toDoubleOrNull() ?: 0.0,
                            eta = fields.getOrNull(2)?.toIntOrNull() ?: 0
                        ))
                    }
                }
                line.startsWith("[Merger]") -> onProgress(DownloadProgress(
                    isMerging = true,
                    progress = 100
                ))
            }
        })
        val resultLine = response.out.lineSequence().lastOrNull { it.startsWith("result:") }
            ?: throw IOException("yt-dlp did not report a completed download")
        val info = Json.parseToJsonElement(resultLine.removePrefix("result:")) as JsonObject
        val file = info.text("filepath")?.let(::File)
            ?: throw IOException("yt-dlp did not report the output file")
        check(file.isFile && file.length() > 0) { "Downloaded file is missing or empty" }
        DownloadResult(
            filename = file.absolutePath,
            title = info.text("title") ?: "Unknown",
            channel = info.text("channel") ?: info.text("uploader") ?: "Unknown",
            thumbnail = info.text("thumbnail").orEmpty(),
            duration = info.scalar("duration")?.doubleOrNull?.toLong() ?: 0,
            filesize = file.length()
        )
    }

    private fun requestFor(url: String): YoutubeDLRequest = YoutubeDLRequest(url)
        .addOption("--ignore-config")
        .addOption("--no-playlist")
        .addCommands(listOf("--"))

    private fun installBundledYtDlp() {
        // The Android library installs an older zipapp if this file is missing.
        val directory = File(context.noBackupFilesDir, "${YoutubeDL.baseName}/${YoutubeDL.ytdlpDirName}")
        val zipapp = AtomicFile(File(directory, YoutubeDL.ytdlpBin))
        val version = File(directory, "bundled-version")
        if (zipapp.baseFile.isFile && version.isFile && version.readText() == BuildConfig.YT_DLP_VERSION) return

        val output = zipapp.startWrite()
        try {
            context.assets.open("yt-dlp").use { it.copyTo(output) }
            zipapp.finishWrite(output)
        } catch (error: Exception) {
            zipapp.failWrite(output)
            throw error
        }
        version.writeText(BuildConfig.YT_DLP_VERSION)
    }

    private fun JsonObject.scalar(key: String): JsonPrimitive? = this[key] as? JsonPrimitive

    private fun JsonObject.text(key: String): String? = scalar(key)?.contentOrNull
}
