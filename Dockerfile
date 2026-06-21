# syntax=docker/dockerfile:1.7

FROM golang:1.24-bookworm AS backend-builder

WORKDIR /src/backend

COPY backend/go.mod backend/go.sum ./
RUN go mod download

COPY backend/ ./

ARG TARGETOS=linux
ARG TARGETARCH=amd64
RUN CGO_ENABLED=0 GOOS=$TARGETOS GOARCH=$TARGETARCH \
    go build -trimpath -ldflags="-s -w" -o /out/storybox-server ./cmd/server


FROM node:24-bookworm-slim AS web-builder

WORKDIR /src/web

ENV COREPACK_ENABLE_DOWNLOAD_PROMPT=0
ENV NUXT_TELEMETRY_DISABLED=1

RUN corepack enable

COPY web/package.json web/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY web/ ./
RUN pnpm build


FROM node:24-bookworm-slim AS runtime

WORKDIR /app

ENV NODE_ENV=production
ENV NITRO_HOST=0.0.0.0
ENV NITRO_PORT=3000
ENV HOST=0.0.0.0
ENV PORT=3000
ENV STORY_SERVER_PORT=8080
ENV STORY_DB_PATH=/data/storybox.db
ENV STORY_LIBRARY_ROOT=/library
ENV STORY_CORS_ORIGINS=http://localhost:3000
ENV NUXT_PUBLIC_API_BASE=
ENV NUXT_API_PROXY_TARGET=

RUN mkdir -p /data /library

COPY --from=backend-builder /out/storybox-server /usr/local/bin/storybox-server
COPY --from=web-builder /src/web/.output /app/web
COPY docker/entrypoint.sh /usr/local/bin/storybox-entrypoint

RUN chmod +x /usr/local/bin/storybox-entrypoint

EXPOSE 3000 8080
VOLUME ["/data", "/library"]

ENTRYPOINT ["storybox-entrypoint"]
