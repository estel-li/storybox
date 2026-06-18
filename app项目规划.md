你是一个资深 Android 工程师，请在现有 monorepo 项目中新增 Android 客户端。

项目背景：

这是一个局域网儿童故事播放系统，后端是 Go + Gin + GORM + SQLite，Web 管理端是 Nuxt 4。现在需要新增 Android 客户端，用于孩子在 Android 手机上听 NAS 中的儿童故事。

请直接生成完整 Android 项目代码，不要只写方案。

# 一、项目位置

现有项目结构大致如下：

```text
老李讲故事/
├── backend/
├── web/
├── docs/
└── README.md
```

请新增：

```text
老李讲故事/
└── android/
    ├── app/
    ├── build.gradle.kts
    ├── settings.gradle.kts
    └── README.md
```

Android 项目可以独立用 Android Studio 打开，也可以作为 monorepo 的子项目存在。

# 二、Android 技术栈

请使用：

* Kotlin
* Jetpack Compose
* Material 3
* AndroidX Navigation Compose
* ViewModel
* Kotlin Coroutines
* Flow / StateFlow
* Retrofit
* OkHttp
* Kotlin Serialization 或 Moshi，优先 Kotlin Serialization
* DataStore Preferences
* Media3 / ExoPlayer
* MediaSession
* Foreground playback service
* Coil Compose，用于后续封面扩展
* Gradle Kotlin DSL

不要使用 Flutter。

不要使用 React Native。

不要使用 WebView 套壳。

# 三、最低版本要求

建议：

```text
minSdk = 26
targetSdk 使用当前 Android Gradle Plugin 支持的稳定版本
compileSdk 使用当前 Android Gradle Plugin 支持的稳定版本
```

如果具体版本不确定，请使用项目当前 Android Studio / Gradle 推荐的稳定版本。

# 四、应用定位

App 名称：

```text
家庭故事盒
```

英文包名建议：

```text
net.lijue.storybox
```

这是一个局域网儿童故事播放器。

第一版只需要支持：

```text
配置服务器地址
连接 Go 后端
浏览分类
浏览专辑
浏览故事
搜索故事
播放故事
后台播放
锁屏控制
上一首/下一首
断点续播
收藏
继续听
睡眠定时
```

第一版不需要：

```text
用户登录
儿童账号
公网访问
下载离线缓存
视频播放
评论
会员
付费
推荐算法
复杂动画
```

# 五、后端 API 约定

后端默认地址由用户配置，例如：

```text
http://192.168.1.2:8080
或者 https://nas.lijue.net:8001
```

Android 首次启动时进入服务器配置页。

需要调用以下 API。

## 健康检查

```http
GET /api/health
```

返回示例：

```json
{
  "ok": true,
  "version": "0.1.0"
}
```

## 分类

```http
GET /api/categories
```

返回分类列表。

字段可能包括：

```json
{
  "id": 1,
  "name": "01_经典名著",
  "display_name": "经典名著",
  "sort_order": 1,
  "is_visible": true
}
```

## 分类下专辑

```http
GET /api/categories/{id}/albums
```

## 所有专辑

```http
GET /api/albums
```

## 专辑详情

```http
GET /api/albums/{id}
```

## 专辑下故事

```http
GET /api/albums/{id}/stories
```

故事列表字段可能包括：

```json
{
  "id": 1,
  "category_id": 1,
  "album_id": 1,
  "chapter": "001群雄逐鹿",
  "title": "英雄问世三结义1",
  "display_title": "英雄问世三结义1",
  "sort_order": 1,
  "file_ext": ".mp3",
  "file_size": 12345678,
  "duration_seconds": 0,
  "relative_path": "01_经典名著/三国演义（完结）/001群雄逐鹿/001.英雄问世三结义1.mp3",
  "is_visible": true
}
```

## 故事详情

```http
GET /api/stories/{id}
```

## 音频播放

```http
GET /api/stories/{id}/stream
```

Android 播放时直接把完整 URL 交给 Media3：

```text
{serverBaseUrl}/api/stories/{id}/stream
```

后端已支持 HTTP Range，Android 端应支持拖动进度。

## 搜索

```http
GET /api/search?q=关键词
```

## 播放历史

```http
GET /api/devices/{deviceId}/history
POST /api/devices/{deviceId}/history
```

POST 请求：

```json
{
  "story_id": 1,
  "position_seconds": 120,
  "duration_seconds": 600
}
```

## 收藏

```http
GET /api/devices/{deviceId}/favorites
POST /api/devices/{deviceId}/favorites/{storyId}
DELETE /api/devices/{deviceId}/favorites/{storyId}
```

# 六、本地持久化要求

使用 DataStore 保存：

```text
server_base_url
device_id
last_playing_story_id
last_album_id
last_position_seconds
sleep_timer_mode
```

`device_id` 规则：

* App 首次启动生成 UUID。
* 保存在 DataStore。
* 后续播放记录、收藏都使用这个 deviceId。
* 不做账号登录。

# 七、App 页面结构

请使用 Jetpack Compose + Navigation Compose。

页面包括：

```text
Splash / 启动检查页
ServerSetup / 服务器配置页
Home / 首页
CategoryList / 分类页
AlbumList / 专辑列表页
AlbumDetail / 专辑详情页
Search / 搜索页
Favorites / 收藏页
History / 继续听页
Player / 播放页
Settings / 设置页
```

# 八、页面功能详细说明

## 1. 启动页 Splash

逻辑：

* 读取 DataStore 中的 server_base_url。
* 如果没有配置，跳转服务器配置页。
* 如果已配置，调用 `/api/health`。
* 如果连接成功，进入首页。
* 如果连接失败，进入服务器配置页，并提示“服务器连接失败，请检查地址”。

## 2. 服务器配置页

功能：

* 输入服务器地址。
* 默认提示格式：`http://192.168.1.10:8080`
* 点击“测试连接”。
* 调用 `/api/health`。
* 连接成功后保存地址并进入首页。
* 连接失败显示错误。
* 地址末尾如果有 `/`，自动去掉。

UI 要求：

* 大输入框
* 大按钮
* 明确提示“请填写家庭故事盒后端地址”

## 3. 首页

首页展示：

```text
继续听
分类入口
最近专辑 / 推荐专辑
收藏入口
搜索入口
设置入口
```

MVP 首页可以简洁，不需要复杂推荐算法。

继续听：

* 从后端历史接口获取最近播放。
* 如果没有历史，隐藏继续听或显示“还没有播放记录”。

分类入口：

* 调用 `/api/categories`。
* 用大卡片展示分类。

UI 风格：

* 儿童友好
* 大按钮
* 大卡片
* 大字体
* 每屏不要太密集

## 4. 分类页

展示所有分类。

点击分类进入该分类下的专辑列表。

## 5. 专辑列表页

调用：

```http
GET /api/categories/{id}/albums
```

展示：

* 专辑名称
* 故事数量
* 简介，如果有
* 点击进入专辑详情

## 6. 专辑详情页

调用：

```http
GET /api/albums/{id}
GET /api/albums/{id}/stories
```

展示：

* 专辑名称
* 简介
* 播放全部按钮
* 故事列表

故事列表需要按 `chapter` 分组。

例如：

```text
001群雄逐鹿
  001 英雄问世三结义1
  002 英雄问世三结义2

002孙刘联盟
  001 ...
  002 ...
```

点击故事：

* 创建当前专辑播放队列。
* 从点击的故事开始播放。
* 跳转播放页。

## 7. 搜索页

功能：

* 输入关键词
* 调用 `/api/search?q=关键词`
* 展示故事结果
* 点击故事播放
* 可以显示专辑名、章节名

## 8. 收藏页

调用：

```http
GET /api/devices/{deviceId}/favorites
```

展示收藏故事。

点击播放。

## 9. 继续听页

调用：

```http
GET /api/devices/{deviceId}/history
```

展示最近播放记录：

* 故事标题
* 专辑名
* 播放进度
* 最近播放时间

点击后从上次位置继续播放。

## 10. 播放页

播放页是核心页面。

展示：

```text
故事标题
专辑名称
章节名称
播放/暂停
上一首
下一首
进度条
当前时间 / 总时长
收藏按钮
睡眠定时按钮
播放队列按钮
```

功能：

* 播放当前故事
* 暂停/继续
* 上一首/下一首
* 拖动进度
* 收藏/取消收藏
* 定时关闭
* 播放进度定期上报后端

进度上报策略：

* 每 15 秒上报一次。
* 暂停时上报。
* 切换下一首时上报。
* App 进入后台时尽量上报。
* 播放结束时上报。

## 11. 睡眠定时

提供选项：

```text
关闭
15 分钟
30 分钟
60 分钟
播完当前故事后停止
```

实现要求：

* 使用 ViewModel / Service 状态维护。
* 倒计时结束后暂停播放。
* “播完当前故事后停止”在当前故事播放完成时停止，不自动下一首。

## 12. 设置页

功能：

* 查看当前服务器地址
* 修改服务器地址
* 测试连接
* 查看设备 ID
* 清空本地缓存
* 关于 App

# 九、播放架构要求

使用 Media3。

请实现：

```text
StoryPlaybackService
StoryPlayerManager
PlayerViewModel
PlaybackQueue
```

## 1. 后台播放

需要支持：

* App 切后台继续播放
* 通知栏播放控制
* 锁屏播放控制
* 耳机按钮播放/暂停
* 上一首/下一首

## 2. MediaSession

使用 MediaSession 管理播放状态。

通知栏应显示：

```text
故事标题
专辑名
播放/暂停
上一首
下一首
```

## 3. 播放队列

播放队列来源：

* 专辑详情页点击“播放全部”
* 专辑详情页点击某一集
* 搜索结果点击单集
* 收藏页点击单集
* 继续听点击单集

队列逻辑：

* 专辑播放时，队列为该专辑所有故事。
* 搜索/收藏/历史点击单集时，可以只播放单集，或者尝试加载同专辑列表作为队列。
* MVP 优先保证稳定，可以先单集播放，但专辑详情必须支持上一首/下一首。

## 4. URL 构建

音频 URL：

```kotlin
"$serverBaseUrl/api/stories/${story.id}/stream"
```

注意：

* serverBaseUrl 末尾不要保留 `/`
* story id 必须来自后端
* 不要直接使用 NAS 文件路径

# 十、数据模型

请定义 Kotlin data class。

示例：

```kotlin
data class CategoryDto(
    val id: Long,
    val name: String,
    val displayName: String,
    val sortOrder: Int,
    val isVisible: Boolean
)
```

注意后端可能返回 snake_case 字段，例如：

```json
display_name
sort_order
is_visible
```

请用 `@SerialName` 映射。

需要模型：

```text
HealthResponse
LoginResponse 可不实现
CategoryDto
AlbumDto
StoryDto
SearchResultDto
PlayHistoryDto
FavoriteDto
UpdateHistoryRequest
ApiPageResponse 可选
```

# 十一、网络层

请实现：

```text
ApiClient
StoryBoxApi
ApiRepository
```

Retrofit 接口包括：

```kotlin
@GET("api/health")
suspend fun health(): HealthResponse

@GET("api/categories")
suspend fun getCategories(): List<CategoryDto>

@GET("api/categories/{id}/albums")
suspend fun getAlbumsByCategory(@Path("id") id: Long): List<AlbumDto>

@GET("api/albums/{id}")
suspend fun getAlbum(@Path("id") id: Long): AlbumDto

@GET("api/albums/{id}/stories")
suspend fun getStoriesByAlbum(@Path("id") id: Long): List<StoryDto>

@GET("api/stories/{id}")
suspend fun getStory(@Path("id") id: Long): StoryDto

@GET("api/search")
suspend fun search(@Query("q") keyword: String): List<StoryDto>

@GET("api/devices/{deviceId}/history")
suspend fun getHistory(@Path("deviceId") deviceId: String): List<PlayHistoryDto>

@POST("api/devices/{deviceId}/history")
suspend fun updateHistory(
    @Path("deviceId") deviceId: String,
    @Body body: UpdateHistoryRequest
)

@GET("api/devices/{deviceId}/favorites")
suspend fun getFavorites(@Path("deviceId") deviceId: String): List<StoryDto>

@POST("api/devices/{deviceId}/favorites/{storyId}")
suspend fun addFavorite(
    @Path("deviceId") deviceId: String,
    @Path("storyId") storyId: Long
)

@DELETE("api/devices/{deviceId}/favorites/{storyId}")
suspend fun removeFavorite(
    @Path("deviceId") deviceId: String,
    @Path("storyId") storyId: Long
)
```

如果后端实际返回包装结构，请在 Repository 层适配。

# 十二、状态管理

每个页面使用 ViewModel。

建议：

```text
SplashViewModel
ServerSetupViewModel
HomeViewModel
CategoryViewModel
AlbumListViewModel
AlbumDetailViewModel
SearchViewModel
FavoritesViewModel
HistoryViewModel
PlayerViewModel
SettingsViewModel
```

UI 状态统一使用：

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

或者使用更适合 Compose 的 data class 状态。

# 十三、错误处理

必须处理：

```text
服务器地址未配置
服务器连接失败
接口返回错误
音频文件不存在
播放失败
网络中断
收藏失败
播放记录上传失败
```

错误提示要友好，不要直接暴露异常堆栈。

示例：

```text
无法连接家庭故事盒，请检查手机和 NAS 是否在同一局域网。
故事文件暂时无法播放，请稍后再试。
服务器地址格式不正确。
```

# 十四、UI 设计要求

整体风格：

```text
儿童友好
大卡片
大按钮
大字体
圆角
少层级
少输入
少干扰
```

不要做复杂商业 App 风格。

推荐主色可以使用柔和蓝色、橙色、绿色，但请通过 MaterialTheme 统一管理。

页面组件建议：

```text
StoryBoxScaffold
CategoryCard
AlbumCard
StoryListItem
PlayerBottomBar
LargePlayButton
EmptyState
ErrorState
LoadingState
```

# 十五、权限要求

需要在 AndroidManifest 中配置：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

注意：

* Android 13 及以上需要请求通知权限。
* 如果通知权限未授权，播放功能仍应尽量可用，但通知可能不显示。

# 十六、项目质量要求

请注意：

* 代码必须有清晰中文注释。
* 包结构清晰。
* 不要把所有逻辑写在 Activity 里。
* 网络层、数据层、播放层、UI 层分离。
* Compose 页面要可读。
* ViewModel 不直接持有 Context，除非使用 AndroidViewModel 或明确需要。
* DataStore 统一封装。
* API Base URL 支持运行时修改。
* 修改服务器地址后，需要重新创建 API Client 或 Repository。
* 播放器资源需要正确释放。
* App 退出时不要造成后台服务泄露。

# 十七、建议包结构

请按类似结构实现：

```text
net.lijue.storybox
├── MainActivity.kt
├── StoryBoxApp.kt
├── core/
│   ├── network/
│   ├── datastore/
│   ├── model/
│   ├── common/
│   └── util/
├── data/
│   ├── api/
│   ├── repository/
│   └── mapper/
├── playback/
│   ├── StoryPlaybackService.kt
│   ├── StoryPlayerManager.kt
│   ├── PlaybackQueue.kt
│   └── PlaybackState.kt
├── ui/
│   ├── theme/
│   ├── navigation/
│   ├── components/
│   └── screen/
│       ├── splash/
│       ├── setup/
│       ├── home/
│       ├── category/
│       ├── album/
│       ├── search/
│       ├── favorites/
│       ├── history/
│       ├── player/
│       └── settings/
└── feature 可选
```

# 十八、构建与运行

请提供 Android README，说明：

```text
如何用 Android Studio 打开
如何配置后端地址
如何运行到真机
如何测试局域网连接
如何查看日志
```

需要说明：

手机和 NAS 必须在同一局域网。

如果后端在电脑本机运行，真机不能使用：

```text
http://localhost:8080
```

需要使用电脑或 NAS 的局域网 IP，例如：

```text
http://192.168.1.10:8080
```

# 十九、验收标准

完成后必须满足：

1. Android 项目可以编译。
2. App 首次启动进入服务器配置页。
3. 输入后端地址后可以测试连接。
4. 连接成功后进入首页。
5. 首页可以展示分类。
6. 分类页可以进入专辑列表。
7. 专辑页可以展示章节分组和故事列表。
8. 点击故事可以播放。
9. 播放接口使用 `/api/stories/{id}/stream`。
10. 支持播放、暂停、上一首、下一首。
11. 支持拖动进度。
12. 支持后台播放。
13. 支持锁屏控制。
14. 支持收藏和取消收藏。
15. 支持继续听。
16. 支持搜索。
17. 支持睡眠定时。
18. 播放进度可以上报到后端。
19. 服务器不可达时有友好提示。
20. 修改服务器地址后可以重新连接。

# 二十、请最终输出

请直接生成完整 Android 客户端代码。

完成后请说明：

* Android Studio 如何打开项目
* 如何运行到真机
* 后端地址在哪里配置
* 首次启动流程
* 主要页面说明
* 播放服务如何工作
* 如何测试后台播放
* 如何测试锁屏控制
* 如何测试收藏和继续听
