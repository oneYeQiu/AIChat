package com.aichat.repository;

import com.aichat.entity.FriendRelation;
import com.aichat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FriendRelationRepository extends JpaRepository<FriendRelation, Long> {
    Optional<FriendRelation> findByUserIdAndFriendId(Long userId, Long friendId);
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);

    @Query("SELECT fr FROM FriendRelation fr WHERE fr.friend.id = :userId AND fr.status = 0")
    List<FriendRelation> findPendingRequests(@Param("userId") Long userId);

    @Query("SELECT fr FROM FriendRelation fr WHERE (fr.user.id = :userId OR fr.friend.id = :userId) AND fr.status = 1")
    List<FriendRelation> findAcceptedFriends(@Param("userId") Long userId);

    @Query("SELECT fr FROM FriendRelation fr WHERE fr.user.id = :userId AND fr.friend.id = :friendId AND fr.status = 1")
    Optional<FriendRelation> findAcceptedRelation(@Param("userId") Long userId, @Param("friendId") Long friendId);
}
