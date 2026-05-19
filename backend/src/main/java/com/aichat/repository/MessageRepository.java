package com.aichat.repository;

import com.aichat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
        SELECT m FROM Message m
        WHERE (m.sender.id = :userId AND m.receiver.id = :friendId)
           OR (m.sender.id = :friendId AND m.receiver.id = :userId)
        ORDER BY m.createdAt DESC
    """)
    Page<Message> findChatHistory(@Param("userId") Long userId,
                                   @Param("friendId") Long friendId,
                                   Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.receiver.id = :userId AND m.isRead = false")
    List<Message> findUnreadMessages(@Param("userId") Long userId);

    @Query("""
        SELECT m FROM Message m WHERE m.id IN (
            SELECT MAX(m2.id) FROM Message m2
            WHERE m2.sender.id = :userId OR m2.receiver.id = :userId
            GROUP BY CASE WHEN m2.sender.id = :userId THEN m2.receiver.id ELSE m2.sender.id END
        )
        ORDER BY m.createdAt DESC
    """)
    List<Message> findLastMessages(@Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.sender.id = :friendId AND m.isRead = false")
    long countUnreadBySender(@Param("userId") Long userId, @Param("friendId") Long friendId);

    @Query("SELECT m FROM Message m WHERE m.receiver.id = :userId AND m.sender.id = :senderId ORDER BY m.createdAt DESC")
    Page<Message> findMessagesFromSender(@Param("userId") Long userId,
                                          @Param("senderId") Long senderId,
                                          Pageable pageable);

    Optional<Message> findTopByOrderByIdDesc();
}
