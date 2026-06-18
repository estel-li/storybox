package database

import (
	"log"
	"log/slog"
	"os"
	"path/filepath"

	"github.com/estel/storybox-lan/backend/internal/model"
	"gorm.io/driver/sqlite"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

func Open(dbPath string) (*gorm.DB, error) {
	if err := os.MkdirAll(filepath.Dir(dbPath), 0o755); err != nil {
		return nil, err
	}

	db, err := gorm.Open(sqlite.Open(dbPath), &gorm.Config{
		Logger: logger.New(log.New(os.Stdout, "\r\n", log.LstdFlags), logger.Config{
			LogLevel:                  logger.Warn,
			IgnoreRecordNotFoundError: true,
		}),
	})
	if err != nil {
		return nil, err
	}

	if err := db.AutoMigrate(
		&model.Category{},
		&model.Album{},
		&model.Story{},
		&model.ScanJob{},
		&model.PlayHistory{},
		&model.Favorite{},
		&model.Setting{},
	); err != nil {
		return nil, err
	}

	slog.Info("database initialized", "path", dbPath)
	return db, nil
}
