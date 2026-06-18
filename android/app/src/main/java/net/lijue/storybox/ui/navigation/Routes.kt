package net.lijue.storybox.ui.navigation

import android.net.Uri

object Routes {
    const val Splash = "splash"
    const val Setup = "setup?message={message}"
    const val Home = "home"
    const val Categories = "categories"
    const val AlbumList = "albums/{categoryId}"
    const val AlbumDetail = "album/{albumId}"
    const val Search = "search"
    const val Favorites = "favorites"
    const val History = "history"
    const val Player = "player"
    const val Settings = "settings"

    fun setup(message: String = "") = "setup?message=${Uri.encode(message)}"
    fun albumList(categoryId: Long) = "albums/$categoryId"
    fun albumDetail(albumId: Long) = "album/$albumId"
}
