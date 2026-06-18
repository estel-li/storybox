package net.lijue.storybox.data.repository

import kotlinx.coroutines.flow.first
import net.lijue.storybox.core.datastore.SettingsDataStore
import net.lijue.storybox.core.model.AlbumDto
import net.lijue.storybox.core.model.CategoryDto
import net.lijue.storybox.core.model.FavoriteDto
import net.lijue.storybox.core.model.HealthResponse
import net.lijue.storybox.core.model.PlayHistoryDto
import net.lijue.storybox.core.model.StoryDto
import net.lijue.storybox.core.model.UpdateHistoryRequest
import net.lijue.storybox.core.network.ApiClient
import net.lijue.storybox.data.api.StoryBoxApi

data class PlaybackStart(
    val stories: List<StoryDto>,
    val startIndex: Int
)

class ApiRepository(private val settings: SettingsDataStore) {
    // 后端地址允许运行时修改，因此每次请求按当前 DataStore 值创建 API。
    private suspend fun api(): StoryBoxApi {
        val baseUrl = settings.serverBaseUrl()
        require(baseUrl.isNotBlank()) { "服务器地址未配置" }
        return ApiClient.create(baseUrl).create(StoryBoxApi::class.java)
    }

    private fun apiFor(baseUrl: String): StoryBoxApi {
        val normalized = SettingsDataStore.normalizeBaseUrl(baseUrl)
        require(normalized.startsWith("http://") || normalized.startsWith("https://")) {
            "服务器地址格式不正确"
        }
        return ApiClient.create(normalized).create(StoryBoxApi::class.java)
    }

    suspend fun testConnection(baseUrl: String): HealthResponse = apiFor(baseUrl).health()

    suspend fun health(): HealthResponse = api().health()

    suspend fun categories(): List<CategoryDto> = api().getCategories().items

    suspend fun albumsByCategory(categoryId: Long): List<AlbumDto> =
        api().getAlbumsByCategory(categoryId).items

    suspend fun allAlbums(): List<AlbumDto> = api().getAlbums().items

    suspend fun album(albumId: Long): AlbumDto = api().getAlbum(albumId)

    suspend fun storiesByAlbum(albumId: Long): List<StoryDto> =
        api().getStoriesByAlbum(albumId).items

    suspend fun story(storyId: Long): StoryDto = api().getStory(storyId)

    suspend fun playbackQueueFor(story: StoryDto): PlaybackStart {
        if (story.albumId <= 0) return PlaybackStart(listOf(story), 0)

        val album = story.album ?: runCatching { album(story.albumId) }.getOrNull()
        val category = story.category
        val albumStories = runCatching { storiesByAlbum(story.albumId) }.getOrDefault(emptyList())
        val queue = albumStories
            .ifEmpty { listOf(story) }
            .map { item ->
                item.copy(
                    album = item.album ?: album,
                    category = item.category ?: category
                )
            }
        val startIndex = queue.indexOfFirst { it.id == story.id }
        if (startIndex >= 0) return PlaybackStart(queue, startIndex)
        return PlaybackStart(listOf(story), 0)
    }

    suspend fun search(keyword: String): List<StoryDto> =
        if (keyword.isBlank()) emptyList() else api().search(keyword.trim()).items

    suspend fun history(): List<PlayHistoryDto> {
        val deviceId = settings.ensureDeviceId()
        return api().getHistory(deviceId).items
    }

    suspend fun updateHistory(storyId: Long, positionSeconds: Int, durationSeconds: Int) {
        val deviceId = settings.ensureDeviceId()
        api().updateHistory(
            deviceId = deviceId,
            body = UpdateHistoryRequest(
                storyId = storyId,
                positionSeconds = positionSeconds.coerceAtLeast(0),
                durationSeconds = durationSeconds.coerceAtLeast(0)
            )
        )
    }

    suspend fun favorites(): List<FavoriteDto> {
        val deviceId = settings.ensureDeviceId()
        return api().getFavorites(deviceId).items
    }

    suspend fun favoriteStories(): List<StoryDto> =
        favorites().mapNotNull { it.story }

    suspend fun favoriteIds(): Set<Long> =
        favorites().map { it.storyId }.toSet()

    suspend fun addFavorite(storyId: Long) {
        val deviceId = settings.ensureDeviceId()
        api().addFavorite(deviceId, storyId)
    }

    suspend fun removeFavorite(storyId: Long) {
        val deviceId = settings.ensureDeviceId()
        api().removeFavorite(deviceId, storyId)
    }

    suspend fun streamUrl(storyId: Long): String {
        val baseUrl = settings.serverBaseUrlFlow.first().trim().trimEnd('/')
        require(baseUrl.isNotBlank()) { "服务器地址未配置" }
        return "$baseUrl/api/stories/$storyId/stream"
    }
}
