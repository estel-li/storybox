<script setup lang="ts">
import type { Album, Category, Favorite, ListResponse, PlayHistory, Story } from '~/utils/types'

definePageMeta({ layout: 'default' })

const config = useRuntimeConfig()
const apiBase = String(config.public.apiBase || '').replace(/\/$/, '')
const device = useDeviceStore()

const deviceId = ref('')
const categories = ref<Category[]>([])
const albums = ref<Album[]>([])
const stories = ref<Story[]>([])
const history = ref<PlayHistory[]>([])
const favorites = ref<Favorite[]>([])
const favoriteIds = ref<Set<number>>(new Set())
const selectedCategory = ref<Category | null>(null)
const selectedAlbum = ref<Album | null>(null)
const currentStory = ref<Story | null>(null)
const keyword = ref('')
const loading = ref(false)
const audioEl = ref<HTMLAudioElement | null>(null)
const savingProgress = ref(false)
let lastProgressSaveAt = 0

const title = computed(() => currentStory.value?.display_title || '请选择故事')
const subtitle = computed(() => currentStory.value?.album?.display_name || selectedAlbum.value?.display_name || '')
const streamSrc = computed(() => currentStory.value ? `${apiBase}/api/stories/${currentStory.value.id}/stream` : '')

function formatTime(seconds: number) {
  if (!Number.isFinite(seconds) || seconds <= 0) return '0:00'
  const minute = Math.floor(seconds / 60)
  const second = Math.floor(seconds % 60)
  return `${minute}:${String(second).padStart(2, '0')}`
}

function isFavorite(story: Story) {
  return favoriteIds.value.has(story.id)
}

async function loadCategories() {
  const response = await useApiFetch<ListResponse<Category>>('/api/categories')
  categories.value = response.items
  const firstCategory = categories.value[0]
  if (!selectedCategory.value && firstCategory) {
    await chooseCategory(firstCategory)
  }
}

async function chooseCategory(category: Category) {
  selectedCategory.value = category
  selectedAlbum.value = null
  stories.value = []
  const response = await useApiFetch<ListResponse<Album>>(`/api/categories/${category.id}/albums`)
  albums.value = response.items
}

async function chooseAlbum(album: Album) {
  selectedAlbum.value = album
  keyword.value = ''
  const response = await useApiFetch<ListResponse<Story>>(`/api/albums/${album.id}/stories`)
  stories.value = response.items.map((story) => ({ ...story, album }))
}

async function searchStories() {
  const q = keyword.value.trim()
  if (!q) {
    if (selectedAlbum.value) {
      await chooseAlbum(selectedAlbum.value)
    }
    return
  }
  loading.value = true
  try {
    const response = await useApiFetch<ListResponse<Story>>('/api/search', { query: { q } })
    stories.value = response.items
  } finally {
    loading.value = false
  }
}

async function playStory(story: Story) {
  await saveProgress(true)
  currentStory.value = story
  await nextTick()
  audioEl.value?.play().catch(() => {})
}

async function loadHistory() {
  if (!deviceId.value) return
  const response = await useApiFetch<ListResponse<PlayHistory>>(`/api/devices/${deviceId.value}/history`)
  history.value = response.items
}

async function loadFavorites() {
  if (!deviceId.value) return
  const response = await useApiFetch<ListResponse<Favorite>>(`/api/devices/${deviceId.value}/favorites`)
  favorites.value = response.items
  favoriteIds.value = new Set(response.items.map((item) => item.story_id))
}

async function toggleFavorite(story: Story) {
  if (!deviceId.value) return
  const next = new Set(favoriteIds.value)
  if (next.has(story.id)) {
    await useApiFetch(`/api/devices/${deviceId.value}/favorites/${story.id}`, { method: 'DELETE' })
    next.delete(story.id)
  } else {
    await useApiFetch(`/api/devices/${deviceId.value}/favorites/${story.id}`, { method: 'POST' })
    next.add(story.id)
  }
  favoriteIds.value = next
  await loadFavorites()
}

async function saveProgress(force = false) {
  if (!deviceId.value || !currentStory.value || !audioEl.value || savingProgress.value) return
  const now = Date.now()
  if (!force && now - lastProgressSaveAt < 15000) return
  lastProgressSaveAt = now
  savingProgress.value = true
  try {
    await useApiFetch(`/api/devices/${deviceId.value}/history`, {
      method: 'POST',
      body: {
        story_id: currentStory.value.id,
        position_seconds: Math.floor(audioEl.value.currentTime || 0),
        duration_seconds: Math.floor(Number.isFinite(audioEl.value.duration) ? audioEl.value.duration : 0)
      }
    })
    await loadHistory()
  } catch {
  } finally {
    savingProgress.value = false
  }
}

onMounted(async () => {
  deviceId.value = device.ensure()
  await Promise.all([loadFavorites(), loadHistory()])
  await loadCategories()
})
</script>

<template>
  <div class="min-h-screen bg-[#f6f7f2] pb-36 text-slate-900">
    <header class="border-b border-emerald-100 bg-white">
      <div class="page-shell flex flex-col gap-4 py-5 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 class="text-2xl font-semibold">老李讲故事</h1>
          <p class="text-sm text-slate-500">局域网故事播放器</p>
        </div>
        <div class="flex gap-2">
          <UButton to="/admin" icon="i-lucide-settings" color="neutral" variant="outline">后台</UButton>
          <UButton icon="i-lucide-refresh-cw" color="neutral" variant="ghost" @click="loadCategories">刷新</UButton>
        </div>
      </div>
    </header>

    <main class="page-shell grid gap-5 py-5 xl:grid-cols-[240px_300px_1fr]">
      <section class="space-y-3">
        <div class="flex items-center justify-between">
          <h2 class="text-base font-semibold">分类</h2>
          <UBadge color="primary" variant="soft">{{ categories.length }}</UBadge>
        </div>
        <div class="grid gap-2">
          <UButton
            v-for="category in categories"
            :key="category.id"
            icon="i-lucide-folder"
            :color="selectedCategory?.id === category.id ? 'primary' : 'neutral'"
            :variant="selectedCategory?.id === category.id ? 'soft' : 'outline'"
            block
            class="justify-start"
            @click="chooseCategory(category)"
          >
            {{ category.display_name }}
          </UButton>
          <p v-if="categories.length === 0" class="rounded-md border border-dashed border-slate-300 p-4 text-sm text-slate-500">
            暂无分类
          </p>
        </div>
      </section>

      <section class="space-y-3">
        <div class="flex items-center justify-between">
          <h2 class="text-base font-semibold">专辑</h2>
          <UBadge color="primary" variant="soft">{{ albums.length }}</UBadge>
        </div>
        <div class="grid gap-2">
          <UButton
            v-for="album in albums"
            :key="album.id"
            icon="i-lucide-book-open"
            :color="selectedAlbum?.id === album.id ? 'primary' : 'neutral'"
            :variant="selectedAlbum?.id === album.id ? 'soft' : 'outline'"
            block
            class="justify-start"
            @click="chooseAlbum(album)"
          >
            <span class="truncate">{{ album.display_name }}</span>
            <span class="ml-auto text-xs text-slate-400">{{ album.story_count }}</span>
          </UButton>
          <p v-if="albums.length === 0" class="rounded-md border border-dashed border-slate-300 p-4 text-sm text-slate-500">
            暂无专辑
          </p>
        </div>
      </section>

      <section class="space-y-4">
        <div class="flex flex-col gap-3 rounded-md border border-slate-200 bg-white p-4 md:flex-row md:items-end">
          <UFormField label="搜索故事" class="flex-1">
            <UInput v-model="keyword" icon="i-lucide-search" @keyup.enter="searchStories" />
          </UFormField>
          <UButton icon="i-lucide-search" :loading="loading" @click="searchStories">搜索</UButton>
        </div>

        <div class="grid gap-3 lg:grid-cols-[1fr_280px]">
          <div class="space-y-2">
            <div class="flex items-center justify-between">
              <h2 class="text-base font-semibold">故事</h2>
              <UBadge color="primary" variant="soft">{{ stories.length }}</UBadge>
            </div>
            <div class="max-h-[62vh] overflow-auto rounded-md border border-slate-200 bg-white">
              <button
                v-for="story in stories"
                :key="story.id"
                class="flex w-full items-center gap-3 border-b border-slate-100 px-4 py-3 text-left last:border-b-0 hover:bg-emerald-50"
                :class="currentStory?.id === story.id ? 'bg-emerald-50' : ''"
                @click="playStory(story)"
              >
                <UIcon name="i-lucide-play-circle" class="size-5 shrink-0 text-emerald-600" />
                <span class="min-w-0 flex-1">
                  <span class="block truncate font-medium">{{ story.display_title }}</span>
                  <span class="block truncate text-xs text-slate-500">{{ story.chapter || story.album?.display_name || selectedAlbum?.display_name }}</span>
                </span>
                <UButton
                  :icon="isFavorite(story) ? 'i-lucide-heart' : 'i-lucide-heart-plus'"
                  :color="isFavorite(story) ? 'error' : 'neutral'"
                  variant="ghost"
                  size="sm"
                  @click.stop="toggleFavorite(story)"
                />
              </button>
              <p v-if="stories.length === 0" class="p-6 text-center text-sm text-slate-500">请选择专辑或搜索故事</p>
            </div>
          </div>

          <aside class="space-y-4">
            <div class="rounded-md border border-slate-200 bg-white p-4">
              <h2 class="mb-3 text-base font-semibold">继续听</h2>
              <div class="space-y-2">
                <button
                  v-for="item in history.slice(0, 5)"
                  :key="item.id"
                  class="w-full rounded-md border border-slate-100 px-3 py-2 text-left hover:bg-slate-50"
                  @click="item.story && playStory(item.story)"
                >
                  <span class="block truncate text-sm font-medium">{{ item.story?.display_title || item.story_id }}</span>
                  <span class="text-xs text-slate-500">{{ formatTime(item.position_seconds) }}</span>
                </button>
                <p v-if="history.length === 0" class="text-sm text-slate-500">暂无记录</p>
              </div>
            </div>

            <div class="rounded-md border border-slate-200 bg-white p-4">
              <h2 class="mb-3 text-base font-semibold">收藏</h2>
              <div class="space-y-2">
                <button
                  v-for="item in favorites.slice(0, 8)"
                  :key="item.id"
                  class="w-full rounded-md border border-slate-100 px-3 py-2 text-left hover:bg-slate-50"
                  @click="item.story && playStory(item.story)"
                >
                  <span class="block truncate text-sm font-medium">{{ item.story?.display_title || item.story_id }}</span>
                  <span class="block truncate text-xs text-slate-500">{{ item.story?.album?.display_name || '' }}</span>
                </button>
                <p v-if="favorites.length === 0" class="text-sm text-slate-500">暂无收藏</p>
              </div>
            </div>
          </aside>
        </div>
      </section>
    </main>

    <footer class="fixed inset-x-0 bottom-0 border-t border-slate-200 bg-white/95 backdrop-blur">
      <div class="page-shell flex flex-col gap-3 py-4 md:flex-row md:items-center md:justify-between">
        <div class="min-w-0">
          <p class="truncate text-base font-semibold">{{ title }}</p>
          <p class="truncate text-sm text-slate-500">{{ subtitle || '未播放' }}</p>
        </div>
        <div class="flex min-w-0 flex-1 items-center gap-3 md:max-w-2xl">
          <audio
            ref="audioEl"
            class="w-full"
            :src="streamSrc"
            controls
            @timeupdate="saveProgress(false)"
            @pause="saveProgress(true)"
            @ended="saveProgress(true)"
          />
          <UButton
            v-if="currentStory"
            :icon="isFavorite(currentStory) ? 'i-lucide-heart' : 'i-lucide-heart-plus'"
            :color="isFavorite(currentStory) ? 'error' : 'neutral'"
            variant="outline"
            @click="toggleFavorite(currentStory)"
          />
        </div>
      </div>
    </footer>
  </div>
</template>
