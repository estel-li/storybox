package net.lijue.storybox.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import net.lijue.storybox.AppContainer
import net.lijue.storybox.core.common.ViewModelFactory
import net.lijue.storybox.ui.components.PlayerBottomBar
import net.lijue.storybox.ui.screen.album.AlbumDetailScreen
import net.lijue.storybox.ui.screen.album.AlbumDetailViewModel
import net.lijue.storybox.ui.screen.album.AlbumListScreen
import net.lijue.storybox.ui.screen.album.AlbumListViewModel
import net.lijue.storybox.ui.screen.category.CategoryScreen
import net.lijue.storybox.ui.screen.category.CategoryViewModel
import net.lijue.storybox.ui.screen.favorites.FavoritesScreen
import net.lijue.storybox.ui.screen.favorites.FavoritesViewModel
import net.lijue.storybox.ui.screen.history.HistoryScreen
import net.lijue.storybox.ui.screen.history.HistoryViewModel
import net.lijue.storybox.ui.screen.home.HomeScreen
import net.lijue.storybox.ui.screen.home.HomeViewModel
import net.lijue.storybox.ui.screen.player.PlayerScreen
import net.lijue.storybox.ui.screen.player.PlayerViewModel
import net.lijue.storybox.ui.screen.search.SearchScreen
import net.lijue.storybox.ui.screen.search.SearchViewModel
import net.lijue.storybox.ui.screen.settings.SettingsScreen
import net.lijue.storybox.ui.screen.settings.SettingsViewModel
import net.lijue.storybox.ui.screen.setup.ServerSetupScreen
import net.lijue.storybox.ui.screen.setup.ServerSetupViewModel
import net.lijue.storybox.ui.screen.splash.SplashScreen
import net.lijue.storybox.ui.screen.splash.SplashViewModel

@Composable
fun StoryBoxNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val playbackState by container.playerManager.state.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route.orEmpty()
    val showPlayerBar = playbackState.currentStory != null &&
        route !in setOf(Routes.Splash, Routes.Setup, Routes.Player)

    Scaffold(
        bottomBar = {
            if (showPlayerBar) {
                PlayerBottomBar(
                    state = playbackState,
                    onOpenPlayer = { navController.navigate(Routes.Player) },
                    onPlayPause = { container.playerManager.playPause() }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.Splash,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable(Routes.Splash) {
                val vm: SplashViewModel = viewModel(
                    factory = ViewModelFactory {
                        SplashViewModel(container.settingsDataStore, container.apiRepository)
                    }
                )
                SplashScreen(
                    viewModel = vm,
                    onNavigateHome = {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Splash) { inclusive = true }
                        }
                    },
                    onNavigateSetup = { message ->
                        navController.navigate(Routes.setup(message)) {
                            popUpTo(Routes.Splash) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.Setup,
                arguments = listOf(navArgument("message") { defaultValue = "" })
            ) { entry ->
                val vm: ServerSetupViewModel = viewModel(
                    factory = ViewModelFactory {
                        ServerSetupViewModel(container.settingsDataStore, container.apiRepository)
                    }
                )
                ServerSetupScreen(
                    viewModel = vm,
                    initialMessage = entry.arguments?.getString("message").orEmpty(),
                    onConnected = {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Setup) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.Home) {
                val vm: HomeViewModel = viewModel(
                    factory = ViewModelFactory {
                        HomeViewModel(container.apiRepository, container.playerManager)
                    }
                )
                HomeScreen(
                    viewModel = vm,
                    onCategories = { navController.navigate(Routes.Categories) },
                    onCategory = { navController.navigate(Routes.albumList(it.id)) },
                    onAlbum = { navController.navigate(Routes.albumDetail(it.id)) },
                    onSearch = { navController.navigate(Routes.Search) },
                    onFavorites = { navController.navigate(Routes.Favorites) },
                    onHistory = { navController.navigate(Routes.History) },
                    onSettings = { navController.navigate(Routes.Settings) },
                    onPlayer = { navController.navigate(Routes.Player) }
                )
            }

            composable(Routes.Categories) {
                val vm: CategoryViewModel = viewModel(
                    factory = ViewModelFactory { CategoryViewModel(container.apiRepository) }
                )
                CategoryScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onCategory = { navController.navigate(Routes.albumList(it.id)) }
                )
            }

            composable(
                route = Routes.AlbumList,
                arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
            ) { entry ->
                val categoryId = entry.arguments?.getLong("categoryId") ?: 0L
                val vm: AlbumListViewModel = viewModel(
                    key = "albumList-$categoryId",
                    factory = ViewModelFactory { AlbumListViewModel(categoryId, container.apiRepository) }
                )
                AlbumListScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onAlbum = { navController.navigate(Routes.albumDetail(it.id)) }
                )
            }

            composable(
                route = Routes.AlbumDetail,
                arguments = listOf(navArgument("albumId") { type = NavType.LongType })
            ) { entry ->
                val albumId = entry.arguments?.getLong("albumId") ?: 0L
                val vm: AlbumDetailViewModel = viewModel(
                    key = "albumDetail-$albumId",
                    factory = ViewModelFactory {
                        AlbumDetailViewModel(albumId, container.apiRepository, container.playerManager)
                    }
                )
                AlbumDetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onPlayer = { navController.navigate(Routes.Player) }
                )
            }

            composable(Routes.Search) {
                val vm: SearchViewModel = viewModel(
                    factory = ViewModelFactory {
                        SearchViewModel(container.apiRepository, container.playerManager)
                    }
                )
                SearchScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onPlayer = { navController.navigate(Routes.Player) }
                )
            }

            composable(Routes.Favorites) {
                val vm: FavoritesViewModel = viewModel(
                    factory = ViewModelFactory {
                        FavoritesViewModel(container.apiRepository, container.playerManager)
                    }
                )
                FavoritesScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onPlayer = { navController.navigate(Routes.Player) }
                )
            }

            composable(Routes.History) {
                val vm: HistoryViewModel = viewModel(
                    factory = ViewModelFactory {
                        HistoryViewModel(container.apiRepository, container.playerManager)
                    }
                )
                HistoryScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onPlayer = { navController.navigate(Routes.Player) }
                )
            }

            composable(Routes.Player) {
                val vm: PlayerViewModel = viewModel(
                    factory = ViewModelFactory {
                        PlayerViewModel(container.apiRepository, container.settingsDataStore, container.playerManager)
                    }
                )
                PlayerScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }

            composable(Routes.Settings) {
                val vm: SettingsViewModel = viewModel(
                    factory = ViewModelFactory {
                        SettingsViewModel(container.settingsDataStore, container.apiRepository)
                    }
                )
                SettingsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onSetup = { navController.navigate(Routes.setup()) }
                )
            }
        }
    }
}
