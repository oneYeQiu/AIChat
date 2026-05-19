package com.aichat.scheduler;

import com.aichat.service.AiBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.ai.active-message.enabled", havingValue = "true", matchIfMissing = true)
public class AiActiveMessageScheduler {

    private final AiBotService aiBotService;

    /**
     * 定时执行 AI 主动消息发送任务
     * 使用配置的间隔时间（默认30分钟）
     */
    @Scheduled(fixedDelayString = "${app.ai.active-message.interval-ms:1800000}")
    public void sendActiveMessages() {
        log.info("⏰ 定时任务触发，在线用户: {}", aiBotService.getOnlineUserIds());
        aiBotService.sendActiveMessage();
    }
}
