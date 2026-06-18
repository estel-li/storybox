package net.lijue.storybox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = StoryGreen,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = StoryOrange,
    tertiary = StoryBlue,
    background = StoryCream,
    surface = androidx.compose.ui.graphics.Color.White,
    onBackground = StoryInk,
    onSurface = StoryInk
)

@Composable
fun StoryBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = StoryTypography,
        content = content
    )
}
