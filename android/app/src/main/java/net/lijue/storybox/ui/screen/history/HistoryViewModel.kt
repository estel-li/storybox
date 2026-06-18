package net.lijue.storybox.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.lijue.storybox.core.common.UiState
import net.lijue.storybox.core.common.toFriendlyMessage
import net.lijue.storybox.core.model.PlayHistoryDto
import net.lijue.storybox.data.repository.ApiRepository
import net.lijue.storybox.data.repository.PlaybackStart
import net.lijue.storybox.playback.StoryPlayerManager

class HistoryViewModel(
    private val repository: ApiRepository,
    private val playerManager: StoryPlayerManager
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<PlayHistoryDto>>>(UiState.Loading)
    val state: StateFlow<UiState<List<PlayHistoryDto>>> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { repository.history() }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(it.toFriendlyMessage()) }
        }
    }

    fun continuePlay(item: PlayHistoryDto, onStarted: () -> Unit) {
        val story = item.story ?: return
        viewModelScope.launch {
            val start = runCatching { repository.playbackQueueFor(story) }
                .getOrDefault(PlaybackStart(listOf(story), 0))
            playerManager.playQueue(
                stories = start.stories,
                startIndex = start.startIndex,
                startPositionSeconds = item.positionSeconds
            )
            onStarted()
        }
    }
}
