package net.lijue.storybox.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.lijue.storybox.core.common.toFriendlyMessage
import net.lijue.storybox.core.datastore.SettingsDataStore
import net.lijue.storybox.data.repository.ApiRepository

data class SettingsUiState(
    val serverBaseUrl: String = "",
    val deviceId: String = "",
    val message: String = "",
    val isTesting: Boolean = false
)

class SettingsViewModel(
    private val settings: SettingsDataStore,
    private val repository: ApiRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val appSettings = settings.settingsFlow.first()
            val deviceId = settings.ensureDeviceId()
            _state.value = SettingsUiState(
                serverBaseUrl = appSettings.serverBaseUrl,
                deviceId = deviceId
            )
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isTesting = true, message = "")
            runCatching { repository.health() }
                .onSuccess {
                    _state.value = _state.value.copy(isTesting = false, message = "连接正常，后端版本 ${it.version}")
                }
                .onFailure {
                    _state.value = _state.value.copy(isTesting = false, message = it.toFriendlyMessage())
                }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            settings.clearLocalPlaybackCache()
            load()
            _state.value = _state.value.copy(message = "本地播放缓存已清空。")
        }
    }
}
