package net.lijue.storybox.ui.screen.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.lijue.storybox.core.common.UiState
import net.lijue.storybox.core.common.toFriendlyMessage
import net.lijue.storybox.core.model.CategoryDto
import net.lijue.storybox.data.repository.ApiRepository

class CategoryViewModel(private val repository: ApiRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<CategoryDto>>>(UiState.Loading)
    val state: StateFlow<UiState<List<CategoryDto>>> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { repository.categories() }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(it.toFriendlyMessage()) }
        }
    }
}
