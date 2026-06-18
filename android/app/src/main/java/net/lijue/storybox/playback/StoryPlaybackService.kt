package net.lijue.storybox.playback

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import net.lijue.storybox.StoryBoxApp

class StoryPlaybackService : MediaSessionService() {
    // Media3 会通过这个 session 暴露通知栏、锁屏、耳机按钮等系统播放控制。
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
        (application as StoryBoxApp).container.playerManager.mediaSession
}
