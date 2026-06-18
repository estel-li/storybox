package net.lijue.storybox.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.lijue.storybox.core.common.toFriendlyMessage
import net.lijue.storybox.core.model.StoryDto
import net.lijue.storybox.data.repository.ApiRepository
import net.lijue.storybox.playback.StoryPlayerManager

data class SearchUiState(
    val keyword: String = "",
    val isLoading: Boolean = false,
    val results: List<StoryDto> = emptyList(),
    val errorMessage: String = ""
)

class SearchViewModel(
    private val repository: ApiRepository,
    private val playerManager: StoryPlayerManager
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state

    fun updateKeyword(value: String) {
        _state.value = _state.value.copy(keyword = value, errorMessage = "")
    }

    fun search() {
        val keyword = _state.value.keyword.trim()
        if (keyword.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = "")
            runCatching { repository.search(keyword) }
                .onSuccess { _state.value = _state.value.copy(isLoading = false, results = it) }
                .onFailure { _state.value = _state.value.copy(isLoading = false, errorMessage = it.toFriendlyMessage()) }
        }
    }

    fun play(story: StoryDto, onStarted: () -> Unit) {
        viewModelScope.launch {
            playerManager.playQueue(listOf(story))
            onStarted()
        }
    }
}
