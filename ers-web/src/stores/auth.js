import { defineStore } from 'pinia'
import { authApi } from '@/api'

const TOKEN_KEY = 'ers_token'
const USER_KEY = 'ers_user'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readUser()
  }),
  getters: {
    isLogin: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.role === 'ADMIN',
    displayName: (state) => state.user?.realName || state.user?.username || '',
    creditScore: (state) => state.user?.creditScore ?? 100
  },
  actions: {
    persist() {
      if (this.token) localStorage.setItem(TOKEN_KEY, this.token)
      else localStorage.removeItem(TOKEN_KEY)
      if (this.user) localStorage.setItem(USER_KEY, JSON.stringify(this.user))
      else localStorage.removeItem(USER_KEY)
    },
    async login(form) {
      const data = await authApi.login(form)
      this.token = data.token
      this.user = data.user
      this.persist()
      return data
    },
    async register(form) {
      const data = await authApi.register(form)
      this.token = data.token
      this.user = data.user
      this.persist()
      return data
    },
    async refreshMe() {
      const user = await authApi.me()
      this.user = user
      this.persist()
      return user
    },
    async logout() {
      try {
        await authApi.logout()
      } catch {
        // 无状态令牌，忽略登出接口失败
      }
      this.token = ''
      this.user = null
      this.persist()
    }
  }
})
