package net.lijue.storybox.ui.screen.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.lijue.storybox.core.datastore.SettingsDataStore
import net.lijue.storybox.data.repository.ApiRepository

enum class SplashDestination { Home, Setup }

data class SplashUiState(
    val message: String = "正在连接老李讲故事...",
    val destination: SplashDestination? = null,
    val setupMessage: String = ""
)

class SplashViewModel(
    private val settings: SettingsDataStore,
    private val repository: ApiRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SplashUiState())
    val state: StateFlow<SplashUiState> = _state

    init {
        checkServer()
    }

    private fun checkServer() {
        viewModelScope.launch {
            settings.ensureDeviceId()
            val server = settings.serverBaseUrlFlow.first()
            if (server.isBlank()) {
                _state.value = SplashUiState(
                    message = "请先配置服务器",
                    destination = SplashDestination.Setup
                )
                return@launch
            }
            runCatching { repository.health() }
                .onSuccess {
                    _state.value = SplashUiState(destination = SplashDestination.Home)
                }
                .onFailure {
                    _state.value = SplashUiState(
                        message = "连接失败",
                        destination = SplashDestination.Setup,
                        setupMessage = "服务器连接失败，请检查地址"
                    )
                }
        }
    }
}
