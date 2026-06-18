package util

import (
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
)

const DefaultSortOrder = 999999

var (
	chineseEpisodePattern = regexp.MustCompile(`^第\s*([0-9０-９]+)\s*[集回章讲节]?\s*[-_ .、，:：]*\s*(.*)$`)
	prefixPattern         = regexp.MustCompile(`^([0-9０-９]+)\s*[-_ .、，:：]*\s*(.*)$`)
)

func ParseNamePrefix(name string) (int, string) {
	name = strings.TrimSpace(name)
	if name == "" {
		return DefaultSortOrder, name
	}
	for _, pattern := range []*regexp.Regexp{chineseEpisodePattern, prefixPattern} {
		matches := pattern.FindStringSubmatch(name)
		if len(matches) == 3 {
			sortOrder := parseDigits(matches[1])
			title := strings.TrimSpace(matches[2])
			title = strings.TrimLeft(title, "-_ .、，:：")
			title = strings.TrimSpace(title)
			if title == "" {
				title = name
			}
			return sortOrder, title
		}
	}
	return DefaultSortOrder, name
}

func ParseFileName(filename string) (int, string) {
	base := strings.TrimSuffix(filename, filepath.Ext(filename))
	return ParseNamePrefix(base)
}

func DisplayNameFromDir(name string) string {
	_, displayName := ParseNamePrefix(name)
	return displayName
}

func parseDigits(value string) int {
	normalized := strings.NewReplacer(
		"０", "0",
		"１", "1",
		"２", "2",
		"３", "3",
		"４", "4",
		"５", "5",
		"６", "6",
		"７", "7",
		"８", "8",
		"９", "9",
	).Replace(value)
	parsed, err := strconv.Atoi(normalized)
	if err != nil {
		return DefaultSortOrder
	}
	return parsed
}

func CleanRelativePath(path string) string {
	return filepath.ToSlash(strings.TrimPrefix(path, string(filepath.Separator)))
}
