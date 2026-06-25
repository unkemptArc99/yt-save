package com.ytsave.app.data.engine

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ytsave.app.domain.model.GithubRelease
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val release: GithubRelease) : UpdateState()
    object NoUpdate : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@Singleton
class AppUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private val REPO_LATEST_RELEASE_URL = "https://api.github.com/repos/unkemptArc99/yt-save/releases/latest"
    private var downloadId: Long = -1

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                installApk(context, id)
                context.unregisterReceiver(this)
            }
        }
    }

    suspend fun checkForUpdates(currentVersion: String, manualCheck: Boolean = false) {
        _updateState.value = UpdateState.Checking
        try {
            val release = withContext(Dispatchers.IO) {
                val url = URL(REPO_LATEST_RELEASE_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                    json.decodeFromString<GithubRelease>(responseStr)
                } else {
                    throw Exception("Failed to fetch release: ${connection.responseCode}")
                }
            }

            val remoteVersion = release.tagName.removePrefix("v")
            val current = currentVersion.removePrefix("v")

            if (isNewerVersion(remoteVersion, current)) {
                _updateState.value = UpdateState.UpdateAvailable(release)
            } else {
                _updateState.value = UpdateState.NoUpdate
                if (manualCheck) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "You are on the latest version", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _updateState.value = UpdateState.Error(e.message ?: "Failed to check for updates")
            if (manualCheck) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error checking for updates", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun startUpdate(release: GithubRelease) {
        val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
        if (apkAsset == null) {
            Toast.makeText(context, "No APK found in release", Toast.LENGTH_SHORT).show()
            return
        }

        val request = DownloadManager.Request(Uri.parse(apkAsset.browserDownloadUrl))
            .setTitle("YTSave Update")
            .setDescription("Downloading version ${release.tagName}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, apkAsset.name)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        context.registerReceiver(
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_EXPORTED
        )
        
        downloadId = downloadManager.enqueue(request)
        Toast.makeText(context, "Downloading update...", Toast.LENGTH_SHORT).show()
        _updateState.value = UpdateState.Idle
    }

    private fun installApk(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor != null && cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (statusIndex >= 0 && cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                if (uriIndex >= 0) {
                    val uriString = cursor.getString(uriIndex)
                    if (uriString != null) {
                        val file = File(Uri.parse(uriString).path ?: return)
                        val apkUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )

                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(apkUri, "application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                }
            }
            cursor.close()
        }
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val localParts = local.split(".").map { it.toIntOrNull() ?: 0 }
        val length = maxOf(remoteParts.size, localParts.size)
        
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
    
    fun dismissUpdate() {
        _updateState.value = UpdateState.Idle
    }
}
