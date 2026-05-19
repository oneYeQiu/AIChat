<template>
  <div class="chat-app">
    <!-- 左侧栏 -->
    <div class="sidebar">
      <!-- 用户信息 -->
      <div class="sidebar-header">
        <div class="user-info">
          <div class="avatar">{{ authStore.user?.nickname?.charAt(0) || '👤' }}</div>
          <div class="user-detail">
            <div class="user-name">{{ authStore.user?.nickname }}</div>
            <div class="user-status">{{ chatStore.wsConnected ? '🟢 在线' : '🔴 离线' }}</div>
          </div>
        </div>
        <div class="header-actions">
          <button @click="showSearch = !showSearch" title="添加好友">➕</button>
          <button @click="showRequests = !showRequests" title="好友请求">
            🔔<span v-if="pendingRequests.length" class="badge">{{ pendingRequests.length }}</span>
          </button>
          <button @click="toggleAiActive" :title="aiActive ? '关闭小A主动消息' : '开启小A主动消息'"
                  :style="{ opacity: aiActive ? 1 : 0.5 }">
            {{ aiActive ? '🤖' : '🤖💤' }}
          </button>
          <button @click="triggerAiNow" title="小A立即发一条" :disabled="!aiActive" style="color:#ffd700">⚡</button>
          <button @click="handleLogout" title="退出">🚪</button>
        </div>
      </div>

      <!-- 搜索面板 -->
      <div v-if="showSearch" class="search-panel">
        <div class="search-box">
          <input v-model="searchKeyword" @keyup.enter="handleSearch" placeholder="搜索用户..." />
          <button @click="handleSearch">搜索</button>
        </div>
        <div class="search-results" v-if="searchResults.length">
          <div v-for="u in searchResults" :key="u.id" class="search-item">
            <div class="s-avatar">{{ u.nickname?.charAt(0) }}</div>
            <div class="s-info">
              <div class="s-name">{{ u.nickname }}</div>
              <div class="s-username">@{{ u.username }}</div>
            </div>
            <button @click="handleAddFriend(u.id)" class="add-btn">添加</button>
          </div>
        </div>
        <div v-else-if="searched" class="no-result">未找到用户</div>
      </div>

      <!-- 好友请求面板 -->
      <div v-if="showRequests" class="requests-panel">
        <div class="panel-title">好友请求</div>
        <div v-if="pendingRequests.length === 0" class="no-result">暂无好友请求</div>
        <div v-for="req in pendingRequests" :key="req.id" class="request-item">
          <div class="r-avatar">{{ req.nickname?.charAt(0) }}</div>
          <div class="r-info">
            <div class="r-name">{{ req.nickname }}</div>
            <div class="r-desc">请求添加你为好友</div>
          </div>
          <div class="r-actions">
            <button @click="handleAccept(req.id)" class="btn-accept">同意</button>
            <button @click="handleReject(req.id)" class="btn-reject">拒绝</button>
          </div>
        </div>
      </div>

      <!-- 好友列表 -->
      <div class="friend-list">
        <div v-for="friend in chatStore.friends" :key="friend.id"
             :class="['friend-item', { active: chatStore.currentFriendId === friend.id }]"
             @click="handleSelectFriend(friend.id)">
          <div class="f-avatar">{{ friend.nickname?.charAt(0) || '👤' }}</div>
          <div class="f-info">
            <div class="f-name">
              {{ friend.nickname }}
              <span v-if="friend.username === 'ai_bot'" class="ai-tag">AI</span>
            </div>
            <div class="f-last-msg">{{ friend.lastMessage || '开始聊天吧~' }}</div>
          </div>
          <div class="f-meta">
            <div class="f-time">{{ formatTime(friend.lastMessageTime) }}</div>
            <div v-if="friend.unreadCount" class="unread-badge">{{ friend.unreadCount > 99 ? '99+' : friend.unreadCount }}</div>
          </div>
        </div>
        <div v-if="!chatStore.friends.length" class="no-result">暂无好友，快来添加吧~</div>
      </div>
    </div>

    <!-- 右侧聊天窗口 -->
    <div class="chat-main">
      <template v-if="chatStore.currentFriend">
        <!-- 聊天头部 -->
        <div class="chat-header">
          <div class="ch-avatar">{{ chatStore.currentFriend.nickname?.charAt(0) || '👤' }}</div>
          <div class="ch-name">
            {{ chatStore.currentFriend.nickname }}
            <span v-if="chatStore.currentFriend.username === 'ai_bot'" class="ai-tag">AI</span>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="chat-messages" ref="msgContainer">
          <div v-if="chatStore.messages.length === 0" class="empty-chat">
            <div class="empty-icon">💬</div>
            <p>开始和 {{ chatStore.currentFriend.nickname }} 聊天吧</p>
          </div>
          <div v-for="msg in chatStore.messages" :key="msg.id"
               :class="['message-wrapper', msg.senderId === authStore.user?.id ? 'sent' : 'received']">
            <div class="message-bubble">
              <!-- 语音消息 -->
              <div v-if="msg.type === 'VOICE'" class="voice-msg" @click="playVoice(msg.content, $event)">
                <span class="voice-icon">🎤</span>
                <span class="voice-play-btn">▶</span>
                <span class="voice-duration">语音</span>
              </div>
              <!-- 文字消息 -->
              <div v-else class="msg-content">{{ msg.content }}</div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input" v-if="!isRecording">
          <input v-model="inputText" @keyup.enter="handleSend"
                 :placeholder="'给 ' + chatStore.currentFriend.nickname + ' 发消息...'"
                 type="text" />
          <button @click="startRecording" title="发送语音" class="mic-btn">🎤</button>
          <button @click="handleSend" :disabled="!inputText.trim()">发送</button>
        </div>
        <!-- 录音中 -->
        <div class="chat-input recording" v-else>
          <div class="recording-indicator">
            <span class="rec-dot"></span>
            <span class="rec-time">{{ formatRecTime(recSeconds) }}</span>
          </div>
          <div class="rec-actions">
            <button @click="cancelRecording" class="rec-cancel">✕ 取消</button>
            <button @click="stopRecording" class="rec-send">✓ 发送</button>
          </div>
        </div>
      </template>
      <template v-else>
        <div class="no-chat">
          <div class="no-chat-icon">💬</div>
          <h2>欢迎使用 AIChat</h2>
          <p>选择一个好友开始聊天</p>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useChatStore } from '../stores/chat'
import { friendsAPI, aiAPI, audioAPI, messagesAPI } from '../api'

const route = useRoute()
const authStore = useAuthStore()
const chatStore = useChatStore()

const inputText = ref('')
const msgContainer = ref(null)
const showSearch = ref(false)
const showRequests = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])
const searched = ref(false)
const pendingRequests = ref([])
const aiActive = ref(true)

// 语音录音
const isRecording = ref(false)
const mediaRecorder = ref(null)
const audioChunks = ref([])
const recSeconds = ref(0)
let recTimer = null
let recAutoTimer = null

// 播放语音
function playVoice(url, event) {
  const btn = event.currentTarget.querySelector('.voice-play-btn')
  const audio = new Audio(url)
  btn.textContent = '⏹'
  audio.onended = () => { btn.textContent = '▶' }
  audio.play().catch(() => { btn.textContent = '▶' })
}

// 开始录音
async function startRecording() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    const recorder = new MediaRecorder(stream, { mimeType: 'audio/webm' })
    mediaRecorder.value = recorder
    audioChunks.value = []
    recorder.ondataavailable = e => { if (e.data.size > 0) audioChunks.value.push(e.data) }
    recorder.start()
    isRecording.value = true
    recSeconds.value = 0
    recTimer = setInterval(() => { recSeconds.value++ }, 1000)
    // 60秒自动发送
    recAutoTimer = setTimeout(() => {
      if (isRecording.value) sendVoiceNow()
    }, 60000)
  } catch (e) {
    alert('无法访问麦克风: ' + (e.message || '请检查权限'))
  }
}

// 实际处理录音数据和发送
function sendVoiceNow() {
  const recorder = mediaRecorder.value
  if (!recorder) return
  recorder.onstop = async () => {
    clearInterval(recTimer)
    clearTimeout(recAutoTimer)
    isRecording.value = false
    const blob = new Blob(audioChunks.value, { type: 'audio/webm' })
    if (blob.size < 200) return
    try {
      const uploadRes = await audioAPI.upload(blob)
      const audioUrl = uploadRes.data.url
      const res = await messagesAPI.send({
        receiverId: chatStore.currentFriendId.value,
        content: audioUrl,
        type: 'VOICE'
      })
      const msg = res.data
      if (msg && msg.id && !chatStore.messages.find(m => m.id === msg.id)) {
        chatStore.messages.push(msg)
        setTimeout(() => {
          const el = document.querySelector('.chat-messages')
          if (el) el.scrollTop = el.scrollHeight
        }, 50)
      }
    } catch (e) {
      console.error('语音发送失败', e)
    }
  }
  recorder.stop()
  recorder.stream.getTracks().forEach(t => t.stop())
}

// 停止录音并发送
async function stopRecording() {
  if (!mediaRecorder.value) return
  sendVoiceNow()
}

// 取消录音
function cancelRecording() {
  const recorder = mediaRecorder.value
  if (recorder) {
    recorder.ondataavailable = null
    recorder.onstop = null
    try { recorder.stop() } catch(e) {}
    recorder.stream.getTracks().forEach(t => t.stop())
  }
  clearInterval(recTimer)
  clearTimeout(recAutoTimer)
  isRecording.value = false
  audioChunks.value = []
}

function formatRecTime(seconds) {
  const m = Math.floor(seconds / 60); const s = seconds % 60
  return m + ':' + (s < 10 ? '0' : '') + s
}

// 加载AI状态
onMounted(async () => {
  await chatStore.loadFriends()
  chatStore.connectWebSocket()
  await loadRequests()
  await loadAiStatus()

  // 如果有路由参数，选中对应好友
  const friendId = route.params.friendId
  if (friendId) {
    chatStore.selectFriend(parseInt(friendId))
  }
})

onUnmounted(() => {
  chatStore.disconnectWebSocket()
})

// 消息列表自动滚动
watch(() => chatStore.messages.length, async () => {
  await nextTick()
  const el = msgContainer.value
  if (el) el.scrollTop = el.scrollHeight
})

// 切换好友
async function handleSelectFriend(friendId) {
  await chatStore.selectFriend(friendId)
  await loadRequests()
}

// 发送消息
async function handleSend() {
  if (!inputText.value.trim()) return
  try {
    await chatStore.sendMessage(inputText.value)
    inputText.value = ''
  } catch (e) {
    console.error('发送失败', e)
  }
}

// 搜索用户
async function handleSearch() {
  if (!searchKeyword.value.trim()) return
  try {
    const res = await friendsAPI.search(searchKeyword.value.trim())
    searchResults.value = res.data || []
    searched.value = true
  } catch (e) {
    console.error('搜索失败', e)
  }
}

// 添加好友
async function handleAddFriend(friendId) {
  try {
    await friendsAPI.sendRequest(friendId)
    alert('好友请求已发送')
  } catch (e) {
    alert(e.response?.data?.message || '发送失败')
  }
}

// 加载好友请求
async function loadRequests() {
  try {
    const res = await friendsAPI.getRequests()
    pendingRequests.value = res.data || []
  } catch (e) {
    console.error('加载请求失败', e)
  }
}

// 同意/拒绝好友请求
async function handleAccept(id) {
  try {
    await friendsAPI.acceptRequest(id)
    await loadRequests()
    await chatStore.loadFriends()
  } catch (e) {
    alert('操作失败')
  }
}
async function handleReject(id) {
  try {
    await friendsAPI.rejectRequest(id)
    await loadRequests()
  } catch (e) {
    alert('操作失败')
  }
}

// 退出登录
function handleLogout() {
  chatStore.disconnectWebSocket()
  authStore.logout()
  window.location.href = '/login'
}

// AI主动消息开关
async function loadAiStatus() {
  try {
    const res = await aiAPI.getStatus()
    aiActive.value = res.data.activeMessageEnabled ?? true
  } catch (e) { /* ignore */ }
}

async function toggleAiActive() {
  aiActive.value = !aiActive.value
  try {
    await aiAPI.toggleActive(aiActive.value)
  } catch (e) {
    aiActive.value = !aiActive.value
    alert('切换失败')
  }
}

async function triggerAiNow() {
  try {
    await aiAPI.sendNow()
    console.log('手动触发AI消息发送')
  } catch (e) {
    console.error('触发失败', e?.response?.data?.message || e.message)
  }
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
</script>

<style scoped>
/* ===== 整体布局 ===== */
.chat-app {
  display: flex;
  height: 100vh;
  background: #f5f5f5;
}
.sidebar {
  width: 320px;
  min-width: 320px;
  background: #2e2e2e;
  color: #fff;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #3a3a3a;
}

/* ===== 侧边栏头部 ===== */
.sidebar-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #3a3a3a;
}
.user-info { display: flex; align-items: center; gap: 10px; }
.avatar {
  width: 40px; height: 40px; border-radius: 6px;
  background: #07c160; display: flex; align-items: center;
  justify-content: center; font-size: 18px; color: #fff;
}
.user-name { font-size: 15px; font-weight: 500; }
.user-status { font-size: 12px; color: #aaa; }
.header-actions { display: flex; gap: 8px; }
.header-actions button {
  background: none; border: none; color: #ccc;
  font-size: 18px; cursor: pointer; position: relative;
}
.header-actions button:hover { color: #fff; }
.badge {
  position: absolute; top: -4px; right: -8px;
  background: #e74c3c; color: #fff; font-size: 10px;
  padding: 1px 5px; border-radius: 10px;
}

/* ===== 搜索面板 ===== */
.search-panel, .requests-panel {
  padding: 12px 16px; border-bottom: 1px solid #3a3a3a;
  background: #252525;
}
.search-box { display: flex; gap: 8px; }
.search-box input {
  flex: 1; padding: 8px 12px; border: 1px solid #555;
  border-radius: 6px; background: #3a3a3a; color: #fff;
  font-size: 13px; outline: none;
}
.search-box button {
  padding: 8px 16px; background: #07c160; color: #fff;
  border: none; border-radius: 6px; cursor: pointer; font-size: 13px;
}
.search-results { max-height: 200px; overflow-y: auto; margin-top: 8px; }
.search-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px; border-radius: 6px;
}
.search-item:hover { background: #3a3a3a; }
.s-avatar {
  width: 36px; height: 36px; border-radius: 6px;
  background: #07c160; display: flex; align-items: center;
  justify-content: center; font-size: 15px; color: #fff;
}
.s-info { flex: 1; }
.s-name { font-size: 14px; }
.s-username { font-size: 11px; color: #999; }
.add-btn {
  padding: 4px 12px; background: #07c160; color: #fff;
  border: none; border-radius: 4px; cursor: pointer; font-size: 12px;
}
.no-result { padding: 20px; text-align: center; color: #999; font-size: 13px; }

/* ===== 好友请求 ===== */
.panel-title { font-size: 14px; font-weight: 500; margin-bottom: 8px; }
.request-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 0; border-bottom: 1px solid #3a3a3a;
}
.r-avatar {
  width: 36px; height: 36px; border-radius: 6px;
  background: #576b95; display: flex; align-items: center;
  justify-content: center; font-size: 15px; color: #fff;
}
.r-info { flex: 1; }
.r-name { font-size: 14px; }
.r-desc { font-size: 11px; color: #999; }
.r-actions { display: flex; gap: 6px; }
.btn-accept {
  padding: 4px 10px; background: #07c160; color: #fff;
  border: none; border-radius: 4px; cursor: pointer; font-size: 12px;
}
.btn-reject {
  padding: 4px 10px; background: #666; color: #fff;
  border: none; border-radius: 4px; cursor: pointer; font-size: 12px;
}

/* ===== 好友列表 ===== */
.friend-list { flex: 1; overflow-y: auto; }
.friend-item {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px; cursor: pointer; transition: background 0.15s;
}
.friend-item:hover { background: #3a3a3a; }
.friend-item.active { background: #3a3a3a; }
.f-avatar {
  width: 44px; height: 44px; border-radius: 6px;
  background: #07c160; display: flex; align-items: center;
  justify-content: center; font-size: 18px; color: #fff; flex-shrink: 0;
}
.f-info { flex: 1; min-width: 0; }
.f-name { font-size: 15px; margin-bottom: 4px; }
.ai-tag {
  font-size: 10px; background: #576b95; color: #fff;
  padding: 1px 4px; border-radius: 3px; margin-left: 4px;
}
.f-last-msg {
  font-size: 12px; color: #999; white-space: nowrap;
  overflow: hidden; text-overflow: ellipsis;
}
.f-meta { text-align: right; }
.f-time { font-size: 11px; color: #999; margin-bottom: 4px; }
.unread-badge {
  display: inline-block; background: #e74c3c; color: #fff;
  font-size: 11px; padding: 2px 6px; border-radius: 10px;
  min-width: 18px; text-align: center;
}

/* ===== 右侧聊天区域 ===== */
.chat-main {
  flex: 1; display: flex; flex-direction: column;
  background: #f5f5f5;
}
.chat-header {
  display: flex; align-items: center; gap: 12px;
  padding: 16px 20px; background: #ededed;
  border-bottom: 1px solid #d9d9d9;
}
.ch-avatar {
  width: 38px; height: 38px; border-radius: 6px;
  background: #07c160; display: flex; align-items: center;
  justify-content: center; font-size: 16px; color: #fff;
}
.ch-name { font-size: 16px; font-weight: 500; }

.chat-messages {
  flex: 1; overflow-y: auto; padding: 20px;
  display: flex; flex-direction: column; gap: 16px;
}
.empty-chat { text-align: center; margin-top: 120px; color: #999; }
.empty-icon { font-size: 48px; margin-bottom: 12px; }

.message-wrapper { display: flex; }
.message-wrapper.sent { justify-content: flex-end; }
.message-wrapper.received { justify-content: flex-start; }
.message-bubble {
  max-width: 60%; padding: 10px 14px; border-radius: 8px;
  font-size: 15px; line-height: 1.5; word-break: break-word;
}
.sent .message-bubble { background: #95ec69; color: #000; }
.received .message-bubble { background: #fff; color: #000; }
.msg-content { white-space: pre-wrap; }

.chat-input {
  display: flex; gap: 12px; padding: 16px 20px;
  background: #f5f5f5; border-top: 1px solid #d9d9d9;
}
.chat-input input {
  flex: 1; padding: 10px 16px; border: 1px solid #d9d9d9;
  border-radius: 8px; font-size: 15px; outline: none;
  transition: border-color 0.2s;
}
.chat-input input:focus { border-color: #07c160; }
.chat-input button {
  padding: 10px 24px; background: #07c160; color: #fff;
  border: none; border-radius: 8px; font-size: 15px; cursor: pointer;
}
.chat-input button:hover { opacity: 0.9; }
.chat-input button:disabled { opacity: 0.5; cursor: not-allowed; }
.chat-input .mic-btn { padding: 10px 14px; font-size: 18px; background: #e0e0e0; }
.chat-input .mic-btn:hover { background: #07c160; }

/* 录音状态 */
.chat-input.recording {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; background: #fff5f5;
}
.recording-indicator { display: flex; align-items: center; gap: 10px; }
.rec-dot {
  width: 12px; height: 12px; border-radius: 50%; background: #e74c3c;
  animation: pulse 1s infinite;
}
@keyframes pulse { 0% { opacity: 1; } 50% { opacity: 0.3; } 100% { opacity: 1; } }
.rec-time { font-size: 16px; font-weight: 500; color: #e74c3c; }
.rec-actions { display: flex; gap: 8px; }
.rec-cancel { padding: 8px 16px; background: #e0e0e0; border: none; border-radius: 6px; cursor: pointer; }
.rec-send { padding: 8px 16px; background: #07c160; color: #fff; border: none; border-radius: 6px; cursor: pointer; }

/* 语音消息气泡 */
.voice-msg {
  display: flex; align-items: center; gap: 8px; cursor: pointer;
  min-width: 100px;
}
.voice-msg:hover { opacity: 0.9; }
.voice-icon { font-size: 18px; }
.voice-play-btn { font-size: 14px; color: #07c160; font-weight: bold; }
.voice-duration { font-size: 12px; color: #999; }

.no-chat {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; color: #999;
}
.no-chat-icon { font-size: 64px; margin-bottom: 16px; }
.no-chat h2 { font-size: 22px; margin-bottom: 8px; color: #666; }
</style>
