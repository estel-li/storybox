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
        value = crypto.randomUUID()
        localStorage.setItem(key, value)
      }
      this.deviceId = value
      return value
    }
  }
})
