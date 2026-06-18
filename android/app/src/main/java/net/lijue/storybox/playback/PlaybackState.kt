package net.lijue.storybox.playback

import net.lijue.storybox.core.model.SleepTimerMode
import net.lijue.storybox.core.model.StoryDto

data class StoryPlaybackState(
    val queue: PlaybackQueue = PlaybackQueue(),
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMillis: Long = 0L,
    val durationMillis: Long = 0L,
    val bufferedPercentage: Int = 0,
    val errorMessage: String? = null,
    val sleepTimerMode: SleepTimerMode = SleepTimerMode.Off,
    val sleepRemainingMillis: Long = 0L
) {
    val currentStory: StoryDto? get() = queue.currentStory
    val canSkipPrevious: Boolean get() = queue.currentIndex > 0
    val canSkipNext: Boolean get() = queue.currentIndex < queue.stories.lastIndex
}
