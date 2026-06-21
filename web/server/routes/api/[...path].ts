import { createError, getRequestURL, proxyRequest } from 'h3'
import { joinURL } from 'ufo'

function apiProxyTarget() {
  const configured = process.env.NUXT_API_PROXY_TARGET?.trim()
  if (configured) return configured.replace(/\/$/, '')

  const backendPort = process.env.STORY_SERVER_PORT?.trim() || '8080'
  return `http://127.0.0.1:${backendPort}`
}

export default defineEventHandler((event) => {
  const url = getRequestURL(event)
  const apiPath = url.pathname.replace(/^\/api\/?/, '')
  const target = joinURL(apiProxyTarget(), 'api', apiPath) + url.search

  if (!/^https?:\/\//.test(target)) {
    throw createError({
      statusCode: 500,
      statusMessage: 'Invalid API proxy target'
    })
  }

  return proxyRequest(event, target)
})
