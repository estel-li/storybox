<script setup lang="ts">
import type { Category, ListResponse } from '~/utils/types'

definePageMeta({ layout: 'admin' })

const categories = ref<Category[]>([])
const loading = ref(false)
const savingId = ref<number | null>(null)

async function load() {
  loading.value = true
  try {
    const response = await useApiFetch<ListResponse<Category>>('/api/admin/categories')
    categories.value = response.items
  } finally {
    loading.value = false
  }
}

async function save(category: Category) {
  savingId.value = category.id
  try {
    const updated = await useApiFetch<Category>(`/api/admin/categories/${category.id}`, {
      method: 'PUT',
      body: {
        display_name: category.display_name,
        sort_order: Number(category.sort_order),
        is_visible: category.is_visible
      }
    })
    Object.assign(category, updated)
  } finally {
    savingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <UCard>
    <template #header>
      <div class="flex items-center justify-between">
        <h2 class="font-semibold">分类列表</h2>
        <UButton icon="i-lucide-refresh-cw" color="neutral" variant="ghost" :loading="loading" @click="load">刷新</UButton>
      </div>
    </template>
    <div class="overflow-x-auto">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>目录名</th>
            <th>显示名称</th>
            <th>排序</th>
            <th>显示</th>
            <th />
          </tr>
        </thead>
        <tbody>
          <tr v-for="category in categories" :key="category.id">
            <td>{{ category.id }}</td>
            <td>{{ category.name }}</td>
            <td class="min-w-56">
              <UInput v-model="category.display_name" />
            </td>
            <td class="w-32">
              <UInput v-model.number="category.sort_order" type="number" />
            </td>
            <td class="w-24">
              <input v-model="category.is_visible" class="native-checkbox size-4" type="checkbox">
            </td>
            <td class="w-28">
              <UButton icon="i-lucide-save" size="sm" :loading="savingId === category.id" @click="save(category)">
                保存
              </UButton>
            </td>
          </tr>
          <tr v-if="categories.length === 0">
            <td colspan="6" class="text-center text-slate-500">暂无分类</td>
          </tr>
        </tbody>
      </table>
    </div>
  </UCard>
</template>
