import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI } from '../api'

const storage = sessionStorage

export const useAuthStore = defineStore('auth', () => {
  const user = ref(JSON.parse(storage.getItem('user') || 'null'))
  const token = ref(storage.getItem('token') || '')

  const isLoggedIn = computed(() => !!token.value)

  async function login(username, password) {
    const res = await authAPI.login({ username, password })
    const data = res.data
    token.value = data.token
    user.value = { id: data.userId, username: data.username, nickname: data.nickname, avatar: data.avatar }
    storage.setItem('token', data.token)
    storage.setItem('user', JSON.stringify(user.value))
    return data
  }

  async function register(username, password, nickname) {
    const res = await authAPI.register({ username, password, nickname })
    const data = res.data
    token.value = data.token
    user.value = { id: data.userId, username: data.username, nickname: data.nickname, avatar: data.avatar }
    storage.setItem('token', data.token)
    storage.setItem('user', JSON.stringify(user.value))
    return data
  }

  function logout() {
    token.value = ''
    user.value = null
    storage.removeItem('token')
    storage.removeItem('user')
  }

  return { user, token, isLoggedIn, login, register, logout }
})
