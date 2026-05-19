package com.aichat.websocket;

import com.aichat.service.AiBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final AiBotService aiBotService;

    @EventListener
    public void handleWebSocketConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal != null) {
            try {
                Long userId = Long.parseLong(principal.getName());
                aiBotService.userOnline(accessor.getSessionId(), userId);
                log.info("用户上线: userId={}, sessionId={}", userId, accessor.getSessionId());
            } catch (NumberFormatException e) {
                log.warn("无法解析用户ID: {}", principal.getName());
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        aiBotService.userOffline(accessor.getSessionId());
        log.info("用户下线: sessionId={}", accessor.getSessionId());
    }
}
