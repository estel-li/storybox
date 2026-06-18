package handler

import (
	"errors"
	"log/slog"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/estel/storybox-lan/backend/internal/config"
	"github.com/estel/storybox-lan/backend/internal/middleware"
	"github.com/estel/storybox-lan/backend/internal/model"
	"github.com/estel/storybox-lan/backend/internal/scanner"
	"github.com/estel/storybox-lan/backend/internal/service"
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

type Handler struct {
	db      *gorm.DB
	cfg     config.Config
	auth    *service.AuthService
	scanner *scanner.Scanner
}

func New(db *gorm.DB, cfg config.Config) *Handler {
	auth := service.NewAuthService(cfg.AdminPassword, cfg.JWTSecret)
	return &Handler{
		db:      db,
		cfg:     cfg,
		auth:    auth,
		scanner: scanner.New(db, cfg.LibraryRoot, cfg.FollowSymlinks),
	}
}

func (h *Handler) Register(router *gin.Engine) {
	api := router.Group("/api")
	api.GET("/health", h.health)

	api.POST("/admin/login", h.login)
	admin := api.Group("/admin", middleware.AdminAuth(h.auth))
	admin.POST("/scan", h.scan)
	admin.GET("/scan/jobs", h.scanJobs)
	admin.GET("/stats", h.stats)
	admin.GET("/categories", h.adminCategories)
	admin.PUT("/categories/:id", h.updateCategory)
	admin.GET("/albums", h.adminAlbums)
	admin.PUT("/albums/:id", h.updateAlbum)
	admin.GET("/stories", h.adminStories)
	admin.PUT("/stories/:id", h.updateStory)
	admin.GET("/history", h.adminHistory)

	api.GET("/categories", h.publicCategories)
	api.GET("/categories/:id/albums", h.publicCategoryAlbums)
	api.GET("/albums", h.publicAlbums)
	api.GET("/albums/:id", h.publicAlbum)
	api.GET("/albums/:id/stories", h.publicAlbumStories)
	api.GET("/stories/:id", h.publicStory)
	api.GET("/stories/:id/stream", h.streamStory)
	api.GET("/search", h.search)
	api.GET("/devices/:deviceId/history", h.deviceHistory)
	api.POST("/devices/:deviceId/history", h.saveHistory)
	api.GET("/devices/:deviceId/favorites", h.deviceFavorites)
	api.POST("/devices/:deviceId/favorites/:storyId", h.addFavorite)
	api.DELETE("/devices/:deviceId/favorites/:storyId", h.removeFavorite)
}

func (h *Handler) health(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{"ok": true, "version": "0.1.0"})
}

func (h *Handler) login(c *gin.Context) {
	var request struct {
		Password string `json:"password"`
	}
	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request"})
		return
	}
	token, err := h.auth.Login(request.Password)
	if err != nil {
		slog.Warn("login failed")
		c.JSON(http.StatusUnauthorized, gin.H{"error": "invalid password"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"token": token})
}

func (h *Handler) scan(c *gin.Context) {
	result, err := h.scanner.Run()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error(), "result": result})
		return
	}
	c.JSON(http.StatusOK, result)
}

func (h *Handler) scanJobs(c *gin.Context) {
	var jobs []model.ScanJob
	if err := h.db.Order("started_at DESC").Limit(10).Find(&jobs).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"items": jobs})
}

func (h *Handler) stats(c *gin.Context) {
	var categoryCount, albumCount, storyCount, visibleStoryCount, missingStoryCount int64
	h.db.Model(&model.Category{}).Count(&categoryCount)
	h.db.Model(&model.Album{}).Count(&albumCount)
	h.db.Model(&model.Story{}).Count(&storyCount)
	h.db.Model(&model.Story{}).Where("is_visible = ? AND is_missing = ?", true, false).Count(&visibleStoryCount)
	h.db.Model(&model.Story{}).Where("is_missing = ?", true).Count(&missingStoryCount)

	var lastScan model.ScanJob
	lastScanTime := ""
	if err := h.db.Where("status = ?", "success").Order("started_at DESC").First(&lastScan).Error; err == nil {
		lastScanTime = lastScan.StartedAt.UTC().Format(time.RFC3339)
	}

	c.JSON(http.StatusOK, gin.H{
		"category_count":      categoryCount,
		"album_count":         albumCount,
		"story_count":         storyCount,
		"visible_story_count": visibleStoryCount,
		"missing_story_count": missingStoryCount,
		"last_scan_time":      lastScanTime,
		"library_root":        h.cfg.LibraryRoot,
	})
}

func (h *Handler) adminCategories(c *gin.Context) {
	var categories []model.Category
	if err := h.db.Order("sort_order ASC, name ASC").Find(&categories).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"items": categories})
}

func (h *Handler) updateCategory(c *gin.Context) {
	var category model.Category
	if !h.findByID(c, &category) {
		return
	}
	var request struct {
		DisplayName *string `json:"display_name"`
		SortOrder   *int    `json:"sort_order"`
		IsVisible   *bool   `json:"is_visible"`
	}
	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request"})
		return
	}
	updates := map[string]any{}
	if request.DisplayName != nil {
		updates["display_name"] = strings.TrimSpace(*request.DisplayName)
	}
	if request.SortOrder != nil {
		updates["sort_order"] = *request.SortOrder
	}
	if request.IsVisible != nil {
		updates["is_visible"] = *request.IsVisible
	}
	if err := h.db.Model(&category).Updates(updates).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	h.db.First(&category, category.ID)
	c.JSON(http.StatusOK, category)
}

func (h *Handler) adminAlbums(c *gin.Context) {
	query := h.db.Model(&model.Album{}).Preload("Category")
	if categoryID := c.Query("category_id"); categoryID != "" {
		query = query.Where("category_id = ?", categoryID)
	}
	if keyword := strings.TrimSpace(c.Query("keyword")); keyword != "" {
		like := "%" + keyword + "%"
		query = query.Where("name LIKE ? OR display_name LIKE ? OR description LIKE ?", like, like, like)
	}
	h.paginated(c, query, &[]model.Album{}, "sort_order ASC, name ASC")
}

func (h *Handler) updateAlbum(c *gin.Context) {
	var album model.Album
	if !h.findByID(c, &album) {
		return
	}
	var request struct {
		DisplayName *string `json:"display_name"`
		Description *string `json:"description"`
		SortOrder   *int    `json:"sort_order"`
		IsVisible   *bool   `json:"is_visible"`
	}
	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request"})
		return
	}
	updates := map[string]any{}
	if request.DisplayName != nil {
		updates["display_name"] = strings.TrimSpace(*request.DisplayName)
	}
	if request.Description != nil {
		updates["description"] = *request.Description
	}
	if request.SortOrder != nil {
		updates["sort_order"] = *request.SortOrder
	}
	if request.IsVisible != nil {
		updates["is_visible"] = *request.IsVisible
	}
	if err := h.db.Model(&album).Updates(updates).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	h.db.Preload("Category").First(&album, album.ID)
	c.JSON(http.StatusOK, album)
}

func (h *Handler) adminStories(c *gin.Context) {
	query := h.db.Model(&model.Story{}).Preload("Category").Preload("Album")
	if categoryID := c.Query("category_id"); categoryID != "" {
		query = query.Where("stories.category_id = ?", categoryID)
	}
	if albumID := c.Query("album_id"); albumID != "" {
		query = query.Where("stories.album_id = ?", albumID)
	}
	if keyword := strings.TrimSpace(c.Query("keyword")); keyword != "" {
		like := "%" + keyword + "%"
		query = query.Where("stories.title LIKE ? OR stories.display_title LIKE ? OR stories.relative_path LIKE ? OR stories.chapter LIKE ?", like, like, like, like)
	}
	h.paginated(c, query, &[]model.Story{}, "chapter ASC, sort_order ASC, title ASC")
}

func (h *Handler) updateStory(c *gin.Context) {
	var story model.Story
	if !h.findByID(c, &story) {
		return
	}
	var request struct {
		DisplayTitle *string `json:"display_title"`
		SortOrder    *int    `json:"sort_order"`
		IsVisible    *bool   `json:"is_visible"`
	}
	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request"})
		return
	}
	updates := map[string]any{}
	if request.DisplayTitle != nil {
		updates["display_title"] = strings.TrimSpace(*request.DisplayTitle)
	}
	if request.SortOrder != nil {
		updates["sort_order"] = *request.SortOrder
	}
	if request.IsVisible != nil {
		updates["is_visible"] = *request.IsVisible
	}
	if err := h.db.Model(&story).Updates(updates).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	h.db.Preload("Category").Preload("Album").First(&story, story.ID)
	c.JSON(http.StatusOK, story)
}

func (h *Handler) adminHistory(c *gin.Context) {
	query := h.db.Model(&model.PlayHistory{}).Preload("Story").Preload("Story.Album").Preload("Story.Category")
	h.paginated(c, query, &[]model.PlayHistory{}, "played_at DESC")
}

func (h *Handler) publicCategories(c *gin.Context) {
	var categories []model.Category
	if err := h.db.Where("is_visible = ?", true).Order("sort_order ASC, name ASC").Find(&categories).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"items": categories})
}

func (h *Handler) publicCategoryAlbums(c *gin.Context) {
	var albums []model.Album
	if err := h.db.Where("category_id = ? AND is_visible = ?", c.Param("id"), true).
		Order("sort_order ASC, name ASC").
		Find(&albums).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"items": albums})
}

func (h *Handler) publicAlbums(c *gin.Context) {
	query := h.db.Model(&model.Album{}).Where("is_visible = ?", true).Preload("Category")
	h.paginated(c, query, &[]model.Album{}, "sort_order ASC, name ASC")
}

func (h *Handler) publicAlbum(c *gin.Context) {
	var album model.Album
	if err := h.db.Preload("Category").Where("is_visible = ?", true).First(&album, c.Param("id")).Error; err != nil {
		writeNotFound(c, err)
		return
	}
	c.JSON(http.StatusOK, album)
}

func (h *Handler) publicAlbumStories(c *gin.Context) {
	var stories []model.Story
	if err := h.db.Where("album_id = ? AND is_visible = ? AND is_missing = ?", c.Param("id"), true, false).
		Order("chapter ASC, sort_order ASC, title ASC").
		Find(&stories).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"items": stories})
}

func (h *Handler) publicStory(c *gin.Context) {
	var story model.Story
	if err := h.db.Preload("Album").Preload("Category").
		Where("is_visible = ? AND is_missing = ?", true, false).
		First(&story, c.Param("id")).Error; err != nil {
		writeNotFound(c, err)
		return
	}
	c.JSON(http.StatusOK, story)
}

func (h *Handler) search(c *gin.Context) {
	keyword := strings.TrimSpace(c.Query("q"))
	if keyword == "" {
		c.JSON(http.StatusOK, gin.H{"items": []model.Story{}})
		return
	}
	like := "%" + keyword + "%"
	var stories []model.Story
	if err := h.db.Preload("Album").Preload("Category").
		Where("is_visible = ? AND is_missing = ? AND (title LIKE ? OR display_title LIKE ? OR chapter LIKE ?)", true, false, like, like, like).
		Order("sort_order ASC, title ASC").
		Limit(50).
		Find(&stories).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"items": stories})
}

func (h *Handler) streamStory(c *gin.Context) {
	var story model.Story
	if err := h.db.Where("is_visible = ? AND is_missing = ?", true, false).First(&story, c.Param("id")).Error; err != nil {
		writeNotFound(c, err)
		return
	}
	file, err := os.Open(story.FilePath)
	if err != nil {
		slog.Warn("play file not found", "story_id", story.ID, "error", err)
		c.JSON(http.StatusNotFound, gin.H{"error": "file not found"})
		return
	}
	defer file.Close()

	stat, err := file.Stat()
	if err != nil || stat.IsDir() {
		c.JSON(http.StatusNotFound, gin.H{"error": "file not found"})
		return
	}
	c.Header("Content-Type", contentType(story.FileExt))
	c.Header("Accept-Ranges", "bytes")
	http.ServeContent(c.Writer, c.Request, filepath.Base(story.FilePath), stat.ModTime(), file)
}

func (h *Handler) deviceHistory(c *gin.Context) {
	var history []model.PlayHistory
	if err := h.db.Preload("Story").Preload("Story.Album").Preload("Story.Category").
		Where("device_id = ?", c.Param("deviceId")).
		Order("played_at DESC").
		Limit(50).
		Find(&history).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"items": history})
}

func (h *Handler) saveHistory(c *gin.Context) {
	var request struct {
		StoryID         uint `json:"story_id"`
		PositionSeconds int  `json:"position_seconds"`
		DurationSeconds int  `json:"duration_seconds"`
	}
	if err := c.ShouldBindJSON(&request); err != nil || request.StoryID == 0 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request"})
		return
	}
	history := model.PlayHistory{
		DeviceID:        c.Param("deviceId"),
		StoryID:         request.StoryID,
		PositionSeconds: request.PositionSeconds,
		DurationSeconds: request.DurationSeconds,
		PlayedAt:        time.Now(),
	}
	if err := h.db.Create(&history).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, history)
}

func (h *Handler) deviceFavorites(c *gin.Context) {
	var favorites []model.Favorite
	if err := h.db.Preload("Story").Preload("Story.Album").Preload("Story.Category").
		Where("device_id = ?", c.Param("deviceId")).
		Order("created_at DESC").
		Find(&favorites).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"items": favorites})
}

func (h *Handler) addFavorite(c *gin.Context) {
	storyID, err := strconv.ParseUint(c.Param("storyId"), 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid story id"})
		return
	}
	favorite := model.Favorite{DeviceID: c.Param("deviceId"), StoryID: uint(storyID)}
	if err := scanner.UpsertFavorite(h.db, favorite); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"ok": true})
}

func (h *Handler) removeFavorite(c *gin.Context) {
	if err := h.db.Where("device_id = ? AND story_id = ?", c.Param("deviceId"), c.Param("storyId")).
		Delete(&model.Favorite{}).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"ok": true})
}

func (h *Handler) findByID(c *gin.Context, dest any) bool {
	if err := h.db.First(dest, c.Param("id")).Error; err != nil {
		writeNotFound(c, err)
		return false
	}
	return true
}

func (h *Handler) paginated(c *gin.Context, query *gorm.DB, dest any, order string) {
	page := positiveInt(c.Query("page"), 1)
	pageSize := positiveInt(c.Query("page_size"), 20)
	if pageSize > 100 {
		pageSize = 100
	}

	var total int64
	if err := query.Count(&total).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	if err := query.Order(order).Limit(pageSize).Offset((page - 1) * pageSize).Find(dest).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{
		"items":     dest,
		"total":     total,
		"page":      page,
		"page_size": pageSize,
	})
}

func positiveInt(value string, fallback int) int {
	parsed, err := strconv.Atoi(value)
	if err != nil || parsed <= 0 {
		return fallback
	}
	return parsed
}

func writeNotFound(c *gin.Context, err error) {
	if errors.Is(err, gorm.ErrRecordNotFound) {
		c.JSON(http.StatusNotFound, gin.H{"error": "not found"})
		return
	}
	c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
}

func contentType(ext string) string {
	switch strings.ToLower(strings.TrimPrefix(ext, ".")) {
	case "mp3":
		return "audio/mpeg"
	case "m4a":
		return "audio/mp4"
	case "aac":
		return "audio/aac"
	case "wav":
		return "audio/wav"
	case "flac":
		return "audio/flac"
	case "ogg":
		return "audio/ogg"
	default:
		return "application/octet-stream"
	}
}
