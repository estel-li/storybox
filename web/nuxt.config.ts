const apiProxyTarget = process.env.NUXT_API_PROXY_TARGET || 'http://127.0.0.1:8080'

export default defineNuxtConfig({
  ssr: false,
  srcDir: 'app/',
  modules: ['@nuxt/ui', '@pinia/nuxt'],
  css: ['~/assets/css/main.css'],
  compatibilityDate: '2026-06-18',
  devtools: { enabled: true },
  routeRules: {
    '/api/**': {
      proxy: `${apiProxyTarget}/api/**`
    }
  },
  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || ''
    }
  }
})
