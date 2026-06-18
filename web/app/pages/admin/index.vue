<script setup lang="ts">
import type { AdminStats } from '~/utils/types'

definePageMeta({ layout: 'admin' })

const stats = ref<AdminStats | null>(null)
const loading = ref(false)
const errorMessage = ref('')

const tiles = computed(() => {
  const value = stats.value
  return [
    { label: '分类数', value: value?.category_count ?? 0, icon: 'i-lucide-folder-tree' },
    { label: '专辑数', value: value?.album_count ?? 0, icon: 'i-lucide-library' },
    { label: '故事数', value: value?.story_count ?? 0, icon: 'i-lucide-list-music' },
    { label: '可见故事', value: value?.visible_story_count ?? 0, icon: 'i-lucide-eye' },
    { label: '缺失故事', value: value?.missing_story_count ?? 0, icon: 'i-lucide-file-warning' }
  ]
})

async function loadStats() {
  loading.value = true
  errorMessage.value = ''
  try {
    stats.value = await useApiFetch<AdminStats>('/api/admin/stats')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<template>
  <div class="space-y-5">
    <div class="flex items-center justify-between gap-3">
      <div />
      <UButton icon="i-lucide-refresh-cw" color="neutral" variant="outline" :loading="loading" @click="loadStats">
        刷新
      </UButton>
    </div>

    <UAlert v-if="errorMessage" color="error" variant="soft" :title="errorMessage" />

    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
      <UCard v-for="tile in tiles" :key="tile.label">
        <div class="flex items-center justify-between gap-4">
          <div>
            <p class="text-sm text-slate-500">{{ tile.label }}</p>
            <p class="mt-2 text-3xl font-semibold text-slate-900">{{ tile.value }}</p>
          </div>
          <UIcon :name="tile.icon" class="size-8 text-primary" />
        </div>
      </UCard>
    </div>

    <UCard>
      <div class="grid gap-4 md:grid-cols-2">
        <div>
          <p class="text-sm text-slate-500">最近扫描时间</p>
          <p class="mt-1 font-medium text-slate-900">{{ stats?.last_scan_time || '暂无' }}</p>
        </div>
        <div>
          <p class="text-sm text-slate-500">故事库路径</p>
          <p class="mt-1 break-all font-medium text-slate-900">{{ stats?.library_root || '-' }}</p>
        </div>
      </div>
    </UCard>
  </div>
</template>
