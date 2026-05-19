package com.aichat.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_active_message_record")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AiActiveMessageRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "msg_id", nullable = false)
    private Message message;

    @Column(name = "send_time")
    @Builder.Default
    private LocalDateTime sendTime = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (sendTime == null) sendTime = LocalDateTime.now();
    }
}
