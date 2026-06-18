package net.lijue.storybox.ui.screen.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.lijue.storybox.ui.components.EmptyState
import net.lijue.storybox.ui.components.ErrorState
import net.lijue.storybox.ui.components.LoadingState
import net.lijue.storybox.ui.components.StoryBoxScaffold
import net.lijue.storybox.ui.components.StoryListItem

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onPlayer: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StoryBoxScaffold(
        title = "搜索故事",
        actions = { TextButton(onClick = onBack) { Text("返回") } }
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = state.keyword,
                        onValueChange = viewModel::updateKeyword,
                        modifier = Modifier.weight(1f),
                        label = { Text("关键词") },
                        singleLine = true
                    )
                    Button(onClick = viewModel::search, enabled = !state.isLoading) {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                }
            }
            if (state.isLoading) {
                item { LoadingState("正在搜索...") }
            } else if (state.errorMessage.isNotBlank()) {
                item { ErrorState(state.errorMessage, viewModel::search) }
            } else if (state.results.isEmpty()) {
                item { EmptyState("输入关键词，找一个故事听吧。") }
            } else {
                items(state.results, key = { it.id }) { story ->
                    StoryListItem(story = story, onClick = { viewModel.play(story, onPlayer) })
                }
            }
        }
    }
}
