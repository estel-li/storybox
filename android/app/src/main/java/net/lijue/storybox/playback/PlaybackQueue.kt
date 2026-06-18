package net.lijue.storybox.playback

import net.lijue.storybox.core.model.StoryDto

data class PlaybackQueue(
    val stories: List<StoryDto> = emptyList(),
    val currentIndex: Int = 0
) {
    val currentStory: StoryDto?
        get() = stories.getOrNull(currentIndex)

    fun withIndex(index: Int): PlaybackQueue =
        copy(currentIndex = index.coerceIn(0, (stories.size - 1).coerceAtLeast(0)))
}
