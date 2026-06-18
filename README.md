# 老李讲故事

老李讲故事是一个家庭局域网儿童故事播放系统 MVP。它会扫描 NAS 上已经整理好的故事音频目录，把分类、专辑、章节和故事写入 SQLite，并提供 Web 管理后台和简单儿童播放端。

## 技术栈

- 后端：Go、Gin、GORM、SQLite（纯 Go 驱动 `github.com/glebarez/sqlite`，不需要 CGO）、slog、godotenv、JWT
- 前端：Nuxt 4、TypeScript、Nuxt UI、Pinia
- Android：Kotlin、Jetpack Compose、Material 3、Retrofit、DataStore、Media3
- 运行方式：本地运行，或 Docker 单镜像部署

## 目录结构

```text
老李讲故事/
├── backend/          Go 后端
├── web/              Nuxt 4 Web 端
├── android/          Android 原生客户端
├── docs/             API、数据库、扫描规则和 MVP 文档
├── scripts/          本地开发脚本
└── README.md
```

## 启动后端

```bash
cd backend
cp .env.example .env
go mod tidy
go run ./cmd/server
```

后端默认监听 `http://localhost:8080`。

## 编译 NAS/Linux 后端

后端 SQLite 使用纯 Go 驱动 `github.com/glebarez/sqlite`，不依赖 `github.com/mattn/go-sqlite3`，因此不需要开启 CGO。可以直接在 Mac 上交叉编译 Linux 二进制：

```bash
cd backend
CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o server-linux ./cmd/server
```

## Docker 打包

项目提供一个单镜像 Docker 打包方式：镜像里同时包含 Go 后端和 Nuxt Web 端。容器默认启动：

- Go 后端：`0.0.0.0:8080`
- Nuxt Web：`0.0.0.0:3000`
- Web 端默认通过同源 `/api` 代理到容器内 Go 后端，访问 `http://NAS_IP:3000` 即可使用 Web 管理后台和播放端。

本地打包：

```bash
./scripts/docker-build.sh
```

运行示例：

```bash
docker run -d --name storybox-lan \
  -p 3000:3000 \
  -p 8080:8080 \
  -v storybox-data:/data \
  -v /volume3/16T/儿童故事/01_library_故事库:/library:ro \
  -e STORY_DB_PATH=/data/storybox.db \
  -e STORY_LIBRARY_ROOT=/library \
  -e STORY_ADMIN_PASSWORD=admin123456 \
  -e STORY_JWT_SECRET=please-change-this-secret \
  storybox-lan:local
```

GitHub Actions 工作流位于 `.github/workflows/docker.yml`。推送到 `main` / `master` 或 `v*` 标签时，会构建并发布多架构镜像到 GitHub Container Registry：

```text
ghcr.io/<github-owner>/storybox-lan
```

## 启动 Web

```bash
cd web
cp .env.example .env
pnpm install
pnpm dev
```

Web 默认监听 `http://localhost:3000`。

## 启动 Android

用 Android Studio 打开 `android/` 目录，等待 Gradle Sync 完成后运行到真机。

真机不能使用 `http://localhost:8080` 连接电脑上的后端，需要填写电脑或 NAS 的局域网 IP，例如：

```text
http://192.168.1.10:8080
```

## 配置 NAS 故事库路径

编辑 `backend/.env`：

```env
STORY_LIBRARY_ROOT=/volume3/16T/儿童故事/01_library_故事库
```

如果在本机测试，可以改成本地测试目录。

## 第一次扫描

1. 启动后端。
2. 启动 Web。
3. 打开 `http://localhost:3000/admin/login`。
4. 使用 `STORY_ADMIN_PASSWORD` 登录，默认是 `admin123456`。
5. 进入“扫描管理”，点击“开始扫描”。

也可以直接调用 API：

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{"password":"admin123456"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

curl -X POST http://localhost:8080/api/admin/scan \
  -H "Authorization: Bearer $TOKEN"
```

## 测试音频播放

扫描完成后打开：

- 后台：`http://localhost:3000/admin`
- 播放端：`http://localhost:3000/player`
- Android：首次启动后填写后端局域网地址

Range 播放测试：

```bash
curl -I -H "Range: bytes=0-1023" http://localhost:8080/api/stories/1/stream
```

正常情况下应返回 `206 Partial Content` 和 `Content-Range`。

## 修改后台密码

编辑 `backend/.env`：

```env
STORY_ADMIN_PASSWORD=your-new-password
STORY_JWT_SECRET=please-change-this-secret
```

改完后重启后端。
