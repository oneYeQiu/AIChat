package com.aichat.controller;

import com.aichat.service.AiBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 机器人控制接口
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiBotService aiBotService;

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", Map.of(
                        "activeMessageEnabled", aiBotService.isActiveMessageEnabled(userId),
                        "onlineUsers", aiBotService.getOnlineUserIds().size()
                )
        ));
    }

    @PostMapping("/toggle-active")
    public ResponseEntity<?> toggleActive(@RequestBody Map<String, Boolean> body,
                                          @RequestAttribute("userId") Long userId) {
        boolean enabled = body.getOrDefault("enabled", false);
        aiBotService.setActiveMessageEnabled(userId, enabled);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", enabled ? "小A主动消息已开启" : "小A主动消息已关闭",
                "data", Map.of("activeMessageEnabled", enabled)
        ));
    }

    @PostMapping("/send-now")
    public ResponseEntity<?> sendNow() {
        aiBotService.sendActiveMessage();
        return ResponseEntity.ok(Map.of("code", 200, "message", "已手动触发主动消息发送"));
    }
}
