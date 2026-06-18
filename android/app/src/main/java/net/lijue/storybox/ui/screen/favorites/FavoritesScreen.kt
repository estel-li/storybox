package net.lijue.storybox.ui.screen.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.lijue.storybox.core.common.UiState
import net.lijue.storybox.ui.components.EmptyState
import net.lijue.storybox.ui.components.ErrorState
import net.lijue.storybox.ui.components.LoadingState
import net.lijue.storybox.ui.components.StoryBoxScaffold
import net.lijue.storybox.ui.components.StoryListItem

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onBack: () -> Unit,
    onPlayer: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StoryBoxScaffold(
        title = "我的收藏",
        actions = { TextButton(onClick = onBack) { Text("返回") } }
    ) {
        when (val value = state) {
            UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(value.message, viewModel::load)
            is UiState.Success -> {
                if (value.data.isEmpty()) EmptyState("还没有收藏故事。")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(value.data, key = { it.id }) { story ->
                        StoryListItem(
                            story = story,
                            isFavorite = true,
                            onClick = { viewModel.play(story, onPlayer) },
                            onFavoriteClick = { viewModel.remove(story) }
                        )
                    }
                }
            }
        }
    }
}
