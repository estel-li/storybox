<script setup lang="ts">
import type { Album, Category, ListResponse } from '~/utils/types'

definePageMeta({ layout: 'admin' })

const categories = ref<Category[]>([])
const albums = ref<Album[]>([])
const categoryId = ref('')
const keyword = ref('')
const page = ref(1)
const pageSize = 20
const total = ref(0)
const loading = ref(false)
const savingId = ref<number | null>(null)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function loadCategories() {
  const response = await useApiFetch<ListResponse<Category>>('/api/admin/categories')
  categories.value = response.items
}

async function loadAlbums() {
  loading.value = true
  try {
    const response = await useApiFetch<ListResponse<Album>>('/api/admin/albums', {
      query: {
        category_id: categoryId.value || undefined,
        keyword: keyword.value || undefined,
        page: page.value,
        page_size: pageSize
      }
    })
    albums.value = response.items
    total.value = response.total || 0
  } finally {
    loading.value = false
  }
}

async function save(album: Album) {
  savingId.value = album.id
  try {
    const updated = await useApiFetch<Album>(`/api/admin/albums/${album.id}`, {
      method: 'PUT',
      body: {
        display_name: album.display_name,
        description: album.description,
        sort_order: Number(album.sort_order),
        is_visible: album.is_visible
      }
    })
    Object.assign(album, updated)
  } finally {
    savingId.value = null
  }
}

function applyFilters() {
  page.value = 1
  loadAlbums()
}

function nextPage() {
  if (page.value >= totalPages.value) return
  page.value++
  loadAlbums()
}

function previousPage() {
  if (page.value <= 1) return
  page.value--
  loadAlbums()
}

onMounted(async () => {
  await loadCategories()
  await loadAlbums()
})
</script>

<template>
  <div class="space-y-5">
    <UCard>
      <div class="flex flex-col gap-3 lg:flex-row lg:items-end">
        <label class="flex flex-col gap-1 text-sm text-slate-600">
          分类
          <select v-model="categoryId" class="native-select min-w-52">
            <option value="">全部分类</option>
            <option v-for="category in categories" :key="category.id" :value="String(category.id)">
              {{ category.display_name }}
            </option>
          </select>
        </label>
        <UFormField label="关键词" class="min-w-72 flex-1">
          <UInput v-model="keyword" icon="i-lucide-search" @keyup.enter="applyFilters" />
        </UFormField>
        <UButton icon="i-lucide-search" :loading="loading" @click="applyFilters">搜索</UButton>
      </div>
    </UCard>

    <UCard>
      <template #header>
        <div class="flex items-center justify-between">
          <h2 class="font-semibold">专辑列表</h2>
          <span class="text-sm text-slate-500">共 {{ total }} 个</span>
        </div>
      </template>
      <div class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>分类</th>
              <th>目录名</th>
              <th>显示名称</th>
              <th>简介</th>
              <th>故事数</th>
              <th>排序</th>
              <th>显示</th>
              <th />
            </tr>
          </thead>
          <tbody>
            <tr v-for="album in albums" :key="album.id">
              <td>{{ album.id }}</td>
              <td>{{ album.category?.display_name || album.category_id }}</td>
              <td class="min-w-52">{{ album.name }}</td>
              <td class="min-w-56">
                <UInput v-model="album.display_name" />
              </td>
              <td class="min-w-72">
                <UTextarea v-model="album.description" :rows="2" />
              </td>
              <td>{{ album.story_count }}</td>
              <td class="w-32">
                <UInput v-model.number="album.sort_order" type="number" />
              </td>
              <td class="w-24">
                <input v-model="album.is_visible" class="native-checkbox size-4" type="checkbox">
              </td>
              <td class="min-w-48">
                <div class="flex gap-2">
                  <UButton icon="i-lucide-save" size="sm" :loading="savingId === album.id" @click="save(album)">
                    保存
                  </UButton>
                  <UButton
                    :to="`/admin/stories?album_id=${album.id}`"
                    icon="i-lucide-list-music"
                    color="neutral"
                    variant="outline"
                    size="sm"
                  >
                    故事
                  </UButton>
                </div>
              </td>
            </tr>
            <tr v-if="albums.length === 0">
              <td colspan="9" class="text-center text-slate-500">暂无专辑</td>
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
