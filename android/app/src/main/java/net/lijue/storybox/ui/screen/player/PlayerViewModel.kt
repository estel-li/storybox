package net.lijue.storybox.ui.screen.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.lijue.storybox.core.datastore.SettingsDataStore
import net.lijue.storybox.core.model.SleepTimerMode
import net.lijue.storybox.data.repository.ApiRepository
import net.lijue.storybox.playback.StoryPlaybackState
import net.lijue.storybox.playback.StoryPlayerManager

data class PlayerUiState(
    val favoriteIds: Set<Long> = emptySet(),
    val progressMessage: String = ""
)

class PlayerViewModel(
    private val repository: ApiRepository,
    private val settings: SettingsDataStore,
    private val playerManager: StoryPlayerManager
) : ViewModel() {
    val playbackState: StateFlow<StoryPlaybackState> = playerManager.state

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    private var sleepDeadlineMillis = 0L
    private var tickerJob: Job? = null

    init {
        loadFavorites()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                playerManager.publishState()
                handleSleepTimerTick()
            }
        }
    }

    fun playPause() {
        playerManager.playPause()
    }

    fun previous() {
        playerManager.skipPrevious()
    }

    fun next() {
        playerManager.skipNext()
    }

    fun seekTo(positionMillis: Long) {
        playerManager.seekTo(positionMillis)
    }

    fun playQueueItem(index: Int) {
        playerManager.playQueueItem(index)
    }

    fun toggleFavorite() {
        val storyId = playbackState.value.currentStory?.id ?: return
        viewModelScope.launch {
            val next = _uiState.value.favoriteIds.toMutableSet()
            runCatching {
                if (storyId in next) {
                    repository.removeFavorite(storyId)
                    next.remove(storyId)
                } else {
                    repository.addFavorite(storyId)
                    next.add(storyId)
                }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(favoriteIds = next)
            }.onFailure {
                _uiState.value = _uiState.value.copy(progressMessage = "收藏失败，请稍后再试。")
            }
        }
    }

    fun setSleepTimer(mode: SleepTimerMode) {
        viewModelScope.launch {
            settings.saveSleepTimerMode(mode)
            sleepDeadlineMillis = if (mode.durationMillis > 0) {
                System.currentTimeMillis() + mode.durationMillis
            } else {
                0L
            }
            playerManager.setSleepTimerMode(mode, mode.durationMillis)
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(progressMessage = "")
        playerManager.clearPlaybackError()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            runCatching { repository.favoriteIds() }
                .onSuccess { _uiState.value = _uiState.value.copy(favoriteIds = it) }
        }
    }

    private fun handleSleepTimerTick() {
        val mode = playbackState.value.sleepTimerMode
        if (mode.durationMillis <= 0) return
        val remaining = sleepDeadlineMillis - System.currentTimeMillis()
        playerManager.setSleepRemaining(remaining)
        if (remaining <= 0L) {
            playerManager.pause()
            setSleepTimer(SleepTimerMode.Off)
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        super.onCleared()
    }
}
