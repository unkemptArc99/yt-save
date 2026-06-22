package com.ytsave.app.data.engine

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val python: Python
    private val module: PyObject

    init {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        python = Python.getInstance()
        module = python.getModule("download_manager")
    }

    class ProgressCallbackProxy(private val listener: (String) -> Unit) {
        fun call(progressJson: String) {
            listener(progressJson)
        }
    }

    suspend fun extractInfo(url: String): String = withContext(Dispatchers.IO) {
        module.callAttr("extract_info", url).toString()
    }

    suspend fun downloadVideo(
        url: String,
        outputDir: String,
        formatSpec: String,
        onProgress: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val proxy = ProgressCallbackProxy(onProgress)
        module.callAttr("download_video", url, outputDir, formatSpec, proxy).toString()
    }

    suspend fun downloadAudioOnly(
        url: String,
        outputDir: String,
        audioFormat: String,
        onProgress: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val proxy = ProgressCallbackProxy(onProgress)
        module.callAttr("download_audio_only", url, outputDir, audioFormat, proxy).toString()
    }
}
