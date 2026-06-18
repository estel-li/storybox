package net.lijue.storybox.ui.screen.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.lijue.storybox.core.model.SleepTimerMode
import net.lijue.storybox.ui.components.EmptyState
import net.lijue.storybox.ui.components.StoryBoxScaffold

@Composable
fun PlayerScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val story = playback.currentStory

    StoryBoxScaffold(
        title = "正在播放",
        actions = { TextButton(onClick = onBack) { Text("返回") } }
    ) {
        if (story == null) {
            EmptyState("还没有选择故事。")
            return@StoryBoxScaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = story.safeTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = story.album?.displayName.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (story.chapter.isNotBlank()) {
                            Text(
                                text = story.chapter,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        if (playback.isBuffering) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                        }
                        Slider(
                            value = playback.positionMillis.toFloat(),
                            onValueChange = { viewModel.seekTo(it.toLong()) },
                            valueRange = 0f..playback.durationMillis.coerceAtLeast(1L).toFloat(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatMillis(playback.positionMillis))
                            Text(formatMillis(playback.durationMillis))
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = viewModel::previous, enabled = playback.canSkipPrevious) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "上一首")
                            }
                            IconButton(onClick = viewModel::playPause) {
                                Icon(
                                    if (playback.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                    contentDescription = "播放暂停"
                                )
                            }
                            IconButton(onClick = viewModel::next, enabled = playback.canSkipNext) {
                                Icon(Icons.Default.SkipNext, contentDescription = "下一首")
                            }
                            IconButton(onClick = viewModel::toggleFavorite) {
                                Icon(
                                    if (story.id in uiState.favoriteIds) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "收藏"
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text("睡眠定时", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SleepTimerMode.entries) { mode ->
                        FilterChip(
                            selected = playback.sleepTimerMode == mode,
                            onClick = { viewModel.setSleepTimer(mode) },
                            label = { Text(mode.label) },
                            leadingIcon = {
                                if (mode != SleepTimerMode.Off) {
                                    Icon(Icons.Default.Timer, contentDescription = null)
                                }
                            }
                        )
                    }
                }
                if (playback.sleepRemainingMillis > 0) {
                    Text("将在 ${playback.sleepRemainingMillis / 1000 / 60 + 1} 分钟后暂停")
                }
            }

            if (uiState.progressMessage.isNotBlank() || playback.errorMessage != null) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(uiState.progressMessage.ifBlank { playback.errorMessage.orEmpty() })
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = viewModel::clearMessage) { Text("知道了") }
                        }
                    }
                }
            }

            item {
                Text("播放队列", style = MaterialTheme.typography.titleMedium)
            }
            itemsIndexed(playback.queue.stories, key = { _, item -> item.id }) { index, item ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.playQueueItem(index) }
                ) {
                    Text(
                        text = item.safeTitle,
                        modifier = Modifier.padding(16.dp),
                        color = if (item.id == story.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun formatMillis(value: Long): String {
    val seconds = (value / 1000L).coerceAtLeast(0L)
    val minute = seconds / 60L
    val second = seconds % 60L
    return "$minute:${second.toString().padStart(2, '0')}"
}
