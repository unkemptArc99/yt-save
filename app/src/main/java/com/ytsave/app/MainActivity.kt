package com.ytsave.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ytsave.app.data.engine.AppUpdater
import com.ytsave.app.data.engine.UpdateState
import com.ytsave.app.data.local.PreferencesManager
import com.ytsave.app.ui.components.UpdateDialog
import com.ytsave.app.ui.navigation.YTSaveNavGraph
import com.ytsave.app.ui.theme.YTSaveTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    @Inject
    lateinit var appUpdater: AppUpdater

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedUrl = when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                } else null
            }
            else -> null
        }

        lifecycleScope.launch {
            val versionName = try {
                packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
            } catch (e: Exception) { "1.0.0" }
            appUpdater.checkForUpdates(versionName, manualCheck = false)
        }

        setContent {
            val themeMode by preferencesManager.themeMode.collectAsStateWithLifecycle(initialValue = "system")
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            YTSaveTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    YTSaveNavGraph(sharedUrl = sharedUrl)
                    
                    val updateState by appUpdater.updateState.collectAsStateWithLifecycle()
                    if (updateState is UpdateState.UpdateAvailable) {
                        val release = (updateState as UpdateState.UpdateAvailable).release
                        UpdateDialog(
                            release = release,
                            onConfirm = { appUpdater.startUpdate(release) },
                            onDismiss = { appUpdater.dismissUpdate() }
                        )
                    }
                }
            }
        }
    }
}
