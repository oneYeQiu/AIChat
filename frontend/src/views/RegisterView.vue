<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="app-title">💬 AIChat</h1>
      <p class="app-subtitle">创建新账号</p>
      <form @submit.prevent="handleRegister" class="login-form">
        <input v-model="username" type="text" placeholder="用户名 (3-50位)" required />
        <input v-model="nickname" type="text" placeholder="昵称（可选）" />
        <input v-model="password" type="password" placeholder="密码 (6位以上)" required />
        <input v-model="password2" type="password" placeholder="确认密码" required />
        <p v-if="error" class="error-msg">{{ error }}</p>
        <button type="submit" :disabled="loading">
          {{ loading ? '注册中...' : '注 册' }}
        </button>
      </form>
      <p class="switch-link">
        已有账号？<router-link to="/login">立即登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const username = ref('')
const nickname = ref('')
const password = ref('')
const password2 = ref('')
const loading = ref(false)
const error = ref('')

async function handleRegister() {
  error.value = ''
  if (password.value !== password2.value) {
    error.value = '两次密码不一致'
    return
  }
  if (password.value.length < 6) {
    error.value = '密码至少6位'
    return
  }
  loading.value = true
  try {
    await authStore.register(username.value, password.value, nickname.value || undefined)
    router.push('/chat')
  } catch (e) {
    error.value = e.response?.data?.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container { display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #f5f5f5; }
.login-card { background: #fff; padding: 40px; border-radius: 12px; box-shadow: 0 2px 20px rgba(0,0,0,0.08); width: 380px; text-align: center; }
.app-title { font-size: 32px; color: #07c160; margin-bottom: 4px; }
.app-subtitle { color: #999; font-size: 14px; margin-bottom: 32px; }
.login-form { display: flex; flex-direction: column; gap: 16px; }
.login-form input { padding: 12px 16px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 15px; outline: none; transition: border-color 0.2s; }
.login-form input:focus { border-color: #07c160; }
.login-form button { padding: 12px; background: #07c160; color: #fff; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; }
.login-form button:hover { opacity: 0.9; }
.login-form button:disabled { opacity: 0.6; cursor: not-allowed; }
.error-msg { color: #e74c3c; font-size: 14px; text-align: left; }
.switch-link { margin-top: 20px; font-size: 14px; color: #999; }
.switch-link a { color: #07c160; text-decoration: none; }
</style>
