# 老李讲故事

老李讲故事是一个家庭局域网儿童故事播放系统 MVP。它会扫描 NAS 上已经整理好的故事音频目录，把分类、专辑、章节和故事写入 SQLite，并提供 Web 管理后台和简单儿童播放端。

## 技术栈

- 后端：Go、Gin、GORM、SQLite、slog、godotenv、JWT
- 前端：Nuxt 4、TypeScript、Nuxt UI、Pinia
- 运行方式：本地运行，不使用 Docker

## 目录结构

```text
storybox-lan/
├── backend/          Go 后端
├── web/              Nuxt 4 Web 端
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

## 启动 Web

```bash
cd web
cp .env.example .env
pnpm install
pnpm dev
```

Web 默认监听 `http://localhost:3000`。

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
