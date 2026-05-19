package com.aichat.controller;

import com.aichat.dto.FriendDTO;
import com.aichat.dto.FriendRequestDTO;
import com.aichat.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(@RequestAttribute("userId") Long userId,
                                          @RequestBody Map<String, Long> body) {
        try {
            Long friendId = body.get("friendId");
            friendService.sendFriendRequest(userId, friendId);
            return ResponseEntity.ok(Map.of("code", 200, "message", "好友请求已发送"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getRequests(@RequestAttribute("userId") Long userId) {
        List<FriendRequestDTO> requests = friendService.getPendingRequests(userId);
        return ResponseEntity.ok(Map.of("code", 200, "data", requests));
    }

    @PutMapping("/requests/{id}/accept")
    public ResponseEntity<?> acceptRequest(@PathVariable Long id) {
        try {
            friendService.acceptRequest(id);
            return ResponseEntity.ok(Map.of("code", 200, "message", "已同意好友请求"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @PutMapping("/requests/{id}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id) {
        try {
            friendService.rejectRequest(id);
            return ResponseEntity.ok(Map.of("code", 200, "message", "已拒绝好友请求"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getFriendList(@RequestAttribute("userId") Long userId) {
        List<FriendDTO> friends = friendService.getFriendList(userId);
        return ResponseEntity.ok(Map.of("code", 200, "data", friends));
    }
}
