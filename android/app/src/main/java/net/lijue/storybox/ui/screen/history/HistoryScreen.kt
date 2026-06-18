package net.lijue.storybox.ui.screen.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.lijue.storybox.core.common.UiState
import net.lijue.storybox.ui.components.EmptyState
import net.lijue.storybox.ui.components.ErrorState
import net.lijue.storybox.ui.components.LargeCard
import net.lijue.storybox.ui.components.LoadingState
import net.lijue.storybox.ui.components.StoryBoxScaffold

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onPlayer: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StoryBoxScaffold(
        title = "继续听",
        actions = { TextButton(onClick = onBack) { Text("返回") } }
    ) {
        when (val value = state) {
            UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(value.message, viewModel::load)
            is UiState.Success -> {
                if (value.data.isEmpty()) EmptyState("还没有播放记录。")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(value.data, key = { it.id }) { item ->
                        LargeCard(onClick = { viewModel.continuePlay(item, onPlayer) }) {
                            Icon(Icons.Default.History, contentDescription = null)
                            Column {
                                Text(item.story?.safeTitle ?: "故事 ${item.storyId}")
                                Spacer(Modifier.height(4.dp))
                                Text("进度 ${item.positionSeconds / 60} 分钟 · ${item.playedAt}")
                            }
                        }
                    }
                }
            }
        }
    }
}
