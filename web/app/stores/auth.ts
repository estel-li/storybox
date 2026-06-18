export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: ''
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    init() {
      if (!import.meta.client || this.token) return
      this.token = localStorage.getItem('storybox_admin_token') || ''
    },
    setToken(token: string) {
      this.token = token
      if (import.meta.client) {
        localStorage.setItem('storybox_admin_token', token)
      }
    },
    logout() {
      this.token = ''
      if (import.meta.client) {
        localStorage.removeItem('storybox_admin_token')
      }
    }
  }
})
