export interface Category {
  id: number
  name: string
  display_name: string
  sort_order: number
  is_visible: boolean
  created_at: string
  updated_at: string
}

export interface Album {
  id: number
  category_id: number
  category?: Category
  name: string
  display_name: string
  cover_path: string
  description: string
  sort_order: number
  story_count: number
  is_visible: boolean
  created_at: string
  updated_at: string
}

export interface Story {
  id: number
  category_id: number
  category?: Category
  album_id: number
  album?: Album
  chapter: string
  title: string
  display_title: string
  sort_order: number
  relative_path: string
  file_ext: string
  file_size: number
  duration_seconds: number
  is_visible: boolean
  is_missing: boolean
  created_at: string
  updated_at: string
}

export interface ScanResult {
  total_files: number
  audio_files: number
  created_count: number
  updated_count: number
  missing_count: number
  duration_ms: number
}

export interface ScanJob extends ScanResult {
  id: number
  started_at: string
  finished_at?: string
  status: string
  message: string
  created_at: string
  updated_at: string
}

export interface AdminStats {
  category_count: number
  album_count: number
  story_count: number
  visible_story_count: number
  missing_story_count: number
  last_scan_time: string
  library_root: string
}

export interface PlayHistory {
  id: number
  device_id: string
  story_id: number
  story?: Story
  position_seconds: number
  duration_seconds: number
  played_at: string
  created_at: string
  updated_at: string
}

export interface Favorite {
  id: number
  device_id: string
  story_id: number
  story?: Story
  created_at: string
}

export interface ListResponse<T> {
  items: T[]
  total?: number
  page?: number
  page_size?: number
}
