package com.ytsave.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.ytsave.app.data.local.DownloadEntity
import com.ytsave.app.domain.model.VideoInfo
import com.ytsave.app.domain.usecase.DownloadVideoUseCase
import com.ytsave.app.domain.usecase.FetchVideoInfoUseCase
import com.ytsave.app.domain.usecase.GetDownloadsUseCase
import com.ytsave.app.data.repository.DownloadRepository
import com.ytsave.app.data.local.PreferencesManager
import com.ytsave.app.domain.model.FormatType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FetchState {
    object Idle : FetchState()
    object Loading : FetchState()
    data class Success(val videoInfo: VideoInfo) : FetchState()
    data class Error(val message: String) : FetchState()
}

data class HomeUiState(
    val url: String = "",
    val fetchState: FetchState = FetchState.Idle,
    val activeDownloads: List<DownloadEntity> = emptyList(),
    val defaultFormat: FormatType = FormatType.VIDEO_AUDIO
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fetchVideoInfoUseCase: FetchVideoInfoUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val getDownloadsUseCase: GetDownloadsUseCase,
    private val downloadRepository: DownloadRepository,
    private val preferencesManager: PreferencesManager,
    private val workManager: WorkManager
) : ViewModel() {

    private val _urlState = MutableStateFlow("")
    private val _fetchState = MutableStateFlow<FetchState>(FetchState.Idle)

    val uiState: StateFlow<HomeUiState> = combine(
        _urlState,
        _fetchState,
        getDownloadsUseCase.getActive(),
        preferencesManager.defaultFormat
    ) { url, fetchState, activeDownloads, defaultFormatStr ->
        HomeUiState(
            url = url,
            fetchState = fetchState,
            activeDownloads = activeDownloads,
            defaultFormat = FormatType.fromId(defaultFormatStr)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun updateUrl(url: String) {
        _urlState.value = url
        if (_fetchState.value is FetchState.Error) {
            _fetchState.value = FetchState.Idle
        }
    }

    private val _downloadEvents = MutableSharedFlow<String>()
    val downloadEvents = _downloadEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            var previousCompleted = emptySet<Long>()
            getDownloadsUseCase.getCompleted()
                .drop(1) // Ignore initial load
                .collect { completedList ->
                    val currentIds = completedList.map { it.id }.toSet()
                    val newIds = currentIds - previousCompleted
                    if (newIds.isNotEmpty()) {
                        val newDownloads = completedList.filter { it.id in newIds }
                        newDownloads.forEach { download ->
                            _downloadEvents.emit(download.title)
                        }
                    }
                    previousCompleted = currentIds
                }
        }
    }

    fun fetchVideoInfo(url: String) {
        if (url.isBlank()) return
        
        viewModelScope.launch {
            _fetchState.value = FetchState.Loading
            val result = fetchVideoInfoUseCase(url)
            
            result.fold(
                onSuccess = { videoInfo ->
                    _fetchState.value = FetchState.Success(videoInfo)
                },
                onFailure = { error ->
                    _fetchState.value = FetchState.Error(error.message ?: "Failed to fetch info")
                }
            )
        }
    }

    fun startDownload(formatSpec: String, downloadType: String) {
        val currentUrl = _urlState.value
        val state = _fetchState.value
        if (currentUrl.isBlank() || state !is FetchState.Success) return
        
        viewModelScope.launch {
            downloadVideoUseCase(currentUrl, formatSpec, downloadType, state.videoInfo.title, state.videoInfo.channel)
            // Reset state after starting
            _urlState.value = ""
            _fetchState.value = FetchState.Idle
        }
    }

    fun cancelDownload(downloadId: Long) {
        workManager.cancelAllWorkByTag(downloadId.toString())
        viewModelScope.launch {
            val download = downloadRepository.getDownloadById(downloadId)
            if (download != null) {
                downloadRepository.updateDownload(download.copy(status = com.ytsave.app.data.local.DownloadStatus.CANCELLED))
            }
        }
    }
}
