# 数据库

数据库使用 SQLite 和 GORM AutoMigrate，默认文件为 `backend/storage/storybox.db`。

## categories

- `id`
- `name`
- `display_name`
- `sort_order`
- `is_visible`
- `created_at`
- `updated_at`

## albums

- `id`
- `category_id`
- `name`
- `display_name`
- `cover_path`
- `description`
- `sort_order`
- `story_count`
- `is_visible`
- `created_at`
- `updated_at`

## stories

- `id`
- `category_id`
- `album_id`
- `chapter`
- `title`
- `display_title`
- `sort_order`
- `file_path`
- `relative_path`
- `file_ext`
- `file_size`
- `duration_seconds`
- `file_hash`
- `is_visible`
- `is_missing`
- `created_at`
- `updated_at`

`file_path` 只在后端内部使用，API 响应不会直接返回真实文件路径。

## scan_jobs

记录每次扫描的开始时间、完成时间、状态、统计和耗时。

## play_history

按 `device_id` 保存播放进度。

## favorites

按 `device_id` 和 `story_id` 保存收藏，二者组合唯一。

## settings

预留设置表。
