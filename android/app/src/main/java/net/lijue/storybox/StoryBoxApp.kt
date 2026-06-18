package net.lijue.storybox

import android.app.Application
import net.lijue.storybox.core.datastore.SettingsDataStore
import net.lijue.storybox.data.repository.ApiRepository
import net.lijue.storybox.playback.StoryPlayerManager

class StoryBoxApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: Application) {
    val settingsDataStore = SettingsDataStore(app.applicationContext)
    val apiRepository = ApiRepository(settingsDataStore)
    val playerManager = StoryPlayerManager(app.applicationContext, settingsDataStore, apiRepository)
}
