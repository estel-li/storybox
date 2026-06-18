package net.lijue.storybox.ui.screen.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.lijue.storybox.core.common.UiState
import net.lijue.storybox.core.common.toFriendlyMessage
import net.lijue.storybox.core.model.AlbumDto
import net.lijue.storybox.data.repository.ApiRepository

class AlbumListViewModel(
    private val categoryId: Long,
    private val repository: ApiRepository
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<AlbumDto>>>(UiState.Loading)
    val state: StateFlow<UiState<List<AlbumDto>>> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { repository.albumsByCategory(categoryId) }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(it.toFriendlyMessage()) }
        }
    }
}
