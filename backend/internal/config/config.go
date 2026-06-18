package config

import (
	"log/slog"
	"os"
	"strconv"
	"strings"

	"github.com/joho/godotenv"
)

type Config struct {
	ServerPort     string
	DBPath         string
	LibraryRoot    string
	AdminPassword  string
	JWTSecret      string
	FollowSymlinks bool
	CORSOrigins    []string
}

func Load() Config {
	if err := godotenv.Load(); err != nil {
		slog.Info("no .env file found, using defaults and environment")
	}

	return Config{
		ServerPort:     env("STORY_SERVER_PORT", "8080"),
		DBPath:         env("STORY_DB_PATH", "./storage/storybox.db"),
		LibraryRoot:    env("STORY_LIBRARY_ROOT", "/volume3/16T/儿童故事/01_library_故事库"),
		AdminPassword:  env("STORY_ADMIN_PASSWORD", "admin123456"),
		JWTSecret:      env("STORY_JWT_SECRET", "please-change-me"),
		FollowSymlinks: envBool("STORY_FOLLOW_SYMLINKS", true),
		CORSOrigins:    envList("STORY_CORS_ORIGINS", []string{"http://localhost:3000"}),
	}
}

func env(key, fallback string) string {
	value := strings.TrimSpace(os.Getenv(key))
	if value == "" {
		return fallback
	}
	return value
}

func envBool(key string, fallback bool) bool {
	value := strings.TrimSpace(os.Getenv(key))
	if value == "" {
		return fallback
	}
	parsed, err := strconv.ParseBool(value)
	if err != nil {
		return fallback
	}
	return parsed
}

func envList(key string, fallback []string) []string {
	value := strings.TrimSpace(os.Getenv(key))
	if value == "" {
		return fallback
	}
	parts := strings.Split(value, ",")
	out := make([]string, 0, len(parts))
	for _, part := range parts {
		trimmed := strings.TrimSpace(part)
		if trimmed != "" {
			out = append(out, trimmed)
		}
	}
	if len(out) == 0 {
		return fallback
	}
	return out
}
