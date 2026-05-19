package com.aichat.controller;

import com.aichat.dto.MessageDTO;
import com.aichat.dto.SendMessageRequest;
import com.aichat.entity.User;
import com.aichat.repository.UserRepository;
import com.aichat.service.AiBotService;
import com.aichat.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final AiBotService aiBotService;
    private final UserRepository userRepository;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestAttribute("userId") Long userId,
                                          @Valid @RequestBody SendMessageRequest request) {
        try {
            MessageDTO msg = messageService.sendMessage(userId, request);
            // 如果发送给 AI 机器人，触发自动回复
            if (isAiBot(request.getReceiverId())) {
                new Thread(() -> aiBotService.generateReply(userId)).start();
            }
            return ResponseEntity.ok(Map.of("code", 200, "data", msg));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @GetMapping("/{friendId}")
    public ResponseEntity<?> getHistory(@RequestAttribute("userId") Long userId,
                                         @PathVariable Long friendId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        Page<MessageDTO> messages = messageService.getChatHistory(userId, friendId, page, size);
        return ResponseEntity.ok(Map.of("code", 200, "data", messages));
    }

    @PutMapping("/read/{senderId}")
    public ResponseEntity<?> markRead(@RequestAttribute("userId") Long userId,
                                       @PathVariable Long senderId) {
        messageService.markAsRead(userId, senderId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "ok"));
    }

    private boolean isAiBot(Long userId) {
        return userRepository.findById(userId)
                .map(u -> "ai_bot".equals(u.getUsername()))
                .orElse(false);
    }
}
