package net.lijue.storybox.ui.screen.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateHome: () -> Unit,
    onNavigateSetup: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.destination) {
        when (state.destination) {
            SplashDestination.Home -> onNavigateHome()
            SplashDestination.Setup -> onNavigateSetup(state.setupMessage)
            null -> Unit
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically)
    ) {
        Icon(
            Icons.Default.PlayCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text("老李讲故事", style = MaterialTheme.typography.titleLarge)
        CircularProgressIndicator()
        Text(state.message, style = MaterialTheme.typography.bodyLarge)
    }
}
