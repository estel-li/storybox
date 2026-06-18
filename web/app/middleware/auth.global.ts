export default defineNuxtRouteMiddleware((to) => {
  if (!to.path.startsWith('/admin') || to.path === '/admin/login') {
    return
  }

  const auth = useAuthStore()
  auth.init()
  if (!auth.isLoggedIn) {
    return navigateTo('/admin/login')
  }
})
