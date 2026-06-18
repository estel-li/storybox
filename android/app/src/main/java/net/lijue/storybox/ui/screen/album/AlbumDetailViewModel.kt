package net.lijue.storybox.ui.screen.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.lijue.storybox.core.common.UiState
import net.lijue.storybox.core.common.toFriendlyMessage
import net.lijue.storybox.core.model.AlbumDto
import net.lijue.storybox.core.model.StoryDto
import net.lijue.storybox.data.repository.ApiRepository
import net.lijue.storybox.playback.StoryPlayerManager

data class AlbumDetailData(
    val album: AlbumDto,
    val stories: List<StoryDto>
) {
    val groupedStories: Map<String, List<StoryDto>> =
        stories.groupBy { it.chapter.ifBlank { "故事列表" } }
}

class AlbumDetailViewModel(
    private val albumId: Long,
    private val repository: ApiRepository,
    private val playerManager: StoryPlayerManager
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<AlbumDetailData>>(UiState.Loading)
    val state: StateFlow<UiState<AlbumDetailData>> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching {
                val album = repository.album(albumId)
                val stories = repository.storiesByAlbum(albumId)
                    .map { it.copy(album = album) }
                AlbumDetailData(album, stories)
            }.onSuccess {
                _state.value = UiState.Success(it)
            }.onFailure {
                _state.value = UiState.Error(it.toFriendlyMessage())
            }
        }
    }

    fun playAll(onStarted: () -> Unit) {
        val data = (_state.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            playerManager.playQueue(data.stories)
            onStarted()
        }
    }

    fun playFrom(story: StoryDto, onStarted: () -> Unit) {
        val data = (_state.value as? UiState.Success)?.data ?: return
        val index = data.stories.indexOfFirst { it.id == story.id }.coerceAtLeast(0)
        viewModelScope.launch {
            playerManager.playQueue(data.stories, startIndex = index)
            onStarted()
        }
    }
}
