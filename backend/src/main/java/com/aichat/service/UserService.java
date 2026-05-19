package com.aichat.service;

import com.aichat.dto.LoginRequest;
import com.aichat.dto.LoginResponse;
import com.aichat.dto.RegisterRequest;
import com.aichat.config.JwtUtil;
import com.aichat.entity.FriendRelation;
import com.aichat.entity.User;
import com.aichat.repository.FriendRelationRepository;
import com.aichat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FriendRelationRepository friendRelationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname() != null ? request.getNickname() : request.getUsername())
                .build();
        user = userRepository.save(user);

        // 自动添加 AI 机器人为好友
        Optional<User> aiBot = userRepository.findByUsername("ai_bot");
        if (aiBot.isPresent()) {
            FriendRelation relation = FriendRelation.builder()
                    .user(user)
                    .friend(aiBot.get())
                    .status(1)
                    .build();
            friendRelationRepository.save(relation);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Transactional
    public void ensureAiBotExists() {
        if (!userRepository.existsByUsername("ai_bot")) {
            User aiBot = User.builder()
                    .username("ai_bot")
                    .password(passwordEncoder.encode("ai_bot_secret_123"))
                    .nickname("小A")
                    .avatar("/ai-bot-avatar.png")
                    .build();
            userRepository.save(aiBot);
        }
    }
}
