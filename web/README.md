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
NUXT_PUBLIC_API_BASE=http://localhost:8080
```

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
