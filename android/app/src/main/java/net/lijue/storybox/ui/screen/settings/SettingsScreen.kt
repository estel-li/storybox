package net.lijue.storybox.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.lijue.storybox.ui.components.StoryBoxScaffold

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onSetup: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StoryBoxScaffold(
        title = "设置",
        actions = { TextButton(onClick = onBack) { Text("返回") } }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Icon(Icons.Default.Router, contentDescription = null)
                    Spacer(Modifier.height(8.dp))
                    Text("服务器地址", style = MaterialTheme.typography.titleMedium)
                    Text(state.serverBaseUrl.ifBlank { "未配置" })
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::testConnection, enabled = !state.isTesting) {
                        Text(if (state.isTesting) "正在测试..." else "测试连接")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onSetup) {
                        Text("修改服务器地址")
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("设备 ID", style = MaterialTheme.typography.titleMedium)
                    Text(state.deviceId)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = viewModel::clearCache) {
                        Text("清空本地播放缓存")
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(Modifier.height(8.dp))
                    Text("关于 App", style = MaterialTheme.typography.titleMedium)
                    Text("家庭故事盒 Android MVP，用于在局域网内播放 NAS 中的儿童故事。")
                    Text("手机和 NAS 必须在同一局域网。")
                }
            }

            if (state.message.isNotBlank()) {
                Text(state.message, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
