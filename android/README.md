# 家庭故事盒 Android

这是“老李讲故事”的 Android 原生客户端。它使用 Kotlin、Jetpack Compose、Material 3、Retrofit、DataStore Preferences 和 Media3/ExoPlayer，实现局域网故事浏览、搜索、播放、后台播放、锁屏控制、收藏、继续听和睡眠定时。

## 如何用 Android Studio 打开

1. 安装 Android Studio 当前稳定版。
2. 打开本目录：`老李讲故事/android/`。
3. 等待 Gradle Sync 完成。
4. 连接 Android 真机，点击 Run。

项目配置：

```text
package = net.lijue.storybox
minSdk = 26
compileSdk = 36
targetSdk = 36
```

## 如何运行到真机

1. 先启动后端：

```bash
cd ../backend
go run ./cmd/server
```

2. 确认手机和后端机器在同一局域网。
3. 在手机浏览器访问后端健康检查地址，例如：

```text
http://192.168.1.10:8080/api/health
```

4. Android Studio 运行 App 到真机。

注意：真机不能使用 `http://localhost:8080` 连接电脑上的后端。请使用电脑或 NAS 的局域网 IP，例如 `http://192.168.1.10:8080`。

## 后端地址在哪里配置

App 首次启动会进入“服务器配置页”，填写：

```text
http://192.168.1.10:8080
```

地址会保存到 DataStore 的 `server_base_url`。设置页可以重新测试连接或修改服务器地址。

## 首次启动流程

1. Splash 读取 DataStore。
2. 如果没有 `server_base_url`，进入服务器配置页。
3. 如果已有地址，调用 `/api/health`。
4. 健康检查成功后进入首页。
5. 连接失败时回到服务器配置页，并提示检查地址。

## 主要页面说明

- `Splash`：启动检查。
- `ServerSetup`：填写和测试后端地址。
- `Home`：继续听、分类、最近专辑、收藏、搜索、设置入口。
- `CategoryList`：全部分类。
- `AlbumList`：分类下专辑。
- `AlbumDetail`：专辑详情、章节分组、播放全部、故事列表。
- `Search`：关键词搜索故事。
- `Favorites`：收藏故事，支持取消收藏。
- `History`：继续听，从上次位置播放。
- `Player`：播放控制、进度拖动、上一首、下一首、收藏、睡眠定时、播放队列。
- `Settings`：服务器地址、设备 ID、清空本地播放缓存、关于。

## 播放服务如何工作

- `StoryPlayerManager` 持有 ExoPlayer、MediaSession 和播放队列。
- `StoryPlaybackService` 继承 `MediaSessionService`，向系统暴露 MediaSession。
- 播放故事时，App 构建 URL：

```kotlin
"$serverBaseUrl/api/stories/${story.id}/stream"
```

- 播放器只访问后端音频流，不直接使用 NAS 文件路径。
- 后端支持 HTTP Range，Media3 可以拖动进度。

## 如何测试后台播放

1. 打开专辑详情。
2. 点击“播放全部”或某个故事。
3. 回到桌面。
4. 确认故事继续播放。
5. 下拉通知栏，测试播放/暂停、上一首、下一首。

Android 13 及以上如果未授权通知权限，后台播放仍会尽量可用，但通知可能不显示。

## 如何测试锁屏控制

1. 播放故事。
2. 锁屏。
3. 在锁屏界面使用系统媒体控件。
4. 验证播放/暂停、上一首、下一首可用。

## 如何测试收藏和继续听

收藏：

1. 播放任意故事。
2. 在播放页点击收藏按钮。
3. 进入“收藏”页，确认故事出现。
4. 再次点击收藏图标可取消收藏。

继续听：

1. 播放故事超过 15 秒，或暂停一次。
2. App 会调用 `/api/devices/{deviceId}/history` 上报进度。
3. 返回首页或进入“继续听”页。
4. 点击记录，应从上次位置继续播放。

## 日志查看

在 Android Studio 的 Logcat 中过滤：

```text
net.lijue.storybox
```

网络请求由 OkHttp 基础日志输出，播放错误会在播放页显示友好提示。
