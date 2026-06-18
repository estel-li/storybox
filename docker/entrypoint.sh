#!/bin/sh
set -eu

if [ "$#" -gt 0 ]; then
  exec "$@"
fi

: "${STORY_SERVER_PORT:=8080}"
: "${STORY_DB_PATH:=/data/storybox.db}"
: "${STORY_LIBRARY_ROOT:=/library}"
: "${NITRO_HOST:=0.0.0.0}"
: "${NITRO_PORT:=3000}"

export STORY_SERVER_PORT STORY_DB_PATH STORY_LIBRARY_ROOT
export HOST="$NITRO_HOST"
export PORT="$NITRO_PORT"
export NITRO_HOST NITRO_PORT

mkdir -p "$(dirname "$STORY_DB_PATH")" "$STORY_LIBRARY_ROOT"

storybox-server &
backend_pid=$!

node /app/web/server/index.mjs &
web_pid=$!

shutdown() {
  kill -TERM "$backend_pid" "$web_pid" 2>/dev/null || true
  wait "$backend_pid" 2>/dev/null || true
  wait "$web_pid" 2>/dev/null || true
}

trap 'shutdown; exit 143' INT TERM

status=0
while :; do
  if ! kill -0 "$backend_pid" 2>/dev/null; then
    wait "$backend_pid" || status=$?
    break
  fi
  if ! kill -0 "$web_pid" 2>/dev/null; then
    wait "$web_pid" || status=$?
    break
  fi
  sleep 1
done

shutdown
exit "$status"
