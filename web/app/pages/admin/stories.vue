<script setup lang="ts">
import type { Album, Category, ListResponse, Story } from '~/utils/types'

definePageMeta({ layout: 'admin' })

const route = useRoute()
const config = useRuntimeConfig()
const apiBase = String(config.public.apiBase || '').replace(/\/$/, '')

const categories = ref<Category[]>([])
const albums = ref<Album[]>([])
const stories = ref<Story[]>([])
const categoryId = ref('')
const albumId = ref('')
const keyword = ref('')
const page = ref(1)
const pageSize = 20
const total = ref(0)
const loading = ref(false)
const savingId = ref<number | null>(null)
const previewId = ref<number | null>(null)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

function streamUrl(story: Story) {
  return `${apiBase}/api/stories/${story.id}/stream`
}

function formatSize(size: number) {
  if (!size) return '-'
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

async function loadCategories() {
  const response = await useApiFetch<ListResponse<Category>>('/api/admin/categories')
  categories.value = response.items
}

async function loadAlbums() {
  const response = await useApiFetch<ListResponse<Album>>('/api/admin/albums', {
    query: {
      category_id: categoryId.value || undefined,
      page: 1,
      page_size: 100
    }
  })
  albums.value = response.items
}

async function loadStories() {
  loading.value = true
  try {
    const response = await useApiFetch<ListResponse<Story>>('/api/admin/stories', {
      query: {
        category_id: categoryId.value || undefined,
        album_id: albumId.value || undefined,
        keyword: keyword.value || undefined,
        page: page.value,
        page_size: pageSize
      }
    })
    stories.value = response.items
    total.value = response.total || 0
  } finally {
    loading.value = false
  }
}

async function save(story: Story) {
  savingId.value = story.id
  try {
    const updated = await useApiFetch<Story>(`/api/admin/stories/${story.id}`, {
      method: 'PUT',
      body: {
        display_title: story.display_title,
        sort_order: Number(story.sort_order),
        is_visible: story.is_visible
      }
    })
    Object.assign(story, updated)
  } finally {
    savingId.value = null
  }
}

async function applyFilters() {
  page.value = 1
  await loadStories()
}

async function handleCategoryChange() {
  albumId.value = ''
  await loadAlbums()
  await applyFilters()
}

function nextPage() {
  if (page.value >= totalPages.value) return
  page.value++
  loadStories()
}

function previousPage() {
  if (page.value <= 1) return
  page.value--
  loadStories()
}

onMounted(async () => {
  albumId.value = typeof route.query.album_id === 'string' ? route.query.album_id : ''
  categoryId.value = typeof route.query.category_id === 'string' ? route.query.category_id : ''
  await loadCategories()
  await loadAlbums()
  await loadStories()
})
</script>

<template>
  <div class="space-y-5">
    <UCard>
      <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-[220px_260px_1fr_auto] xl:items-end">
        <label class="flex flex-col gap-1 text-sm text-slate-600">
          分类
          <select v-model="categoryId" class="native-select" @change="handleCategoryChange">
            <option value="">全部分类</option>
            <option v-for="category in categories" :key="category.id" :value="String(category.id)">
              {{ category.display_name }}
            </option>
          </select>
        </label>
        <label class="flex flex-col gap-1 text-sm text-slate-600">
          专辑
          <select v-model="albumId" class="native-select">
            <option value="">全部专辑</option>
            <option v-for="album in albums" :key="album.id" :value="String(album.id)">
              {{ album.display_name }}
            </option>
          </select>
        </label>
        <UFormField label="关键词">
          <UInput v-model="keyword" icon="i-lucide-search" @keyup.enter="applyFilters" />
        </UFormField>
        <UButton icon="i-lucide-search" :loading="loading" @click="applyFilters">搜索</UButton>
      </div>
    </UCard>

    <UCard>
      <template #header>
        <div class="flex items-center justify-between">
          <h2 class="font-semibold">故事列表</h2>
          <span class="text-sm text-slate-500">共 {{ total }} 个</span>
        </div>
      </template>
      <div class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>分类</th>
              <th>专辑</th>
              <th>章节</th>
              <th>标题</th>
              <th>格式</th>
              <th>大小</th>
              <th>状态</th>
              <th>排序</th>
              <th>显示</th>
              <th />
            </tr>
          </thead>
          <tbody>
            <template v-for="story in stories" :key="story.id">
              <tr>
                <td>{{ story.id }}</td>
                <td>{{ story.category?.display_name || story.category_id }}</td>
                <td class="min-w-48">{{ story.album?.display_name || story.album_id }}</td>
                <td class="min-w-40">{{ story.chapter || '-' }}</td>
                <td class="min-w-72">
                  <UInput v-model="story.display_title" />
                  <p class="mt-1 truncate text-xs text-slate-500">{{ story.relative_path }}</p>
                </td>
                <td>{{ story.file_ext }}</td>
                <td>{{ formatSize(story.file_size) }}</td>
                <td>
                  <UBadge :color="story.is_missing ? 'error' : 'success'">
                    {{ story.is_missing ? '缺失' : '正常' }}
                  </UBadge>
                </td>
                <td class="w-32">
                  <UInput v-model.number="story.sort_order" type="number" />
                </td>
                <td class="w-24">
                  <input v-model="story.is_visible" class="native-checkbox size-4" type="checkbox">
                </td>
                <td class="min-w-48">
                  <div class="flex gap-2">
                    <UButton icon="i-lucide-save" size="sm" :loading="savingId === story.id" @click="save(story)">
                      保存
                    </UButton>
                    <UButton
                      icon="i-lucide-headphones"
                      size="sm"
                      color="neutral"
                      variant="outline"
                      :disabled="story.is_missing || !story.is_visible"
                      @click="previewId = previewId === story.id ? null : story.id"
                    >
                      试听
                    </UButton>
                  </div>
                </td>
              </tr>
              <tr v-if="previewId === story.id">
                <td colspan="11">
                  <audio class="w-full" controls :src="streamUrl(story)" />
                </td>
              </tr>
            </template>
            <tr v-if="stories.length === 0">
              <td colspan="11" class="text-center text-slate-500">暂无故事</td>
            </tr>
          </tbody>
        </table>
      </div>
      <template #footer>
        <div class="flex items-center justify-end gap-2">
          <UButton icon="i-lucide-chevron-left" color="neutral" variant="outline" :disabled="page <= 1" @click="previousPage">
            上一页
          </UButton>
          <span class="text-sm text-slate-600">{{ page }} / {{ totalPages }}</span>
          <UButton icon="i-lucide-chevron-right" color="neutral" variant="outline" :disabled="page >= totalPages" @click="nextPage">
            下一页
          </UButton>
        </div>
      </template>
    </UCard>
  </div>
</template>
