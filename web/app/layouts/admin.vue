<script setup lang="ts">
const auth = useAuthStore()
const route = useRoute()

const links = [
  { label: '仪表盘', to: '/admin', icon: 'i-lucide-gauge' },
  { label: '扫描管理', to: '/admin/scan', icon: 'i-lucide-scan-search' },
  { label: '分类管理', to: '/admin/categories', icon: 'i-lucide-folder-tree' },
  { label: '专辑管理', to: '/admin/albums', icon: 'i-lucide-library' },
  { label: '故事管理', to: '/admin/stories', icon: 'i-lucide-list-music' },
  { label: '播放记录', to: '/admin/history', icon: 'i-lucide-history' }
]

const title = computed(() => links.find((item) => item.to === route.path)?.label || '老李讲故事')

function logout() {
  auth.logout()
  navigateTo('/admin/login')
}
</script>

<template>
  <div class="min-h-screen bg-slate-50 text-slate-900">
    <aside class="fixed inset-y-0 left-0 hidden w-60 border-r border-slate-200 bg-white px-4 py-5 md:block">
      <div class="mb-6 px-2">
        <p class="text-lg font-semibold">老李讲故事</p>
        <p class="text-sm text-slate-500">家长管理后台</p>
      </div>
      <nav class="space-y-1">
        <UButton
          v-for="link in links"
          :key="link.to"
          :to="link.to"
          :icon="link.icon"
          :color="route.path === link.to ? 'primary' : 'neutral'"
          :variant="route.path === link.to ? 'soft' : 'ghost'"
          block
          class="justify-start"
        >
          {{ link.label }}
        </UButton>
      </nav>
    </aside>

    <section class="min-h-screen md:pl-60">
      <header class="sticky top-0 z-10 border-b border-slate-200 bg-white/95 backdrop-blur">
        <div class="flex h-16 items-center justify-between px-4 md:px-8">
          <div>
            <h1 class="text-xl font-semibold">{{ title }}</h1>
          </div>
          <div class="flex items-center gap-2">
            <UButton to="/player" icon="i-lucide-play" color="neutral" variant="ghost">播放端</UButton>
            <UButton icon="i-lucide-log-out" color="neutral" variant="outline" @click="logout">退出</UButton>
          </div>
        </div>
        <div class="flex gap-2 overflow-x-auto border-t border-slate-100 px-4 py-2 md:hidden">
          <UButton
            v-for="link in links"
            :key="link.to"
            :to="link.to"
            :icon="link.icon"
            :color="route.path === link.to ? 'primary' : 'neutral'"
            :variant="route.path === link.to ? 'soft' : 'ghost'"
            size="sm"
          >
            {{ link.label }}
          </UButton>
        </div>
      </header>

      <div class="px-4 py-6 md:px-8">
        <slot />
      </div>
    </section>
  </div>
</template>
