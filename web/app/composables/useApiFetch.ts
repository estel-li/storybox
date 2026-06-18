import { $fetch as ofetch, type FetchOptions } from 'ofetch'

type ApiMethod =
  | 'GET'
  | 'POST'
  | 'PUT'
  | 'DELETE'
  | 'PATCH'
  | 'HEAD'
  | 'OPTIONS'
  | 'get'
  | 'post'
  | 'put'
  | 'delete'
  | 'patch'
  | 'head'
  | 'options'

type ApiOptions = Omit<FetchOptions<'json'>, 'method'> & {
  method?: ApiMethod
}

export function useApiFetch<T>(path: string, options: ApiOptions = {}) {
  const config = useRuntimeConfig()
  const auth = useAuthStore()
  auth.init()

  const headers = new Headers(options.headers as HeadersInit)
  if (auth.token) {
    headers.set('Authorization', `Bearer ${auth.token}`)
  }

  return ofetch<T>(path, {
    baseURL: config.public.apiBase,
    ...options,
    headers
  })
}

export function useApiUrl(path: string) {
  const config = useRuntimeConfig()
  const base = String(config.public.apiBase || '').replace(/\/$/, '')
  return `${base}${path}`
}
