# Web

Nuxt 4 Web 端包含家长管理后台和儿童播放端。

## 启动

```bash
cp .env.example .env
pnpm install
pnpm dev
```

默认地址：`http://localhost:3000`。

## 环境变量

```env
NUXT_PUBLIC_API_BASE=
NUXT_API_PROXY_TARGET=http://127.0.0.1:8080
```

默认情况下 Web 端使用同源 `/api`，由 Nuxt/Nitro 代理到 `NUXT_API_PROXY_TARGET`。如果 Web 和后端分开部署，可以把 `NUXT_PUBLIC_API_BASE` 设置成后端完整地址。

## 页面

- `/`：跳转到 `/player`
- `/admin/login`：后台登录
- `/admin`：仪表盘
- `/admin/scan`：扫描管理
- `/admin/categories`：分类管理
- `/admin/albums`：专辑管理
- `/admin/stories`：故事管理
- `/admin/history`：播放记录
- `/player`：儿童播放端
