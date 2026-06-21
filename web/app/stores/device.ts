function createDeviceId() {
  const cryptoApi = globalThis.crypto
  if (typeof cryptoApi?.randomUUID === 'function') {
    return cryptoApi.randomUUID()
  }

  if (typeof cryptoApi?.getRandomValues === 'function') {
    const bytes = cryptoApi.getRandomValues(new Uint8Array(16))
    bytes[6] = (bytes[6]! & 0x0f) | 0x40
    bytes[8] = (bytes[8]! & 0x3f) | 0x80
    const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0'))
    return `${hex.slice(0, 4).join('')}-${hex.slice(4, 6).join('')}-${hex.slice(6, 8).join('')}-${hex.slice(8, 10).join('')}-${hex.slice(10, 16).join('')}`
  }

  return `device-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
}

export const useDeviceStore = defineStore('device', {
  state: () => ({
    deviceId: ''
  }),
  actions: {
    ensure() {
      if (!import.meta.client) return this.deviceId
      const key = 'storybox_device_id'
      let value = localStorage.getItem(key)
      if (!value) {
        value = createDeviceId()
        localStorage.setItem(key, value)
      }
      this.deviceId = value
      return value
    }
  }
})
