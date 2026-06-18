package net.lijue.storybox.ui.screen.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.lijue.storybox.core.common.UiState
import net.lijue.storybox.core.common.toFriendlyMessage
import net.lijue.storybox.core.model.StoryDto
import net.lijue.storybox.data.repository.ApiRepository
import net.lijue.storybox.playback.StoryPlayerManager

class FavoritesViewModel(
    private val repository: ApiRepository,
    private val playerManager: StoryPlayerManager
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<StoryDto>>>(UiState.Loading)
    val state: StateFlow<UiState<List<StoryDto>>> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { repository.favoriteStories() }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(it.toFriendlyMessage()) }
        }
    }

    fun play(story: StoryDto, onStarted: () -> Unit) {
        viewModelScope.launch {
            playerManager.playQueue(listOf(story))
            onStarted()
        }
    }

    fun remove(story: StoryDto) {
        viewModelScope.launch {
            runCatching { repository.removeFavorite(story.id) }
            load()
        }
    }
}
