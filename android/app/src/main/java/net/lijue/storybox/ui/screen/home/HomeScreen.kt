package net.lijue.storybox.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.lijue.storybox.core.common.UiState
import net.lijue.storybox.core.model.AlbumDto
import net.lijue.storybox.core.model.CategoryDto
import net.lijue.storybox.ui.components.AlbumCard
import net.lijue.storybox.ui.components.CategoryCard
import net.lijue.storybox.ui.components.EmptyState
import net.lijue.storybox.ui.components.ErrorState
import net.lijue.storybox.ui.components.LargeCard
import net.lijue.storybox.ui.components.LoadingState
import net.lijue.storybox.ui.components.StoryBoxScaffold

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCategories: () -> Unit,
    onCategory: (CategoryDto) -> Unit,
    onAlbum: (AlbumDto) -> Unit,
    onSearch: () -> Unit,
    onFavorites: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onPlayer: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StoryBoxScaffold(
        title = "老李讲故事",
        actions = {
            OutlinedButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Text("设置")
            }
        }
    ) {
        when (val value = state) {
            UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(value.message, viewModel::load)
            is UiState.Success -> HomeContent(
                data = value.data,
                onCategories = onCategories,
                onCategory = onCategory,
                onAlbum = onAlbum,
                onSearch = onSearch,
                onFavorites = onFavorites,
                onHistory = onHistory,
                onContinue = { item -> viewModel.continuePlay(item, onPlayer) }
            )
        }
    }
}

@Composable
private fun HomeContent(
    data: HomeData,
    onCategories: () -> Unit,
    onCategory: (CategoryDto) -> Unit,
    onAlbum: (AlbumDto) -> Unit,
    onSearch: () -> Unit,
    onFavorites: () -> Unit,
    onHistory: () -> Unit,
    onContinue: (net.lijue.storybox.core.model.PlayHistoryDto) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(onClick = onSearch, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Text("搜索")
                }
                Button(onClick = onFavorites, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Favorite, contentDescription = null)
                    Text("收藏")
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onHistory, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.History, contentDescription = null)
                Text("继续听")
            }
        }

        item {
            Text("继续听", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (data.history.isEmpty()) {
                EmptyState("还没有播放记录")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.history.take(3).forEach { history ->
                        LargeCard(onClick = { onContinue(history) }) {
                            Icon(Icons.Default.History, contentDescription = null)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(history.story?.safeTitle ?: "故事 ${history.storyId}")
                                Text("已听到 ${history.positionSeconds / 60} 分钟")
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("分类", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onCategories) { Text("全部") }
            }
        }
        items(data.categories.take(6), key = { "category-${it.id}" }) { category ->
            CategoryCard(category, onClick = { onCategory(category) })
        }

        item {
            Text("最近专辑", style = MaterialTheme.typography.titleMedium)
        }
        items(data.albums, key = { "album-${it.id}" }) { album ->
            AlbumCard(album, onClick = { onAlbum(album) })
        }
    }
}
