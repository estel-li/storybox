package net.lijue.storybox.ui.screen.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.lijue.storybox.core.common.toFriendlyMessage
import net.lijue.storybox.core.datastore.SettingsDataStore
import net.lijue.storybox.data.repository.ApiRepository

data class ServerSetupUiState(
    val serverUrl: String = "",
    val isTesting: Boolean = false,
    val errorMessage: String = "",
    val connected: Boolean = false
)

class ServerSetupViewModel(
    private val settings: SettingsDataStore,
    private val repository: ApiRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ServerSetupUiState())
    val state: StateFlow<ServerSetupUiState> = _state

    init {
        viewModelScope.launch {
            val saved = settings.serverBaseUrlFlow.first()
            _state.value = _state.value.copy(serverUrl = saved)
        }
    }

    fun setInitialMessage(message: String) {
        if (message.isNotBlank() && _state.value.errorMessage.isBlank()) {
            _state.value = _state.value.copy(errorMessage = message)
        }
    }

    fun updateServerUrl(value: String) {
        _state.value = _state.value.copy(serverUrl = value, errorMessage = "", connected = false)
    }

    fun testAndSave() {
        val normalized = SettingsDataStore.normalizeBaseUrl(_state.value.serverUrl)
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            _state.value = _state.value.copy(errorMessage = "服务器地址格式不正确。")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isTesting = true, errorMessage = "")
            runCatching { repository.testConnection(normalized) }
                .onSuccess { health ->
                    if (health.ok) {
                        settings.saveServerBaseUrl(normalized)
                        _state.value = _state.value.copy(
                            serverUrl = normalized,
                            isTesting = false,
                            connected = true
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isTesting = false,
                            errorMessage = "服务器响应异常，请确认后端已启动。"
                        )
                    }
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isTesting = false,
                        errorMessage = error.toFriendlyMessage()
                    )
                }
        }
    }
}
