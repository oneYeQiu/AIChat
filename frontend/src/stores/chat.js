import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { friendsAPI, messagesAPI } from '../api'
import { useAuthStore } from './auth'

export const useChatStore = defineStore('chat', () => {
  const friends = ref([])
  const currentFriendId = ref(null)
  const messages = ref([])
  const stompClient = ref(null)
  const wsConnected = ref(false)

  const currentFriend = computed(() =>
    friends.value.find(f => f.id === currentFriendId.value)
  )

  async function loadFriends() {
    try {
      const res = await friendsAPI.getList()
      friends.value = res.data || []
    } catch (e) {
      console.error('加载好友列表失败:', e)
    }
  }

  async function loadMessages(friendId, page = 0) {
    try {
      const res = await messagesAPI.getHistory(friendId, page)
      const data = res.data
      const msgs = (data.content || []).reverse()
      if (page === 0) {
        messages.value = msgs
        await messagesAPI.markRead(friendId)
        await loadFriends()
      } else {
        messages.value = [...msgs, ...messages.value]
      }
    } catch (e) {
      console.error('加载消息失败:', e)
    }
  }

  // 发送消息，返回的 DTO 立即添加到消息列表
  async function sendMessage(content) {
    if (!currentFriendId.value || !content.trim()) return
    const authStore = useAuthStore()
    try {
      const res = await messagesAPI.send({
        receiverId: currentFriendId.value,
        content: content.trim()
      })
      const msg = res.data
      if (msg && msg.id) {
        // 只在 WebSocket 还没推送过来时才添加（避免重复）
        if (!messages.value.find(m => m.id === msg.id)) {
          messages.value.push(msg)
          setTimeout(scrollToBottom, 50)
        }
      }
    } catch (e) {
      console.error('发送消息失败:', e)
      // 乐观添加：即使 API 失败也先显示
      const tempMsg = {
        id: Date.now(),
        senderId: authStore.user?.id,
        senderName: authStore.user?.nickname,
        receiverId: currentFriendId.value,
        content: content.trim(),
        type: 'TEXT',
        isRead: false,
        createdAt: new Date().toISOString()
      }
      if (!messages.value.find(m => m.id === tempMsg.id)) {
        messages.value.push(tempMsg)
        setTimeout(scrollToBottom, 50)
      }
      throw e
    }
  }

  async function selectFriend(friendId) {
    currentFriendId.value = friendId
    messages.value = []
    await loadMessages(friendId)
  }

  function scrollToBottom() {
    const el = document.querySelector('.chat-messages')
    if (el) el.scrollTop = el.scrollHeight
  }

  function handleIncomingMessage(msg) {
    // 防止重复消息
    if (msg.id && messages.value.some(m => m.id === msg.id)) return
    // 只显示与当前聊天相关的消息
    if (currentFriendId.value === msg.senderId || currentFriendId.value === msg.receiverId) {
      messages.value.push(msg)
      setTimeout(scrollToBottom, 50)
    }
    // 更新好友列表摘要
    const friendIdx = friends.value.findIndex(f => f.id === msg.senderId || f.id === msg.receiverId)
    if (friendIdx >= 0) {
      friends.value[friendIdx].lastMessage = msg.content
      friends.value[friendIdx].lastMessageTime = msg.createdAt
      const authStore = useAuthStore()
      if (msg.senderId !== authStore.user?.id && msg.senderId !== currentFriendId.value) {
        friends.value[friendIdx].unreadCount = (friends.value[friendIdx].unreadCount || 0) + 1
      }
    }
  }

  // 懒加载 WebSocket
  async function connectWebSocket() {
    if (stompClient.value?.connected) return
    try {
      const [stompModule, sockModule] = await Promise.all([
        import('@stomp/stompjs'),
        import('sockjs-client')
      ])
      const { Client } = stompModule
      // sockjs-client CJS module: the constructor might be at .default or directly
      const SockJSClass = sockModule.default || sockModule

      const token = sessionStorage.getItem('token')
      const socket = new SockJSClass('/ws')
      const client = new Client({
        webSocketFactory: () => socket,
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,
        onConnect: () => {
          console.log('✅ WebSocket 已连接')
          wsConnected.value = true
          client.subscribe('/user/queue/messages', (message) => {
            try {
              const msg = JSON.parse(message.body)
              handleIncomingMessage(msg)
            } catch (e) {
              console.warn('消息解析失败:', e)
            }
          })
        },
        onDisconnect: () => {
          console.log('🔌 WebSocket 已断开')
          wsConnected.value = false
        },
        onStompError: (frame) => {
          console.error('STOMP 错误:', frame)
          wsConnected.value = false
        }
      })
      client.activate()
      stompClient.value = client
    } catch (e) {
      console.warn('WebSocket 连接失败，将使用 HTTP 轮询:', e.message)
      wsConnected.value = false
    }
  }

  function disconnectWebSocket() {
    try { stompClient.value?.deactivate() } catch (e) { /* ignore */ }
    wsConnected.value = false
  }

  return {
    friends, currentFriendId, messages, wsConnected, currentFriend,
    loadFriends, loadMessages, sendMessage, selectFriend,
    connectWebSocket, disconnectWebSocket
  }
})
