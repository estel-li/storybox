# API

默认后端地址：`http://localhost:8080`。

## 健康检查

```http
GET /api/health
```

## 管理登录

```http
POST /api/admin/login
Content-Type: application/json

{"password":"admin123456"}
```

返回：

```json
{"token":"jwt-token"}
```

除登录外，所有 `/api/admin/*` 接口需要：

```http
Authorization: Bearer jwt-token
```

## 管理接口

```http
POST /api/admin/scan
GET  /api/admin/scan/jobs
GET  /api/admin/stats
GET  /api/admin/categories
PUT  /api/admin/categories/:id
GET  /api/admin/albums?category_id=&keyword=&page=&page_size=
PUT  /api/admin/albums/:id
GET  /api/admin/stories?category_id=&album_id=&keyword=&page=&page_size=
PUT  /api/admin/stories/:id
GET  /api/admin/history?page=&page_size=
```

## 前台接口

```http
GET /api/categories
GET /api/categories/:id/albums
GET /api/albums
GET /api/albums/:id
GET /api/albums/:id/stories
GET /api/stories/:id
GET /api/search?q=关键词
GET /api/stories/:id/stream
```

前台接口只返回可见且未缺失的数据。

## 播放记录和收藏

```http
GET    /api/devices/:deviceId/history
POST   /api/devices/:deviceId/history
GET    /api/devices/:deviceId/favorites
POST   /api/devices/:deviceId/favorites/:storyId
DELETE /api/devices/:deviceId/favorites/:storyId
```

播放记录请求体：

```json
{
  "story_id": 1,
  "position_seconds": 120,
  "duration_seconds": 600
}
```

## Range 测试

```bash
curl -I -H "Range: bytes=0-1023" http://localhost:8080/api/stories/1/stream
```
