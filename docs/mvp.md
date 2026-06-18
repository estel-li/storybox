# MVP

本 MVP 跑通这条链路：

```text
扫描 NAS 故事目录 -> 写入 SQLite -> Web 后台查看和管理 -> Web 端播放音频
```

## 已包含

- Go 后端
- Gin API
- GORM AutoMigrate
- SQLite 存储
- 故事库递归扫描
- 分类、专辑、故事管理
- 后台 token 登录
- 支持 HTTP Range 的音频流
- 播放记录
- 收藏
- Nuxt 4 Web 管理后台
- 简单儿童播放端

## 暂不包含

- Docker
- Android
- iOS
- 音频转码
- 复杂账号系统
- 音频时长解析
