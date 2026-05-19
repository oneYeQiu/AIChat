package com.aichat.service;

import com.aichat.entity.AiActiveMessageRecord;
import com.aichat.entity.Message;
import com.aichat.entity.User;
import com.aichat.repository.AiActiveMessageRecordRepository;
import com.aichat.repository.MessageRepository;
import com.aichat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiBotService {

    private final ChatClient.Builder chatClientBuilder;
    private final MessageRepository messageRepository;
    private final MessageService messageService;
    private final UserRepository userRepository;
    private final AiActiveMessageRecordRepository activeRecordRepository;

    @Value("${app.ai.persona}")
    private String persona;

    @Value("${app.ai.active-message.daily-limit:3}")
    private int dailyLimit;

    @Value("${app.ai.active-message.quiet-start-hour:23}")
    private int quietStartHour;

    @Value("${app.ai.active-message.quiet-end-hour:7}")
    private int quietEndHour;

    // 在线用户Session ID集合
    private final ConcurrentHashMap<String, Long> onlineUsers = new ConcurrentHashMap<>();

    // 每个用户的主动消息开关（默认开启）
    private final ConcurrentHashMap<Long, Boolean> userActiveMessageEnabled = new ConcurrentHashMap<>();

    public boolean isActiveMessageEnabled(Long userId) {
        return userActiveMessageEnabled.getOrDefault(userId, true);
    }
    public void setActiveMessageEnabled(Long userId, boolean enabled) {
        userActiveMessageEnabled.put(userId, enabled);
    }

    public void userOnline(String sessionId, Long userId) {
        onlineUsers.put(sessionId, userId);
    }

    public void userOffline(String sessionId) {
        onlineUsers.remove(sessionId);
    }

    public List<Long> getOnlineUserIds() {
        return new ArrayList<>(onlineUsers.values().stream().distinct().toList());
    }

    /**
     * 被动回复：用户向AI发送消息时调用（在新线程中，需要独立事务）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateReply(Long userId) {
        try {
            User aiBot = userRepository.findByUsername("ai_bot").orElse(null);
            if (aiBot == null) return;
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return;

            // 获取最近对话上下文（最近10条），转为时间正序
            List<Message> recentMsgs = new ArrayList<>(
                    messageRepository.findChatHistory(userId, aiBot.getId(), PageRequest.of(0, 10))
                            .getContent());
            Collections.reverse(recentMsgs); // API返回倒序，反转为正序

            String reply = generateChatResponse(user.getNickname(), recentMsgs);
            if (reply != null && !reply.isBlank()) {
                messageService.saveAndPushAiMessage(aiBot, user, reply);
            }
        } catch (Exception e) {
            log.error("AI被动回复生成失败: {}", e.getMessage());
            fallbackReply(userId);
        }
    }

    /**
     * 调用大模型生成回复（使用Spring AI ChatClient fluent API）
     */
    private String generateChatResponse(String userName, List<Message> context) {
        try {
            // 构建对话上下文
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("以下是与用户 ").append(userName).append(" 的最近对话：\n");
            for (Message msg : context) {
                String role = msg.getSender().getUsername().equals("ai_bot") ? "小A" : userName;
                contextBuilder.append(role).append(": ").append(msg.getContent()).append("\n");
            }
            contextBuilder.append("请以小A的身份自然地回复用户的最新消息。");

            String response = chatClientBuilder.build()
                    .prompt()
                    .system(persona)
                    .user(contextBuilder.toString())
                    .call()
                    .content();

            return response;
        } catch (Exception e) {
            log.error("调用大模型失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 降级回复（AI API不可用时）
     */
    private void fallbackReply(Long userId) {
        String[] fallbacks = {
                "哈哈，有意思~ 😄",
                "嗯嗯，我在听呢",
                "好呀好呀！",
                "你说得对 👍",
                "哈哈哈，笑死我了 😂",
                "行，知道了！",
                "不错不错~",
                "我觉得也是",
                "确实如此呢",
                "听起来很好玩！"
        };
        try {
            User aiBot = userRepository.findByUsername("ai_bot").orElse(null);
            User user = userRepository.findById(userId).orElse(null);
            if (aiBot != null && user != null) {
                String reply = fallbacks[(int) (Math.random() * fallbacks.length)];
                messageService.saveAndPushAiMessage(aiBot, user, reply);
            }
        } catch (Exception e) {
            log.error("降级回复也失败了: {}", e.getMessage());
        }
    }

    /**
     * 主动发消息：定时任务调用
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendActiveMessage() {
        try {
            int currentHour = LocalTime.now().getHour();
            if (currentHour >= quietStartHour || currentHour < quietEndHour) {
                log.debug("当前为静默时段，跳过主动消息发送");
                return;
            }

            User aiBot = userRepository.findByUsername("ai_bot").orElse(null);
            if (aiBot == null) return;

            List<Long> onlineIds = getOnlineUserIds();
            if (onlineIds.isEmpty()) return;

            // 随机挑选在线用户
            Collections.shuffle(onlineIds);
            int maxToSend = Math.min(onlineIds.size(), 5);

            for (int i = 0; i < maxToSend; i++) {
                Long targetUserId = onlineIds.get(i);
                try {
                    LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
                    long todayCount = activeRecordRepository.countByUserIdSince(targetUserId, todayStart);
                    if (todayCount >= dailyLimit) continue;

                    // 检查该用户是否关闭了主动消息
                    if (!isActiveMessageEnabled(targetUserId)) continue;

                    User targetUser = userRepository.findById(targetUserId).orElse(null);
                    if (targetUser == null) continue;

                    String activeMsg = generateActiveMessage(targetUser.getNickname());
                    if (activeMsg != null && !activeMsg.isBlank()) {
                        var dto = messageService.saveAndPushAiMessage(aiBot, targetUser, activeMsg);
                        Message saved = messageRepository.findById(dto.getId()).orElse(null);

                        if (saved != null) {
                            activeRecordRepository.save(AiActiveMessageRecord.builder()
                                    .user(targetUser)
                                    .message(saved)
                                    .build());
                        }
                    }
                } catch (Exception e) {
                    log.error("向用户 {} 发送主动消息失败: {}", targetUserId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("主动消息任务执行失败: {}", e.getMessage());
        }
    }

    /**
     * 生成主动消息
     */
    private String generateActiveMessage(String userName) {
        try {
            String prompt = String.format("""
                    请以"小A"的身份，生成一条主动发给朋友%s的问候或分享消息。
                    要求：
                    - 自然随意，像真人朋友发的消息
                    - 1-2句话即可，不要太长
                    - 可以是日常问候、有趣见闻分享、关心等
                    - 偶尔使用emoji
                    - 只说消息内容本身，不要加任何前缀如"小A："之类
                    """, userName);

            String response = chatClientBuilder.build()
                    .prompt()
                    .system(persona)
                    .user(prompt)
                    .call()
                    .content();

            return response;
        } catch (Exception e) {
            log.error("生成主动消息失败: {}", e.getMessage());
        }
        return getRandomActiveFallback();
    }

    private String getRandomActiveFallback() {
        String[] messages = {
                "今天天气不错，你在干嘛呢？☀️",
                "刚看到一个超好笑的视频，分享给你看看 😂",
                "嘿，今天过得怎么样呀？",
                "突然想问你个事儿，你平时喜欢听什么歌？🎵",
                "周末有什么安排吗？",
                "我刚看到一条有趣的新闻，想跟你聊聊",
                "中午好！吃饭了没？🍜",
                "最近工作/学习忙不忙呀？",
                "嘿~ 突然想到你，过来打个招呼 👋",
                "昨晚做了个特别奇怪的梦，改天跟你讲讲 😂"
        };
        return messages[(int) (Math.random() * messages.length)];
    }
}
