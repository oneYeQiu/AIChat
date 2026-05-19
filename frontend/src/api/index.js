import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器：添加 token
api.interceptors.request.use(config => {
  const token = sessionStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：处理 401
api.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api

// Auth API
export const authAPI = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  getMe: () => api.get('/auth/me')
}

// Friends API
export const friendsAPI = {
  getList: () => api.get('/friends'),
  search: (keyword) => api.get('/users/search', { params: { keyword } }),
  sendRequest: (friendId) => api.post('/friends/request', { friendId }),
  getRequests: () => api.get('/friends/requests'),
  acceptRequest: (id) => api.put(`/friends/requests/${id}/accept`),
  rejectRequest: (id) => api.put(`/friends/requests/${id}/reject`)
}

// Messages API
export const messagesAPI = {
  getHistory: (friendId, page = 0, size = 20) =>
    api.get(`/messages/${friendId}`, { params: { page, size } }),
  send: (data) => api.post('/messages/send', data),
  markRead: (senderId) => api.put(`/messages/read/${senderId}`)
}

// Audio API
export const audioAPI = {
  upload: (blob) => {
    const form = new FormData()
    form.append('file', blob, 'voice.webm')
    return api.post('/audio/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

// AI API
export const aiAPI = {
  getStatus: () => api.get('/ai/status'),
  toggleActive: (enabled) => api.post('/ai/toggle-active', { enabled }),
  sendNow: () => api.post('/ai/send-now')
}
