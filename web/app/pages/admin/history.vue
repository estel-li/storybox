<script setup lang="ts">
import type { ListResponse, PlayHistory } from '~/utils/types'

definePageMeta({ layout: 'admin' })

const history = ref<PlayHistory[]>([])
const page = ref(1)
const pageSize = 30
const total = ref(0)
const loading = ref(false)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

function formatTime(seconds: number) {
  const minute = Math.floor(seconds / 60)
  const second = Math.floor(seconds % 60)
  return `${minute}:${String(second).padStart(2, '0')}`
}

async function load() {
  loading.value = true
  try {
    const response = await useApiFetch<ListResponse<PlayHistory>>('/api/admin/history', {
      query: { page: page.value, page_size: pageSize }
    })
    history.value = response.items
    total.value = response.total || 0
  } finally {
    loading.value = false
  }
}

function nextPage() {
  if (page.value >= totalPages.value) return
  page.value++
  load()
}

function previousPage() {
  if (page.value <= 1) return
  page.value--
  load()
}

onMounted(load)
</script>

<template>
  <UCard>
    <template #header>
      <div class="flex items-center justify-between">
        <h2 class="font-semibold">播放记录</h2>
        <UButton icon="i-lucide-refresh-cw" color="neutral" variant="ghost" :loading="loading" @click="load">刷新</UButton>
      </div>
    </template>
    <div class="overflow-x-auto">
      <table class="data-table">
        <thead>
          <tr>
            <th>设备</th>
            <th>故事</th>
            <th>专辑</th>
            <th>进度</th>
            <th>播放时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in history" :key="item.id">
            <td class="max-w-48 truncate">{{ item.device_id }}</td>
            <td>{{ item.story?.display_title || item.story_id }}</td>
            <td>{{ item.story?.album?.display_name || '-' }}</td>
            <td>{{ formatTime(item.position_seconds) }} / {{ formatTime(item.duration_seconds) }}</td>
            <td>{{ item.played_at }}</td>
          </tr>
          <tr v-if="history.length === 0">
            <td colspan="5" class="text-center text-slate-500">暂无记录</td>
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
</template>
