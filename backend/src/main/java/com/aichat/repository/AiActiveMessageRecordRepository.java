package com.aichat.repository;

import com.aichat.entity.AiActiveMessageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface AiActiveMessageRecordRepository extends JpaRepository<AiActiveMessageRecord, Long> {

    @Query("SELECT COUNT(a) FROM AiActiveMessageRecord a WHERE a.user.id = :userId AND a.sendTime >= :since")
    long countByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
