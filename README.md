# 💬 AIChat - 微信风格网页版即时通讯

> 一个高仿微信界面的网页版聊天应用，内置 AI 聊天机器人好友，支持语音消息。

**在线演示 / 代码仓库**：https://github.com/oneYeQiu/AIChat

---

## 📋 目录

- [项目概述](#-项目概述)
- [功能一览](#-功能一览)
- [技术栈](#-技术栈)
- [系统架构](#-系统架构)
- [功能详解](#-功能详解)
- [数据库设计](#-数据库设计)
- [API 文档](#-api-文档)
- [快速启动](#-快速启动)
- [项目结构](#-项目结构)
- [配置说明](#-配置说明)
- [开发历程](#-开发历程)
- [License](#-license)

---

## 🚀 项目概述

AIChat 是一个**全栈即时通讯应用**，完全从零搭建，前端仿微信界面，后端实现完整的用户系统、好友管理、实时聊天和 AI 对话功能。

核心亮点：
- **微信风格 UI** — 左侧好友列表 + 右侧聊天窗口，绿色气泡、红点未读计数
- **实时聊天** — 基于 WebSocket (STOMP) 的即时消息推送
- **AI 好友「小A」** — 接入 DeepSeek 大模型，能回复消息、能主动打招呼
- **语音消息** — 浏览器录音，上传后以语音气泡形式发送和播放
- **MySQL 持久化** — 用户、消息、好友关系全部存储于数据库

---

## ✨ 功能一览

### 基础功能

| 功能 | 说明 |
|------|------|
| 🔐 用户注册/登录 | JWT 认证，密码 BCrypt 加密存储 |
| 👥 搜索用户 | 按用户名/昵称搜索，排除自己和已好友 |
| 🤝 添加好友 | 发送请求 → 同意/拒绝，双向好友关系 |
| 💬 实时文字聊天 | WebSocket 推送，消息即时到达 |
| 🎤 语音消息 | 浏览器麦克风录音，60秒限制，自动发送 |
| 🔴 未读计数 | 好友列表红点显示未读消息数 |
| 📱 微信风格 UI | 左侧列表、右侧聊天、绿色气泡等 |

### AI 特色功能

| 功能 | 说明 |
|------|------|
| 🤖 AI 好友「小A」 | 新用户注册后自动添加为好友 |
| 💬 被动回复 | 给「小A」发消息，自动调用大模型生成回复 |
| 📢 主动消息 | 定时扫描在线用户，随机发送问候/分享 |
| ⚡ 手动触发 | 点击按钮让「小A」立刻发一条消息 |
| 🔕 按用户开关 | 每个用户独立控制是否接收主动消息 |

### 技术特性

- 每个浏览器 Tab 独立登录（`sessionStorage`）
- WebSocket 断线自动重连
- AI 调用失败时自动降级为预设回复
- 静默时段自动停止主动消息
- 每日主动消息数限制

---

## 🛠 技术栈

| 层级 | 技术 | 用途 |
|------|------|------|
| **前端框架** | Vue 3 (Composition API) | 响应式界面 |
| **状态管理** | Pinia | 全局状态（auth/chat） |
| **路由** | Vue Router 4 | SPA 路由 |
| **构建工具** | Vite 5 | 开发服务器 + 打包 |
| **HTTP 客户端** | Axios | API 请求 + 拦截器 |
| **实时通信** | SockJS + STOMP.js | WebSocket 消息推送 |
| **后端框架** | Spring Boot 3.2 | REST API + WebSocket |
| **语言** | Java 17 / 21 | 后端运行时 |
| **认证** | Spring Security + JWT (jjwt) | 无状态认证 |
| **ORM** | Spring Data JPA (Hibernate) | 数据库访问 |
| **数据库** | MySQL 8.0 | 数据持久化 |
| **AI 集成** | Spring AI (OpenAI 兼容) | 调用 DeepSeek 大模型 |
| **构建** | Maven | 后端依赖管理 |
| **包管理** | npm | 前端依赖管理 |

---

## 🏗 系统架构

### 整体架构

```
┌─────────────────────────────────────────────────────┐
│                   浏览器 (Vue 3)                      │
│  ┌──────────┐  ┌──────────┐  ┌───────────────────┐   │
│  │ 登录/注册 │  │ 聊天界面  │  │ WebSocket 客户端  │   │
│  │  Login.vue│  │ Chat.vue │  │ stomp.js + sockjs│   │
│  └────┬─────┘  └────┬─────┘  └────────┬──────────┘   │
│       │              │                 │              │
│       └──────────────┼─────────────────┘              │
│                      │ HTTP / WebSocket               │
└──────────────────────┼────────────────────────────────┘
                       │
┌──────────────────────┼────────────────────────────────┐
│        Nginx / Vite Proxy (localhost:3000 → :8080)     │
└──────────────────────┼────────────────────────────────┘
                       │
┌──────────────────────┼────────────────────────────────┐
│               Spring Boot (localhost:8080)              │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │ AuthController│  │  MessageCtrl│  │ WebSocket     │   │
│  │ FriendController│ │ AiController│  │ (STOMP)      │   │
│  └──────┬──────┘  └──────┬───────┘  └──────┬───────┘   │
│         │                │                  │           │
│  ┌──────┴────────────────┴──────────────────┴───────┐   │
│  │              Service Layer                        │   │
│  │  UserService / FriendService / MessageService    │   │
│  │  AiBotService (Spring AI + DeepSeek)              │   │
│  └──────────────────────┬───────────────────────────┘   │
│                         │                               │
│  ┌──────────────────────┴───────────────────────────┐   │
│  │              Data Layer (JPA Repository)           │   │
│  └──────────────────────┬───────────────────────────┘   │
│                         │                               │
│  ┌──────────────────────┴───────────────────────────┐   │
│  │                MySQL 8.0                          │   │
│  │  user / friend_relation / message /               │   │
│  │  ai_active_message_record                         │   │
│  └───────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────┘
```

### 数据流

```
【发送消息】
  用户A → 输入消息 → POST /api/messages/send
    → 后端保存消息到数据库
    → WebSocket 推送消息给用户B
    → WebSocket 推送消息给用户A（实时显示）
    → 如果是发给AI → 新线程触发 AI 回复

【AI 主动消息】
  @Scheduled(10s) → AiActiveMessageScheduler
    → 扫描在线用户
    → 检查每人是否开启 + 每日限额
    → 调用 DeepSeek API 生成消息
    → 保存 + WebSocket 推送

【语音消息】
  用户 → 点击🎤 → getUserMedia录音
    → 停止后等待 onstop 回调
    → 上传 blob 到 POST /api/audio/upload
    → 返回文件 URL
    → 发送消息 { content: 音频URL, type: 'VOICE' }
    → 接收方点击▶️ → new Audio(url).play()
```

---

## 📖 功能详解

### 👤 用户模块

- **注册**：用户名（唯一）、密码（BCrypt加密）、昵称（可选）
- **登录**：校验密码 → 签发 JWT Token（24h过期）
- **自动加好友**：注册成功后自动添加 AI 机器人「小A」为好友
- **多 Tab 独立**：使用 `sessionStorage`，每个浏览器窗口独立登录

### 👥 好友模块

- **搜索用户**：`/api/users/search?keyword=xxx`，排除自己和 AI 机器人
- **发送请求**：`POST /api/friends/request`
- **处理请求**：同意（建立双向关系）/ 拒绝
- **好友列表**：展示昵称、头像、最后一条消息摘要、时间、未读数

### 💬 聊天模块

- **实时推送**：STOMP over SockJS WebSocket
- **发送文本**：Enter / 点击发送
- **发送语音**：🎤 录音 → 60秒自动发送 → 上传 → 发送 VOICE 消息
- **消息气泡**：自己绿色靠右，对方灰色靠左
- **历史消息**：分页加载，按时间正序排列
- **未读消息**：红点计数，点击好友自动标记已读
- **自动滚动**：新消息到底部

### 🤖 AI 机器人「小A」

#### 被动回复
当用户给「小A」发消息时，后端自动执行：
1. 查询最近 10 条对话上下文
2. 调用 DeepSeek (deepseek-chat) 生成回复
3. 保存为「小A」发出的消息
4. WebSocket 实时推送给用户

如果 DeepSeek API 调用失败，自动降级为预设的随机回复。

#### 主动消息
定时任务每 10 秒执行一次：
1. 获取所有在线用户（已连接 WebSocket）
2. 随机挑选最多 5 人
3. 检查每人是否开启主动消息开关
4. 检查每日限额（默认 99 条）
5. 调用 DeepSeek 生成问候/分享消息
6. 保存并推送

#### AI 人设
`小A` 被设定为：温暖、有趣、偶尔幽默的朋友。用 emoji 表情，1-3 句话，不暴露自己是 AI。

---

## 🗄 数据库设计

### 表结构

```sql
-- 用户表
CREATE TABLE `user` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(50)  NOT NULL UNIQUE,
  password    VARCHAR(255) NOT NULL,
  nickname    VARCHAR(50)  DEFAULT NULL,
  avatar      VARCHAR(500) DEFAULT '/default-avatar.png',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP
);

-- 好友关系表
CREATE TABLE friend_relation (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT NOT NULL,
  friend_id  BIGINT NOT NULL,
  status     INT    DEFAULT 0,  -- 0=待同意 1=已同意 2=已拒绝
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_friend (user_id, friend_id),
  FOREIGN KEY (user_id)   REFERENCES `user`(id),
  FOREIGN KEY (friend_id) REFERENCES `user`(id)
);

-- 消息表
CREATE TABLE message (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  sender_id   BIGINT   NOT NULL,
  receiver_id BIGINT   NOT NULL,
  content     TEXT     NOT NULL,
  type        VARCHAR(20) DEFAULT 'TEXT',  -- TEXT / VOICE / IMAGE
  is_read     BOOLEAN DEFAULT FALSE,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (sender_id)   REFERENCES `user`(id),
  FOREIGN KEY (receiver_id) REFERENCES `user`(id)
);

-- AI主动消息记录表
CREATE TABLE ai_active_message_record (
  id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id   BIGINT NOT NULL,
  msg_id    BIGINT NOT NULL,
  send_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES `user`(id),
  FOREIGN KEY (msg_id)  REFERENCES message(id)
);
```

> 使用 `spring.jpa.hibernate.ddl-auto: update` 自动建表，无需手动执行上述 SQL。

### 实体关系图

```
User (1) ──── (N) FriendRelation (N) ──── (1) User
  │                                            │
  │                                            │
  └─── (N) Message (sender) ──────────────────┘
  │
  └─── (N) Message (receiver)
  │
  └─── (N) AiActiveMessageRecord
```

---

## 📡 API 文档

### 认证接口

| 方法 | 路径 | 说明 | 请求体 |
|------|------|------|--------|
| POST | `/api/auth/register` | 注册 | `{ username, password, nickname? }` |
| POST | `/api/auth/login` | 登录 | `{ username, password }` |
| GET | `/api/auth/me` | 当前用户 | - |

### 好友接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/users/search?keyword=` | 搜索用户 |
| POST | `/api/friends/request` | 发送好友请求 `{ friendId }` |
| GET | `/api/friends/requests` | 收到的好友请求列表 |
| PUT | `/api/friends/requests/{id}/accept` | 同意请求 |
| PUT | `/api/friends/requests/{id}/reject` | 拒绝请求 |
| GET | `/api/friends` | 好友列表 |

### 消息接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/messages/{friendId}?page=&size=` | 历史消息（分页） |
| POST | `/api/messages/send` | 发送消息 `{ receiverId, content, type? }` |
| PUT | `/api/messages/read/{senderId}` | 标记已读 |

### 语音接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/audio/upload` | 上传音频文件 (multipart) |
| GET | `/api/audio/{filename}` | 获取音频文件 |

### AI 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ai/status` | AI 状态（开关+在线人数） |
| POST | `/api/ai/toggle-active` | 切换主动消息 `{ enabled }` |
| POST | `/api/ai/send-now` | 手动触发一次主动消息 |

### WebSocket

- 端点：`/ws` (SockJS + STOMP)
- 订阅：`/user/queue/messages`
- 认证：CONNECT 帧携带 `Authorization: Bearer <token>`

---

## 🚀 快速启动

### 前置要求

| 工具 | 版本要求 |
|------|---------|
| JDK | 17+ |
| Node.js | 18+ |
| Maven | 3.8+ |
| MySQL | 8.0+（可选，开发可用 H2 内存库） |
| DeepSeek / OpenAI 兼容 API | API Key |

### 1️⃣ 克隆并配置

```bash
git clone https://github.com/oneYeQiu/AIChat.git
cd AIChat
```

### 2️⃣ 配置数据库

编辑 `backend/src/main/resources/application.yml`：

```yaml
# 开发模式：H2 内存数据库（不用装 MySQL）
spring:
  datasource:
    url: jdbc:h2:mem:aichat
    driver-class-name: org.h2.Driver
```

```yaml
# 生产模式：MySQL
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aichat?useSSL=false&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: your_password
```

先创建数据库：
```sql
CREATE DATABASE aichat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3️⃣ 配置 AI（可选，不配也能用降级回复）

```yaml
spring:
  ai:
    openai:
      api-key: sk-your-deepseek-api-key
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
          temperature: 0.8
```

### 4️⃣ 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端运行在 `http://localhost:8080` ✅

### 5️⃣ 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:3000` ✅

### 6️⃣ 使用

1. 打开 `http://localhost:3000` 注册账号
2. 注册后自动添加好友「小A」
3. 和「小A」聊天，它会调用 DeepSeek 回复你
4. 再开一个窗口注册另一个用户，搜索加好友测试聊天
5. 点击侧边栏 🤖 按钮控制小A主动消息，点击 ⚡ 手动触发

---

## 📂 项目结构

```
AIChat/
├── backend/                          # Spring Boot 后端
│   ├── pom.xml                       # Maven 依赖 (Spring Boot 3.2.5)
│   ├── src/main/java/com/aichat/
│   │   ├── AichatApplication.java    # 启动类
│   │   ├── config/
│   │   │   ├── SecurityConfig.java   # Spring Security + JWT 配置
│   │   │   ├── JwtAuthFilter.java    # JWT 认证过滤器
│   │   │   ├── JwtUtil.java          # JWT 工具类（签发/验证）
│   │   │   ├── UserIdFilter.java     # 提取 userId 到 request attribute
│   │   │   ├── DataInitializer.java  # 启动时初始化 AI 机器人账号
│   │   │   ├── WebSocketConfig.java  # STOMP over SockJS 配置
│   │   │   └── WebSocketAuthInterceptor.java  # WS 连接认证
│   │   ├── controller/
│   │   │   ├── AuthController.java   # 注册/登录
│   │   │   ├── UserController.java   # 用户搜索
│   │   │   ├── FriendController.java # 好友管理
│   │   │   ├── MessageController.java# 消息收发
│   │   │   ├── AiController.java     # AI 控制（开关/触发）
│   │   │   └── AudioController.java  # 语音上传/播放
│   │   ├── dto/                      # 数据传输对象 (7个)
│   │   ├── entity/                   # JPA 实体 (4个)
│   │   ├── repository/               # 数据仓库 (4个)
│   │   ├── service/
│   │   │   ├── UserService.java      # 用户业务
│   │   │   ├── FriendService.java    # 好友业务
│   │   │   ├── MessageService.java   # 消息业务 + WebSocket 推送
│   │   │   └── AiBotService.java     # AI 回复 + 主动消息 + DeepSeek
│   │   ├── scheduler/
│   │   │   └── AiActiveMessageScheduler.java  # 定时任务
│   │   └── websocket/
│   │       └── WebSocketEventListener.java    # WS 连接/断开事件
│   └── src/main/resources/
│       └── application.yml           # 全部配置
│
├── frontend/                         # Vue 3 前端
│   ├── package.json                  # 依赖管理
│   ├── vite.config.js                # Vite + 代理配置
│   ├── index.html                    # 入口 HTML
│   └── src/
│       ├── main.js                   # Vue 应用入口
│       ├── App.vue                   # 根组件
│       ├── api/
│       │   └── index.js              # Axios 实例 + 全部 API
│       ├── router/
│       │   └── index.js              # 路由 + 守卫
│       ├── stores/
│       │   ├── auth.js               # 认证状态 (Pinia)
│       │   └── chat.js               # 聊天状态 (Pinia + WebSocket)
│       ├── views/
│       │   ├── LoginView.vue         # 登录页
│       │   ├── RegisterView.vue      # 注册页
│       │   └── ChatView.vue          # 主聊天页（600+行）
│       └── assets/
│           └── styles/
│               └── main.css          # 全局样式
│
├── audio/                            # 运行时：语音文件存储目录
├── query.sql                         # 临时查询脚本
├── .gitignore
├── README.md
└── token.txt                         # 临时令牌文件
```

---

## ⚙️ 配置说明

### 核心配置 (`application.yml`)

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 后端端口 | `8080` |
| `spring.datasource.url` | 数据库连接 URL | `jdbc:h2:mem:aichat` |
| `spring.ai.openai.api-key` | DeepSeek / OpenAI API Key | - |
| `spring.ai.openai.base-url` | API 地址 | `https://api.deepseek.com` |
| `spring.ai.openai.chat.options.model` | 模型名 | `deepseek-chat` |
| `app.jwt.secret` | JWT 签名密钥 | (请修改) |
| `app.jwt.expiration-ms` | Token 过期时间 | `86400000` (24h) |
| `app.ai.active-message.interval-ms` | 主动消息间隔 | `10000` (10秒) |
| `app.ai.active-message.daily-limit` | 每日每人上限 | `99` |
| `app.ai.active-message.quiet-start-hour` | 静默开始 | `24` (禁用) |
| `app.ai.active-message.quiet-end-hour` | 静默结束 | `0` (禁用) |

---

## 📚 开发历程

本项目从零开始构建，记录了一些关键决策和踩过的坑：

### 技术选型

| 决策 | 选择 | 原因 |
|------|------|------|
| 前端框架 | Vue 3 + Composition API | 响应式、组合式 API 更灵活 |
| 状态管理 | Pinia | Vue 3 官方推荐，比 Vuex 简洁 |
| 实时通信 | SockJS + STOMP | 兼容性好，支持回退到 HTTP 长轮询 |
| 后端框架 | Spring Boot 3.2 | 生态成熟，JPA/WebSocket/Security 一站式 |
| AI 集成 | Spring AI (OpenAI 兼容) | 一行配置切换模型，支持 DeepSeek |
| 数据库 | MySQL 8.0 | 生产级，JPA 自动建表 |
| 认证 | JWT (jjwt 0.12) | 无状态，适合前后端分离 |

### 踩坑记录

1. **H2 内存数据库重启丢失数据** → 方案一：改文件模式 `jdbc:h2:file:./data/aichat`，方案二：切换到 MySQL ✅
2. **WebSocket STOMP 连接 403** → WebSocket CONNECT 帧需要校验 JWT，通过 `ChannelInterceptor` 处理 ✅
3. **新线程中 Hibernate LazyInitializationException** → 给 `@Transactional` 加上 `Propagation.REQUIRES_NEW` ✅
4. **sockjs-client 浏览器报错** → Vite 需要 `define: { global: 'globalThis' }` polyfill ✅
5. **语音数据发不出去** → `recorder.stop()` 后音频数据还没写入 `audioChunks`，改为在 `onstop` 回调中处理 ✅
6. **多窗口登录冲突** → `localStorage` 改为 `sessionStorage`，每个标签页独立会话 ✅

---

## 📄 License

MIT License — 自由使用、修改、分发。

---

> 💡 有任何问题或建议欢迎提 [Issue](https://github.com/oneYeQiu/AIChat/issues)！
