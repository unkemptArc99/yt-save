package com.ytsave.app.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytsave.app.data.local.DownloadEntity
import com.ytsave.app.domain.usecase.DeleteVideoUseCase
import com.ytsave.app.domain.usecase.SearchVideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val searchVideosUseCase: SearchVideosUseCase,
    private val deleteVideoUseCase: DeleteVideoUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    @OptIn(ExperimentalCoroutinesApi::class)
    val downloads: StateFlow<List<DownloadEntity>> = _searchQuery
        .flatMapLatest { query ->
            searchVideosUseCase(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun deleteDownload(download: DownloadEntity) {
        viewModelScope.launch {
            deleteVideoUseCase(download)
        }
    }
}
