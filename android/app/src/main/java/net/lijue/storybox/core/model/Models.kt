package net.lijue.storybox.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val ok: Boolean = false,
    val version: String = ""
)

@Serializable
data class CategoryDto(
    val id: Long,
    val name: String = "",
    @SerialName("display_name")
    val displayName: String = "",
    @SerialName("sort_order")
    val sortOrder: Int = 999999,
    @SerialName("is_visible")
    val isVisible: Boolean = true
)

@Serializable
data class AlbumDto(
    val id: Long,
    @SerialName("category_id")
    val categoryId: Long = 0,
    val category: CategoryDto? = null,
    val name: String = "",
    @SerialName("display_name")
    val displayName: String = "",
    @SerialName("cover_path")
    val coverPath: String = "",
    val description: String = "",
    @SerialName("sort_order")
    val sortOrder: Int = 999999,
    @SerialName("story_count")
    val storyCount: Int = 0,
    @SerialName("is_visible")
    val isVisible: Boolean = true
)

@Serializable
data class StoryDto(
    val id: Long,
    @SerialName("category_id")
    val categoryId: Long = 0,
    val category: CategoryDto? = null,
    @SerialName("album_id")
    val albumId: Long = 0,
    val album: AlbumDto? = null,
    val chapter: String = "",
    val title: String = "",
    @SerialName("display_title")
    val displayTitle: String = "",
    @SerialName("sort_order")
    val sortOrder: Int = 999999,
    @SerialName("relative_path")
    val relativePath: String = "",
    @SerialName("file_ext")
    val fileExt: String = "",
    @SerialName("file_size")
    val fileSize: Long = 0,
    @SerialName("duration_seconds")
    val durationSeconds: Int = 0,
    @SerialName("is_visible")
    val isVisible: Boolean = true,
    @SerialName("is_missing")
    val isMissing: Boolean = false
) {
    val safeTitle: String get() = displayTitle.ifBlank { title.ifBlank { "未命名故事" } }
}

typealias SearchResultDto = StoryDto

@Serializable
data class PlayHistoryDto(
    val id: Long = 0,
    @SerialName("device_id")
    val deviceId: String = "",
    @SerialName("story_id")
    val storyId: Long = 0,
    val story: StoryDto? = null,
    @SerialName("position_seconds")
    val positionSeconds: Int = 0,
    @SerialName("duration_seconds")
    val durationSeconds: Int = 0,
    @SerialName("played_at")
    val playedAt: String = ""
)

@Serializable
data class FavoriteDto(
    val id: Long = 0,
    @SerialName("device_id")
    val deviceId: String = "",
    @SerialName("story_id")
    val storyId: Long = 0,
    val story: StoryDto? = null,
    @SerialName("created_at")
    val createdAt: String = ""
)

@Serializable
data class UpdateHistoryRequest(
    @SerialName("story_id")
    val storyId: Long,
    @SerialName("position_seconds")
    val positionSeconds: Int,
    @SerialName("duration_seconds")
    val durationSeconds: Int
)

@Serializable
data class ApiPageResponse<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("page_size")
    val pageSize: Int = 20
)

enum class SleepTimerMode(val label: String) {
    Off("关闭"),
    Minutes15("15 分钟"),
    Minutes30("30 分钟"),
    Minutes60("60 分钟"),
    EndOfStory("播完当前故事后停止");

    val durationMillis: Long
        get() = when (this) {
            Off, EndOfStory -> 0L
            Minutes15 -> 15L * 60L * 1000L
            Minutes30 -> 30L * 60L * 1000L
            Minutes60 -> 60L * 60L * 1000L
        }
}
