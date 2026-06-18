# 后端

Go 后端负责扫描故事库、维护 SQLite 数据库、提供管理 API、前台 API 和支持 HTTP Range 的音频流。

## 启动

```bash
cp .env.example .env
go mod tidy
go run ./cmd/server
```

默认地址：`http://localhost:8080`。

## 环境变量

```env
STORY_SERVER_PORT=8080
STORY_DB_PATH=./storage/storybox.db
STORY_LIBRARY_ROOT=/volume3/16T/儿童故事/01_library_故事库
STORY_ADMIN_PASSWORD=admin123456
STORY_JWT_SECRET=please-change-me
STORY_FOLLOW_SYMLINKS=true
STORY_CORS_ORIGINS=http://localhost:3000
```

`.env` 不存在时会使用默认值。SQLite 文件默认创建在 `backend/storage/storybox.db`。

## 常用命令

```bash
go test ./...
go run ./cmd/server
```
