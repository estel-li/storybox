package net.lijue.storybox.ui.screen.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.lijue.storybox.core.common.UiState
import net.lijue.storybox.ui.components.EmptyState
import net.lijue.storybox.ui.components.ErrorState
import net.lijue.storybox.ui.components.LoadingState
import net.lijue.storybox.ui.components.StoryBoxScaffold
import net.lijue.storybox.ui.components.StoryListItem

@Composable
fun AlbumDetailScreen(
    viewModel: AlbumDetailViewModel,
    onBack: () -> Unit,
    onPlayer: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StoryBoxScaffold(
        title = "专辑详情",
        actions = { TextButton(onClick = onBack) { Text("返回") } }
    ) {
        when (val value = state) {
            UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(value.message, viewModel::load)
            is UiState.Success -> AlbumDetailContent(
                data = value.data,
                onPlayAll = { viewModel.playAll(onPlayer) },
                onStory = { viewModel.playFrom(it, onPlayer) }
            )
        }
    }
}

@Composable
private fun AlbumDetailContent(
    data: AlbumDetailData,
    onPlayAll: () -> Unit,
    onStory: (net.lijue.storybox.core.model.StoryDto) -> Unit
) {
    if (data.stories.isEmpty()) {
        EmptyState("这个专辑还没有故事。")
        return
    }
    val expandedChapters = remember(data.album.id) { mutableStateMapOf<String, Boolean>() }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            text = data.album.displayName.ifBlank { data.album.name },
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (data.album.description.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(data.album.description)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onPlayAll, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text("播放全部")
                        }
                    }
                }
            }
        }
        data.groupedStories.forEach { (chapter, stories) ->
            item(key = "chapter-$chapter") {
                val isExpanded = expandedChapters[chapter] == true
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedChapters[chapter] = !isExpanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(chapter, style = MaterialTheme.typography.titleMedium)
                            Text("${stories.size} 个故事", style = MaterialTheme.typography.bodyMedium)
                        }
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "收起" else "展开"
                        )
                    }
                }
            }
            if (expandedChapters[chapter] == true) {
                items(stories, key = { it.id }) { story ->
                    StoryListItem(story = story, onClick = { onStory(story) })
                }
            }
        }
    }
}
