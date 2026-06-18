package main

import (
	"log/slog"
	"os"
	"time"

	"github.com/estel/storybox-lan/backend/internal/config"
	"github.com/estel/storybox-lan/backend/internal/database"
	"github.com/estel/storybox-lan/backend/internal/handler"
	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
)

func main() {
	slog.SetDefault(slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo})))

	cfg := config.Load()
	db, err := database.Open(cfg.DBPath)
	if err != nil {
		slog.Error("database initialization failed", "error", err)
		os.Exit(1)
	}

	router := gin.Default()
	router.Use(cors.New(cors.Config{
		AllowOrigins:     cfg.CORSOrigins,
		AllowMethods:     []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowHeaders:     []string{"Origin", "Content-Type", "Authorization", "Range"},
		ExposeHeaders:    []string{"Content-Length", "Content-Range", "Accept-Ranges"},
		AllowCredentials: true,
		MaxAge:           12 * time.Hour,
	}))

	handler.New(db, cfg).Register(router)

	addr := ":" + cfg.ServerPort
	slog.Info("storybox server starting", "addr", addr, "library_root", cfg.LibraryRoot)
	if err := router.Run(addr); err != nil {
		slog.Error("server stopped", "error", err)
		os.Exit(1)
	}
}
