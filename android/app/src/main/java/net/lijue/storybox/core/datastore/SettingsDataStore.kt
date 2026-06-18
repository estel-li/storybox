package net.lijue.storybox.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.lijue.storybox.core.model.SleepTimerMode

private val Context.storyBoxDataStore by preferencesDataStore("storybox_settings")

data class AppSettings(
    val serverBaseUrl: String = "",
    val deviceId: String = "",
    val lastPlayingStoryId: Long = 0,
    val lastAlbumId: Long = 0,
    val lastPositionSeconds: Int = 0,
    val sleepTimerMode: SleepTimerMode = SleepTimerMode.Off
)

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val serverBaseUrl = stringPreferencesKey("server_base_url")
        val deviceId = stringPreferencesKey("device_id")
        val lastPlayingStoryId = longPreferencesKey("last_playing_story_id")
        val lastAlbumId = longPreferencesKey("last_album_id")
        val lastPositionSeconds = intPreferencesKey("last_position_seconds")
        val sleepTimerMode = stringPreferencesKey("sleep_timer_mode")
    }

    val settingsFlow: Flow<AppSettings> = context.storyBoxDataStore.data.map { prefs ->
        AppSettings(
            serverBaseUrl = prefs[Keys.serverBaseUrl].orEmpty(),
            deviceId = prefs[Keys.deviceId].orEmpty(),
            lastPlayingStoryId = prefs[Keys.lastPlayingStoryId] ?: 0L,
            lastAlbumId = prefs[Keys.lastAlbumId] ?: 0L,
            lastPositionSeconds = prefs[Keys.lastPositionSeconds] ?: 0,
            sleepTimerMode = runCatching {
                SleepTimerMode.valueOf(prefs[Keys.sleepTimerMode] ?: SleepTimerMode.Off.name)
            }.getOrDefault(SleepTimerMode.Off)
        )
    }

    val serverBaseUrlFlow: Flow<String> = settingsFlow.map { it.serverBaseUrl }
    val deviceIdFlow: Flow<String> = settingsFlow.map { it.deviceId }

    // deviceId 是 Android 端替代账号体系的本地身份，用于播放历史和收藏。
    suspend fun ensureDeviceId(): String {
        val current = deviceIdFlow.first()
        if (current.isNotBlank()) return current
        val generated = UUID.randomUUID().toString()
        context.storyBoxDataStore.edit { it[Keys.deviceId] = generated }
        return generated
    }

    suspend fun serverBaseUrl(): String = normalizeBaseUrl(serverBaseUrlFlow.first())

    suspend fun saveServerBaseUrl(value: String) {
        context.storyBoxDataStore.edit { prefs ->
            prefs[Keys.serverBaseUrl] = normalizeBaseUrl(value)
        }
    }

    suspend fun saveLastPlayback(storyId: Long, albumId: Long, positionSeconds: Int) {
        context.storyBoxDataStore.edit { prefs ->
            prefs[Keys.lastPlayingStoryId] = storyId
            prefs[Keys.lastAlbumId] = albumId
            prefs[Keys.lastPositionSeconds] = positionSeconds
        }
    }

    suspend fun saveSleepTimerMode(mode: SleepTimerMode) {
        context.storyBoxDataStore.edit { prefs -> prefs[Keys.sleepTimerMode] = mode.name }
    }

    suspend fun clearLocalPlaybackCache() {
        val currentDeviceId = ensureDeviceId()
        val currentServer = serverBaseUrl()
        context.storyBoxDataStore.edit { prefs ->
            prefs.clear()
            prefs[Keys.deviceId] = currentDeviceId
            if (currentServer.isNotBlank()) prefs[Keys.serverBaseUrl] = currentServer
        }
    }

    companion object {
        // Retrofit 的 baseUrl 必须以 / 结尾，但业务侧统一保存不带末尾 / 的地址。
        fun normalizeBaseUrl(value: String): String = value.trim().trimEnd('/')
    }
}
