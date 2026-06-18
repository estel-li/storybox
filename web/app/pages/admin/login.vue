<script setup lang="ts">
definePageMeta({ layout: 'default' })

const auth = useAuthStore()
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')

async function submit() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await useApiFetch<{ token: string }>('/api/admin/login', {
      method: 'POST',
      body: { password: password.value }
    })
    auth.setToken(response.token)
    await navigateTo('/admin')
  } catch {
    errorMessage.value = '密码不正确'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-slate-100 px-4">
    <UCard class="w-full max-w-sm">
      <form class="space-y-4" @submit.prevent="submit">
        <div>
          <h1 class="text-xl font-semibold text-slate-900">老李讲故事</h1>
          <p class="mt-1 text-sm text-slate-500">家长管理后台</p>
        </div>
        <UFormField label="管理员密码">
          <UInput
            v-model="password"
            type="password"
            icon="i-lucide-key-round"
            autocomplete="current-password"
            autofocus
            class="w-full"
          />
        </UFormField>
        <UAlert v-if="errorMessage" color="error" variant="soft" :title="errorMessage" />
        <UButton type="submit" icon="i-lucide-log-in" :loading="loading" block>登录</UButton>
      </form>
    </UCard>
  </div>
</template>
