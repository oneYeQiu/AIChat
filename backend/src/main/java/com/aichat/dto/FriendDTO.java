package com.aichat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendDTO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private long unreadCount;
}
