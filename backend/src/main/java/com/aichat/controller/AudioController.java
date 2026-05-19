package com.aichat.controller;

import com.aichat.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * 语音消息控制器：上传和播放音频
 */
@RestController
@RequestMapping("/api/audio")
@RequiredArgsConstructor
@Slf4j
public class AudioController {

    private final MessageService messageService;

    /** 音频文件存储目录 */
    private final Path audioDir = Paths.get(System.getProperty("user.dir"), "audio");

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(audioDir);
        log.info("音频存储目录: {}", audioDir.toAbsolutePath());
    }

    /**
     * 上传音频文件
     * @param file 音频文件 (webm/ogg格式)
     * @return 文件访问路径
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "文件为空"));
            }
            // 生成唯一文件名
            String ext = ".webm";
            String filename = UUID.randomUUID() + ext;
            Path targetPath = audioDir.resolve(filename);
            file.transferTo(targetPath.toFile());

            String url = "/api/audio/" + filename;
            return ResponseEntity.ok(Map.of("code", 200, "data", Map.of("url", url), "message", "上传成功"));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "上传失败: " + e.getMessage()));
        }
    }

    /**
     * 获取音频文件
     */
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getAudio(@PathVariable String filename) {
        try {
            Path filePath = audioDir.resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("audio/webm"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
