package scanner

import (
	"errors"
	"fmt"
	"log/slog"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"time"

	"github.com/estel/storybox-lan/backend/internal/model"
	"github.com/estel/storybox-lan/backend/internal/util"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

type Scanner struct {
	db             *gorm.DB
	root           string
	followSymlinks bool
}

type Result struct {
	TotalFiles   int64 `json:"total_files"`
	AudioFiles   int64 `json:"audio_files"`
	CreatedCount int64 `json:"created_count"`
	UpdatedCount int64 `json:"updated_count"`
	MissingCount int64 `json:"missing_count"`
	DurationMS   int64 `json:"duration_ms"`
}

var audioExtensions = map[string]bool{
	".mp3":  true,
	".m4a":  true,
	".aac":  true,
	".wav":  true,
	".flac": true,
	".ogg":  true,
}

func New(db *gorm.DB, root string, followSymlinks bool) *Scanner {
	return &Scanner{db: db, root: root, followSymlinks: followSymlinks}
}

func IsAudioFile(path string) bool {
	return audioExtensions[strings.ToLower(filepath.Ext(path))]
}

func (s *Scanner) Run() (Result, error) {
	start := time.Now()
	job := model.ScanJob{StartedAt: start, Status: "running"}
	if err := s.db.Create(&job).Error; err != nil {
		return Result{}, err
	}

	slog.Info("scan started", "root", s.root)
	result, err := s.scan(start)
	now := time.Now()
	job.FinishedAt = &now
	job.DurationMS = now.Sub(start).Milliseconds()
	result.DurationMS = job.DurationMS
	job.TotalFiles = result.TotalFiles
	job.AudioFiles = result.AudioFiles
	job.CreatedCount = result.CreatedCount
	job.UpdatedCount = result.UpdatedCount
	job.MissingCount = result.MissingCount

	if err != nil {
		job.Status = "failed"
		job.Message = err.Error()
		_ = s.db.Save(&job).Error
		slog.Error("scan failed", "error", err)
		return result, err
	}

	job.Status = "success"
	job.Message = "scan completed"
	if saveErr := s.db.Save(&job).Error; saveErr != nil {
		return result, saveErr
	}

	slog.Info("scan completed",
		"total_files", result.TotalFiles,
		"audio_files", result.AudioFiles,
		"created", result.CreatedCount,
		"updated", result.UpdatedCount,
		"missing", result.MissingCount,
		"duration_ms", result.DurationMS,
	)
	return result, nil
}

func (s *Scanner) scan(start time.Time) (Result, error) {
	info, err := os.Stat(s.root)
	if err != nil {
		return Result{}, fmt.Errorf("library root unavailable: %w", err)
	}
	if !info.IsDir() {
		return Result{}, errors.New("library root is not a directory")
	}

	var result Result
	seenStories := make(map[string]struct{})
	categoryCache := make(map[string]model.Category)
	albumCache := make(map[string]model.Album)

	err = walk(s.root, s.followSymlinks, func(path string, info os.FileInfo) error {
		if info.IsDir() {
			return nil
		}
		result.TotalFiles++
		if !IsAudioFile(path) {
			return nil
		}
		result.AudioFiles++

		relativePath, err := filepath.Rel(s.root, path)
		if err != nil {
			return err
		}
		relativePath = util.CleanRelativePath(relativePath)
		parts := strings.Split(relativePath, "/")
		if len(parts) < 3 {
			slog.Warn("skip audio file outside category/album structure", "path", relativePath)
			return nil
		}

		category, err := s.findOrCreateCategory(parts[0], categoryCache)
		if err != nil {
			return err
		}
		categoryCache[parts[0]] = category

		albumKey := fmt.Sprintf("%d:%s", category.ID, parts[1])
		album, err := s.findOrCreateAlbum(category.ID, parts[1], albumCache)
		if err != nil {
			return err
		}
		albumCache[albumKey] = album

		chapter := ""
		if len(parts) > 3 {
			chapter = strings.Join(parts[2:len(parts)-1], "/")
		}

		sortOrder, title := util.ParseFileName(parts[len(parts)-1])
		fileExt := strings.TrimPrefix(strings.ToLower(filepath.Ext(path)), ".")
		fileHash := fmt.Sprintf("%s:%d:%d", relativePath, info.Size(), info.ModTime().Unix())

		var story model.Story
		err = s.db.Where("relative_path = ?", relativePath).First(&story).Error
		switch {
		case errors.Is(err, gorm.ErrRecordNotFound):
			story = model.Story{
				CategoryID:      category.ID,
				AlbumID:         album.ID,
				Chapter:         chapter,
				Title:           title,
				DisplayTitle:    title,
				SortOrder:       sortOrder,
				FilePath:        path,
				RelativePath:    relativePath,
				FileExt:         fileExt,
				FileSize:        info.Size(),
				DurationSeconds: 0,
				FileHash:        fileHash,
				IsVisible:       true,
				IsMissing:       false,
			}
			if err := s.db.Create(&story).Error; err != nil {
				return err
			}
			result.CreatedCount++
		case err != nil:
			return err
		default:
			updates := map[string]any{}
			if story.CategoryID != category.ID {
				updates["category_id"] = category.ID
			}
			if story.AlbumID != album.ID {
				updates["album_id"] = album.ID
			}
			if story.Chapter != chapter {
				updates["chapter"] = chapter
			}
			if story.Title != title {
				updates["title"] = title
			}
			if story.SortOrder != sortOrder {
				updates["sort_order"] = sortOrder
			}
			if story.FilePath != path {
				updates["file_path"] = path
			}
			if story.FileExt != fileExt {
				updates["file_ext"] = fileExt
			}
			if story.FileSize != info.Size() {
				updates["file_size"] = info.Size()
			}
			if story.FileHash != fileHash {
				updates["file_hash"] = fileHash
			}
			if story.IsMissing {
				updates["is_missing"] = false
			}
			if strings.TrimSpace(story.DisplayTitle) == "" {
				updates["display_title"] = title
			}
			if len(updates) > 0 {
				if err := s.db.Model(&story).Updates(updates).Error; err != nil {
					return err
				}
				result.UpdatedCount++
			}
		}
		seenStories[relativePath] = struct{}{}
		return nil
	})
	if err != nil {
		return result, err
	}

	if err := s.markMissing(seenStories, &result); err != nil {
		return result, err
	}
	if err := s.refreshAlbumStoryCounts(); err != nil {
		return result, err
	}

	result.DurationMS = time.Since(start).Milliseconds()
	return result, nil
}

func (s *Scanner) findOrCreateCategory(name string, cache map[string]model.Category) (model.Category, error) {
	if category, ok := cache[name]; ok {
		return category, nil
	}
	sortOrder, displayName := util.ParseNamePrefix(name)
	category := model.Category{Name: name}
	err := s.db.Where("name = ?", name).First(&category).Error
	switch {
	case errors.Is(err, gorm.ErrRecordNotFound):
		category.DisplayName = displayName
		category.SortOrder = sortOrder
		category.IsVisible = true
		err = s.db.Create(&category).Error
	case err == nil:
	default:
		return category, err
	}
	return category, err
}

func (s *Scanner) findOrCreateAlbum(categoryID uint, name string, cache map[string]model.Album) (model.Album, error) {
	key := fmt.Sprintf("%d:%s", categoryID, name)
	if album, ok := cache[key]; ok {
		return album, nil
	}
	sortOrder, _ := util.ParseNamePrefix(name)
	album := model.Album{Name: name, CategoryID: categoryID}
	err := s.db.Where("category_id = ? AND name = ?", categoryID, name).First(&album).Error
	switch {
	case errors.Is(err, gorm.ErrRecordNotFound):
		album.DisplayName = name
		album.SortOrder = sortOrder
		album.IsVisible = true
		err = s.db.Create(&album).Error
	case err == nil:
	default:
		return album, err
	}
	return album, err
}

func (s *Scanner) markMissing(seen map[string]struct{}, result *Result) error {
	var stories []model.Story
	if err := s.db.Where("is_missing = ?", false).Find(&stories).Error; err != nil {
		return err
	}
	for _, story := range stories {
		if _, ok := seen[story.RelativePath]; ok {
			continue
		}
		if err := s.db.Model(&story).Update("is_missing", true).Error; err != nil {
			return err
		}
		result.MissingCount++
	}
	return nil
}

func (s *Scanner) refreshAlbumStoryCounts() error {
	var albums []model.Album
	if err := s.db.Find(&albums).Error; err != nil {
		return err
	}
	for _, album := range albums {
		var count int64
		if err := s.db.Model(&model.Story{}).
			Where("album_id = ? AND is_missing = ?", album.ID, false).
			Count(&count).Error; err != nil {
			return err
		}
		if err := s.db.Model(&album).Update("story_count", count).Error; err != nil {
			return err
		}
	}
	return nil
}

type walkFunc func(path string, info os.FileInfo) error

func walk(root string, followSymlinks bool, fn walkFunc) error {
	visited := map[string]struct{}{}
	var walkDir func(string) error

	walkDir = func(dir string) error {
		realDir := dir
		if followSymlinks {
			if evaluated, err := filepath.EvalSymlinks(dir); err == nil {
				realDir = evaluated
			}
		}
		if _, ok := visited[realDir]; ok {
			return nil
		}
		visited[realDir] = struct{}{}

		entries, err := os.ReadDir(dir)
		if err != nil {
			return err
		}
		sort.Slice(entries, func(i, j int) bool { return entries[i].Name() < entries[j].Name() })

		for _, entry := range entries {
			path := filepath.Join(dir, entry.Name())
			info, err := entry.Info()
			if err != nil {
				return err
			}
			if entry.Type()&os.ModeSymlink != 0 && followSymlinks {
				info, err = os.Stat(path)
				if err != nil {
					slog.Warn("skip broken symlink", "path", path, "error", err)
					continue
				}
			}
			if err := fn(path, info); err != nil {
				return err
			}
			if info.IsDir() {
				if err := walkDir(path); err != nil {
					return err
				}
			}
		}
		return nil
	}

	rootInfo, err := os.Stat(root)
	if err != nil {
		return err
	}
	if err := fn(root, rootInfo); err != nil {
		return err
	}
	return walkDir(root)
}

func UpsertFavorite(db *gorm.DB, favorite model.Favorite) error {
	return db.Clauses(clause.OnConflict{DoNothing: true}).Create(&favorite).Error
}
