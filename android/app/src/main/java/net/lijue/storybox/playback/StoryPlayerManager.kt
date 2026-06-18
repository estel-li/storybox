package net.lijue.storybox.playback

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.lijue.storybox.core.common.toFriendlyMessage
import net.lijue.storybox.core.datastore.SettingsDataStore
import net.lijue.storybox.core.model.SleepTimerMode
import net.lijue.storybox.core.model.StoryDto
import net.lijue.storybox.data.repository.ApiRepository

@androidx.annotation.OptIn(UnstableApi::class)
class StoryPlayerManager(
    private val context: Context,
    private val settings: SettingsDataStore,
    private val repository: ApiRepository
) {
    private var queue = PlaybackQueue()
    private var stopAfterCurrent = false
    private var lastHistoryUploadAt = 0L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    val mediaSession: MediaSession = MediaSession.Builder(context, player).build()

    private val _state = MutableStateFlow(StoryPlaybackState())
    val state: StateFlow<StoryPlaybackState> = _state.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying && stopAfterCurrent && hasReachedCurrentStoryEnd()) {
                    stopAfterCurrent = false
                    player.setPauseAtEndOfMediaItems(false)
                    _state.value = snapshot().copy(
                        sleepTimerMode = SleepTimerMode.Off,
                        sleepRemainingMillis = 0L
                    )
                } else {
                    publishState()
                }
                if (!isPlaying) uploadProgress(force = true)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                publishState()
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.value = snapshot().copy(
                    errorMessage = error.toFriendlyMessage()
                )
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                uploadProgress(force = true)
                val index = player.currentMediaItemIndex.coerceAtLeast(0)
                queue = queue.withIndex(index)
                publishState()
            }
        })
        scope.launch {
            while (isActive) {
                delay(1000)
                publishState()
                uploadProgress(force = false)
            }
        }
    }

    suspend fun playQueue(
        stories: List<StoryDto>,
        startIndex: Int = 0,
        startPositionSeconds: Int = 0
    ) {
        if (stories.isEmpty()) return
        queue = PlaybackQueue(stories = stories, currentIndex = startIndex)
        // Media3 只接收后端流地址，不触碰 NAS 真实文件路径。
        val items = stories.map { story ->
            MediaItem.Builder()
                .setMediaId(story.id.toString())
                .setUri(repository.streamUrl(story.id))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(story.safeTitle)
                        .setAlbumTitle(story.album?.displayName.orEmpty())
                        .setArtist("家庭故事盒")
                        .build()
                )
                .build()
        }
        player.setMediaItems(
            items,
            startIndex.coerceIn(0, items.lastIndex),
            startPositionSeconds.coerceAtLeast(0) * 1000L
        )
        player.prepare()
        startPlaybackService()
        player.play()
        publishState()
    }

    fun playPause() {
        if (player.isPlaying) player.pause() else {
            startPlaybackService()
            player.play()
        }
        publishState()
    }

    fun pause() {
        player.pause()
        publishState()
    }

    fun seekTo(positionMillis: Long) {
        player.seekTo(positionMillis.coerceAtLeast(0L))
        publishState()
    }

    fun skipNext() {
        uploadProgress(force = true)
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.play()
        }
        publishState()
    }

    fun skipPrevious() {
        uploadProgress(force = true)
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            player.play()
        } else {
            player.seekTo(0)
        }
        publishState()
    }

    fun setSleepTimerMode(mode: SleepTimerMode, remainingMillis: Long = 0L) {
        stopAfterCurrent = mode == SleepTimerMode.EndOfStory
        player.setPauseAtEndOfMediaItems(stopAfterCurrent)
        _state.value = snapshot().copy(
            sleepTimerMode = mode,
            sleepRemainingMillis = remainingMillis
        )
    }

    fun setSleepRemaining(remainingMillis: Long) {
        _state.value = _state.value.copy(sleepRemainingMillis = remainingMillis.coerceAtLeast(0L))
    }

    fun clearPlaybackError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun snapshot(): StoryPlaybackState {
        val duration = player.duration.takeUnless { it == C.TIME_UNSET } ?: 0L
        return StoryPlaybackState(
            queue = queue.withIndex(player.currentMediaItemIndex.coerceAtLeast(0)),
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            positionMillis = player.currentPosition.coerceAtLeast(0L),
            durationMillis = duration.coerceAtLeast(0L),
            bufferedPercentage = player.bufferedPercentage,
            errorMessage = _state.value.errorMessage,
            sleepTimerMode = _state.value.sleepTimerMode,
            sleepRemainingMillis = _state.value.sleepRemainingMillis
        )
    }

    fun publishState() {
        _state.value = snapshot()
    }

    fun release() {
        uploadProgress(force = true)
        scope.cancel()
        mediaSession.release()
        player.release()
    }

    private fun startPlaybackService() {
        // 启动 MediaSessionService 后，系统通知栏和锁屏会接管播放控制。
        val intent = Intent(context, StoryPlaybackService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    private fun hasReachedCurrentStoryEnd(): Boolean {
        val duration = player.duration.takeUnless { it == C.TIME_UNSET } ?: return false
        return duration > 0L && player.currentPosition >= duration - 1_000L
    }

    private fun uploadProgress(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastHistoryUploadAt < 15_000L) return
        val state = snapshot()
        val story = state.currentStory ?: return
        lastHistoryUploadAt = now
        scope.launch {
            val positionSeconds = (state.positionMillis / 1000L).toInt()
            val durationSeconds = (state.durationMillis / 1000L).toInt()
            runCatching {
                repository.updateHistory(story.id, positionSeconds, durationSeconds)
                settings.saveLastPlayback(story.id, story.albumId, positionSeconds)
            }
        }
    }
}
