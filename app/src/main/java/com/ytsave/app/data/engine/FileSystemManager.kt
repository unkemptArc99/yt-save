package com.ytsave.app.data.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileSystemManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getTempDownloadDir(): File {
        val dir = File(context.cacheDir, "downloads")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getStorageDir(): File {
        // Fallback for older devices if needed, default to Movies
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "YTSave")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getFinalOutputPath(title: String, ext: String): String {
        return File(getStorageDir(), "$title.$ext").absolutePath
    }

    fun moveToPublicStorage(sourceFile: File, title: String): File {
        val ext = sourceFile.extension.ifEmpty { "mp4" }
        val isAudio = ext in listOf("m4a", "mp3", "opus")
        
        val safeTitle = title.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$safeTitle.$ext")
            put(MediaStore.MediaColumns.MIME_TYPE, if (isAudio) "audio/$ext" else "video/$ext")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val dir = if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$dir/YTSave")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (isAudio) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            if (isAudio) MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collection, contentValues)
            ?: throw RuntimeException("Failed to create MediaStore entry")

        resolver.openOutputStream(uri)?.use { outStream ->
            FileInputStream(sourceFile).use { inStream ->
                inStream.copyTo(outStream)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        sourceFile.delete()
        
        // On older Android, we might need the actual path
        val dirName = if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
        val fallbackDir = File(Environment.getExternalStoragePublicDirectory(dirName), "YTSave")
        return File(fallbackDir, "$safeTitle.$ext")
    }

    fun deleteVideoFile(filePath: String): Boolean {
        val file = File(filePath)
        if (file.exists() && file.delete()) {
            val resolver = context.contentResolver
            val uri = MediaStore.Files.getContentUri("external")
            val selection = "${MediaStore.MediaColumns.DATA}=?"
            val selectionArgs = arrayOf(filePath)
            resolver.delete(uri, selection, selectionArgs)
            return true
        }
        return false
    }

    fun playVideo(filePath: String, context: Context) {
        val file = File(filePath)
        if (!file.exists()) return

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val isAudio = file.extension in listOf("m4a", "mp3", "opus")
        val mimeType = if (isAudio) "audio/*" else "video/*"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Open with")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun openInFileExplorer(filePath: String, context: Context) {
        val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Movies%2FYTSave")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "vnd.android.document/directory")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback if directory not found
        }
    }
}
