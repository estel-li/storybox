<script setup lang="ts">
import type { AdminStats, ListResponse, ScanJob, ScanResult } from '~/utils/types'

definePageMeta({ layout: 'admin' })

const stats = ref<AdminStats | null>(null)
const jobs = ref<ScanJob[]>([])
const result = ref<ScanResult | null>(null)
const loading = ref(false)
const errorMessage = ref('')

async function load() {
  const [statsResponse, jobsResponse] = await Promise.all([
    useApiFetch<AdminStats>('/api/admin/stats'),
    useApiFetch<ListResponse<ScanJob>>('/api/admin/scan/jobs')
  ])
  stats.value = statsResponse
  jobs.value = jobsResponse.items
}

async function startScan() {
  loading.value = true
  errorMessage.value = ''
  result.value = null
  try {
    result.value = await useApiFetch<ScanResult>('/api/admin/scan', { method: 'POST' })
    await load()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '扫描失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="space-y-5">
    <UCard>
      <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <p class="text-sm text-slate-500">当前扫描目录</p>
          <p class="mt-1 break-all font-medium text-slate-900">{{ stats?.library_root || '-' }}</p>
        </div>
        <UButton icon="i-lucide-scan-search" :loading="loading" @click="startScan">开始扫描</UButton>
      </div>
    </UCard>

    <UAlert v-if="errorMessage" color="error" variant="soft" :title="errorMessage" />

    <UCard v-if="result">
      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-6">
        <div>
          <p class="text-sm text-slate-500">总文件</p>
          <p class="text-2xl font-semibold">{{ result.total_files }}</p>
        </div>
        <div>
          <p class="text-sm text-slate-500">音频文件</p>
          <p class="text-2xl font-semibold">{{ result.audio_files }}</p>
        </div>
        <div>
          <p class="text-sm text-slate-500">新增</p>
          <p class="text-2xl font-semibold">{{ result.created_count }}</p>
        </div>
        <div>
          <p class="text-sm text-slate-500">更新</p>
          <p class="text-2xl font-semibold">{{ result.updated_count }}</p>
        </div>
        <div>
          <p class="text-sm text-slate-500">缺失</p>
          <p class="text-2xl font-semibold">{{ result.missing_count }}</p>
        </div>
        <div>
          <p class="text-sm text-slate-500">耗时</p>
          <p class="text-2xl font-semibold">{{ result.duration_ms }} ms</p>
        </div>
      </div>
    </UCard>

    <UCard>
      <template #header>
        <div class="flex items-center justify-between">
          <h2 class="font-semibold">最近扫描记录</h2>
          <UButton icon="i-lucide-refresh-cw" color="neutral" variant="ghost" size="sm" @click="load">刷新</UButton>
        </div>
      </template>
      <div class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>开始时间</th>
              <th>状态</th>
              <th>音频</th>
              <th>新增</th>
              <th>更新</th>
              <th>缺失</th>
              <th>耗时</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="job in jobs" :key="job.id">
              <td>{{ job.started_at }}</td>
              <td>
                <UBadge :color="job.status === 'success' ? 'success' : job.status === 'failed' ? 'error' : 'warning'">
                  {{ job.status }}
                </UBadge>
              </td>
              <td>{{ job.audio_files }}</td>
              <td>{{ job.created_count }}</td>
              <td>{{ job.updated_count }}</td>
              <td>{{ job.missing_count }}</td>
              <td>{{ job.duration_ms }} ms</td>
            </tr>
            <tr v-if="jobs.length === 0">
              <td colspan="7" class="text-center text-slate-500">暂无记录</td>
            </tr>
          </tbody>
        </table>
      </div>
    </UCard>
  </div>
</template>
