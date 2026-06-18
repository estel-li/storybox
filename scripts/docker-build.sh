#!/bin/sh
set -eu

IMAGE_NAME="${IMAGE_NAME:-storybox-lan:local}"
DOCKERFILE="${DOCKERFILE:-Dockerfile}"
CONTEXT_DIR="${CONTEXT_DIR:-.}"

docker build \
  -f "$DOCKERFILE" \
  -t "$IMAGE_NAME" \
  "$CONTEXT_DIR"

cat <<EOF

Built $IMAGE_NAME

Example run:
  docker run --rm -p 3000:3000 -p 8080:8080 \\
    -v storybox-data:/data \\
    -v /path/to/story-library:/library:ro \\
    -e STORY_LIBRARY_ROOT=/library \\
    $IMAGE_NAME
EOF
