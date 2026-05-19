package com.aichat.controller;

import com.aichat.entity.User;
import com.aichat.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final FriendService friendService;

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String keyword,
                                          @RequestAttribute("userId") Long userId) {
        List<User> users = friendService.searchUsers(keyword, userId);
        List<Map<String, Object>> result = users.stream().map(u -> Map.<String, Object>of(
                "id", u.getId(),
                "username", u.getUsername(),
                "nickname", u.getNickname(),
                "avatar", u.getAvatar()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("code", 200, "data", result));
    }
}
