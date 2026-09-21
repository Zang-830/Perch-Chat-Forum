import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi } from '../api/community'
import type { User } from '../types'

function loadUser(): User | null {
  const raw = localStorage.getItem('campus_user')
  if (!raw) return null
  try {
    return JSON.parse(raw) as User
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('campus_token'))
  const user = ref<User | null>(loadUser())
  const isAuthenticated = computed(() => Boolean(token.value && user.value))

  function persist(nextToken: string, nextUser: User) {
    token.value = nextToken
    user.value = nextUser
    localStorage.setItem('campus_token', nextToken)
    localStorage.setItem('campus_user', JSON.stringify(nextUser))
  }

  async function login(username: string, password: string) {
    const data = await authApi.login({ username, password })
    persist(data.token, data.user)
  }

  async function register(username: string, nickname: string, password: string) {
    const data = await authApi.register({ username, nickname, password })
    persist(data.token, data.user)
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('campus_token')
    localStorage.removeItem('campus_user')
  }

  return { token, user, isAuthenticated, login, register, logout }
})

