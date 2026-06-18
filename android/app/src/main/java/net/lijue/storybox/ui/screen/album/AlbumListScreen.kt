package net.lijue.storybox.ui.screen.album

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
import net.lijue.storybox.core.model.AlbumDto
import net.lijue.storybox.ui.components.AlbumCard
import net.lijue.storybox.ui.components.EmptyState
import net.lijue.storybox.ui.components.ErrorState
import net.lijue.storybox.ui.components.LoadingState
import net.lijue.storybox.ui.components.StoryBoxScaffold

@Composable
fun AlbumListScreen(
    viewModel: AlbumListViewModel,
    onBack: () -> Unit,
    onAlbum: (AlbumDto) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StoryBoxScaffold(
        title = "专辑",
        actions = { TextButton(onClick = onBack) { Text("返回") } }
    ) {
        when (val value = state) {
            UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(value.message, viewModel::load)
            is UiState.Success -> {
                if (value.data.isEmpty()) EmptyState("这个分类里还没有专辑。")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(value.data, key = { it.id }) { album ->
                        AlbumCard(album, onClick = { onAlbum(album) })
                    }
                }
            }
        }
    }
}
