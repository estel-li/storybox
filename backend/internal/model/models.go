package model

import "time"

type Category struct {
	ID          uint      `gorm:"primaryKey" json:"id"`
	Name        string    `gorm:"not null;uniqueIndex" json:"name"`
	DisplayName string    `gorm:"not null" json:"display_name"`
	SortOrder   int       `gorm:"not null;default:999999" json:"sort_order"`
	IsVisible   bool      `gorm:"not null;default:true;index" json:"is_visible"`
	CreatedAt   time.Time `json:"created_at"`
	UpdatedAt   time.Time `json:"updated_at"`
}

type Album struct {
	ID          uint      `gorm:"primaryKey" json:"id"`
	CategoryID  uint      `gorm:"not null;index;uniqueIndex:idx_album_category_name" json:"category_id"`
	Category    *Category `json:"category,omitempty"`
	Name        string    `gorm:"not null;uniqueIndex:idx_album_category_name" json:"name"`
	DisplayName string    `gorm:"not null" json:"display_name"`
	CoverPath   string    `json:"cover_path"`
	Description string    `json:"description"`
	SortOrder   int       `gorm:"not null;default:999999" json:"sort_order"`
	StoryCount  int64     `gorm:"not null;default:0" json:"story_count"`
	IsVisible   bool      `gorm:"not null;default:true;index" json:"is_visible"`
	CreatedAt   time.Time `json:"created_at"`
	UpdatedAt   time.Time `json:"updated_at"`
}

type Story struct {
	ID              uint      `gorm:"primaryKey" json:"id"`
	CategoryID      uint      `gorm:"not null;index" json:"category_id"`
	Category        *Category `json:"category,omitempty"`
	AlbumID         uint      `gorm:"not null;index" json:"album_id"`
	Album           *Album    `json:"album,omitempty"`
	Chapter         string    `gorm:"index" json:"chapter"`
	Title           string    `gorm:"not null;index" json:"title"`
	DisplayTitle    string    `gorm:"not null" json:"display_title"`
	SortOrder       int       `gorm:"not null;default:999999" json:"sort_order"`
	FilePath        string    `gorm:"not null" json:"-"`
	RelativePath    string    `gorm:"not null;uniqueIndex" json:"relative_path"`
	FileExt         string    `gorm:"not null" json:"file_ext"`
	FileSize        int64     `gorm:"not null;default:0" json:"file_size"`
	DurationSeconds int       `gorm:"not null;default:0" json:"duration_seconds"`
	FileHash        string    `json:"file_hash,omitempty"`
	IsVisible       bool      `gorm:"not null;default:true;index" json:"is_visible"`
	IsMissing       bool      `gorm:"not null;default:false;index" json:"is_missing"`
	CreatedAt       time.Time `json:"created_at"`
	UpdatedAt       time.Time `json:"updated_at"`
}

type ScanJob struct {
	ID           uint       `gorm:"primaryKey" json:"id"`
	StartedAt    time.Time  `json:"started_at"`
	FinishedAt   *time.Time `json:"finished_at"`
	Status       string     `gorm:"not null;index" json:"status"`
	Message      string     `json:"message"`
	TotalFiles   int64      `json:"total_files"`
	AudioFiles   int64      `json:"audio_files"`
	CreatedCount int64      `json:"created_count"`
	UpdatedCount int64      `json:"updated_count"`
	MissingCount int64      `json:"missing_count"`
	DurationMS   int64      `json:"duration_ms"`
	CreatedAt    time.Time  `json:"created_at"`
	UpdatedAt    time.Time  `json:"updated_at"`
}

type PlayHistory struct {
	ID              uint      `gorm:"primaryKey" json:"id"`
	DeviceID        string    `gorm:"not null;index" json:"device_id"`
	StoryID         uint      `gorm:"not null;index" json:"story_id"`
	Story           *Story    `json:"story,omitempty"`
	PositionSeconds int       `gorm:"not null;default:0" json:"position_seconds"`
	DurationSeconds int       `gorm:"not null;default:0" json:"duration_seconds"`
	PlayedAt        time.Time `gorm:"not null;index" json:"played_at"`
	CreatedAt       time.Time `json:"created_at"`
	UpdatedAt       time.Time `json:"updated_at"`
}

type Favorite struct {
	ID        uint      `gorm:"primaryKey" json:"id"`
	DeviceID  string    `gorm:"not null;index;uniqueIndex:idx_favorite_device_story" json:"device_id"`
	StoryID   uint      `gorm:"not null;index;uniqueIndex:idx_favorite_device_story" json:"story_id"`
	Story     *Story    `json:"story,omitempty"`
	CreatedAt time.Time `json:"created_at"`
}

type Setting struct {
	ID        uint      `gorm:"primaryKey" json:"id"`
	Key       string    `gorm:"not null;uniqueIndex" json:"key"`
	Value     string    `json:"value"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}
