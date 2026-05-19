package com.aichat.service;

import com.aichat.dto.MessageDTO;
import com.aichat.dto.SendMessageRequest;
import com.aichat.entity.Message;
import com.aichat.entity.User;
import com.aichat.repository.MessageRepository;
import com.aichat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 发送消息（用户发送）
     */
    @Transactional
    public MessageDTO sendMessage(Long senderId, SendMessageRequest request) {
        User sender = userRepository.findById(senderId).orElseThrow();
        User receiver = userRepository.findById(request.getReceiverId()).orElseThrow();

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : "TEXT")
                .isRead(false)
                .build();
        message = messageRepository.save(message);

        MessageDTO dto = toDTO(message);

        // 推送消息给接收者
        messagingTemplate.convertAndSendToUser(
                receiver.getId().toString(),
                "/queue/messages",
                dto
        );
        // 也推送给发送者自己，让他的界面实时更新
        messagingTemplate.convertAndSendToUser(
                sender.getId().toString(),
                "/queue/messages",
                dto
        );

        return dto;
    }

    /**
     * 保存 AI 消息并推送
     */
    @Transactional
    public MessageDTO saveAndPushAiMessage(User aiBot, User targetUser, String content) {
        Message message = Message.builder()
                .sender(aiBot)
                .receiver(targetUser)
                .content(content)
                .type("TEXT")
                .isRead(false)
                .build();
        message = messageRepository.save(message);

        MessageDTO dto = toDTO(message);
        messagingTemplate.convertAndSendToUser(
                targetUser.getId().toString(),
                "/queue/messages",
                dto
        );
        return dto;
    }

    /**
     * 获取历史消息（分页，按时间倒序返回）
     */
    public Page<MessageDTO> getChatHistory(Long userId, Long friendId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return messageRepository.findChatHistory(userId, friendId, pageRequest)
                .map(this::toDTO);
    }

    /**
     * 标记消息为已读
     */
    @Transactional
    public void markAsRead(Long userId, Long senderId) {
        messageRepository.findMessagesFromSender(userId, senderId, PageRequest.of(0, 100))
                .forEach(m -> {
                    if (!m.getIsRead()) {
                        m.setIsRead(true);
                        messageRepository.save(m);
                    }
                });
    }

    private MessageDTO toDTO(Message msg) {
        return MessageDTO.builder()
                .id(msg.getId())
                .senderId(msg.getSender().getId())
                .senderName(msg.getSender().getNickname())
                .receiverId(msg.getReceiver().getId())
                .content(msg.getContent())
                .type(msg.getType())
                .isRead(msg.getIsRead())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
