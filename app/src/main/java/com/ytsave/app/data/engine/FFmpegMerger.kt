package com.ytsave.app.data.engine

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FFmpegMerger @Inject constructor() {

    fun mergeAudioVideo(videoPath: String, audioPath: String, outputPath: String): File {
        val command = "-i \"$videoPath\" -i \"$audioPath\" -c copy \"$outputPath\""
        
        val session = FFmpegKit.execute(command)
        
        if (ReturnCode.isSuccess(session.returnCode)) {
            // Clean up temporary files
            File(videoPath).delete()
            File(audioPath).delete()
            return File(outputPath)
        } else {
            throw RuntimeException("FFmpeg merge failed with state ${session.state} and return code ${session.returnCode}. ${session.failStackTrace}")
        }
    }
}
