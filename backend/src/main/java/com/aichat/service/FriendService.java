package com.aichat.service;

import com.aichat.dto.FriendDTO;
import com.aichat.dto.FriendRequestDTO;
import com.aichat.entity.FriendRelation;
import com.aichat.entity.Message;
import com.aichat.entity.User;
import com.aichat.repository.FriendRelationRepository;
import com.aichat.repository.MessageRepository;
import com.aichat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRelationRepository friendRelationRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    // 搜索用户（排除自己和已是好友的）
    public List<User> searchUsers(String keyword, Long currentUserId) {
        List<User> users = userRepository.searchByKeyword(keyword);
        return users.stream()
                .filter(u -> !u.getId().equals(currentUserId))
                .filter(u -> !"ai_bot".equals(u.getUsername())) // 隐藏AI机器人
                .collect(Collectors.toList());
    }

    // 发送好友请求
    @Transactional
    public void sendFriendRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new RuntimeException("不能添加自己为好友");
        }
        if (friendRelationRepository.existsByUserIdAndFriendId(userId, friendId)) {
            throw new RuntimeException("已发送过好友请求或已是好友");
        }
        User user = userRepository.findById(userId).orElseThrow();
        User friend = userRepository.findById(friendId).orElseThrow();

        FriendRelation relation = FriendRelation.builder()
                .user(user)
                .friend(friend)
                .status(0)
                .build();
        friendRelationRepository.save(relation);
    }

    // 收到的好友请求列表
    public List<FriendRequestDTO> getPendingRequests(Long userId) {
        return friendRelationRepository.findPendingRequests(userId).stream()
                .map(fr -> FriendRequestDTO.builder()
                        .id(fr.getId())
                        .userId(fr.getUser().getId())
                        .username(fr.getUser().getUsername())
                        .nickname(fr.getUser().getNickname())
                        .avatar(fr.getUser().getAvatar())
                        .status(fr.getStatus())
                        .createdAt(fr.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // 同意好友请求
    @Transactional
    public void acceptRequest(Long requestId) {
        FriendRelation fr = friendRelationRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("请求不存在"));
        fr.setStatus(1);
        friendRelationRepository.save(fr);

        // 建立双向好友关系
        if (!friendRelationRepository.existsByUserIdAndFriendId(fr.getFriend().getId(), fr.getUser().getId())) {
            FriendRelation reverse = FriendRelation.builder()
                    .user(fr.getFriend())
                    .friend(fr.getUser())
                    .status(1)
                    .build();
            friendRelationRepository.save(reverse);
        }
    }

    // 拒绝好友请求
    @Transactional
    public void rejectRequest(Long requestId) {
        FriendRelation fr = friendRelationRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("请求不存在"));
        fr.setStatus(2);
        friendRelationRepository.save(fr);
    }

    // 获取好友列表（含最后一条消息摘要）
    public List<FriendDTO> getFriendList(Long userId) {
        List<FriendRelation> relations = friendRelationRepository.findAcceptedFriends(userId);
        Map<Long, FriendDTO> friendMap = new LinkedHashMap<>();

        for (FriendRelation fr : relations) {
            User friend = fr.getUser().getId().equals(userId) ? fr.getFriend() : fr.getUser();
            if (friendMap.containsKey(friend.getId())) continue;

            long unread = messageRepository.countUnreadBySender(userId, friend.getId());
            List<Message> lastMsgs = messageRepository.findChatHistory(
                    userId, friend.getId(),
                    org.springframework.data.domain.PageRequest.of(0, 1)).getContent();
            String lastMsg = lastMsgs.isEmpty() ? "" : lastMsgs.get(0).getContent();
            java.time.LocalDateTime lastTime = lastMsgs.isEmpty() ? null : lastMsgs.get(0).getCreatedAt();

            friendMap.put(friend.getId(), FriendDTO.builder()
                    .id(friend.getId())
                    .username(friend.getUsername())
                    .nickname(friend.getNickname())
                    .avatar(friend.getAvatar())
                    .lastMessage(lastMsg)
                    .lastMessageTime(lastTime)
                    .unreadCount(unread)
                    .build());
        }
        return new ArrayList<>(friendMap.values());
    }
}
